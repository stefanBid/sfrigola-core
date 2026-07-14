package com.sb.sfrigola_core.domains.categories.service.impl;

import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.common.models.contracts.SCPagedResult;
import com.sb.sfrigola_core.common.util.SCPaginationUtils;
import com.sb.sfrigola_core.domains.categories.dto.input.AddCategoryDto;
import com.sb.sfrigola_core.domains.categories.dto.input.ReorderedCategoriesTreeDto;
import com.sb.sfrigola_core.domains.categories.dto.input.UpdateCategoryDto;
import com.sb.sfrigola_core.domains.categories.dto.input.UpsetCategoryTranslationDto;
import com.sb.sfrigola_core.domains.categories.dto.view.CategoryDetailsAdminDto;
import com.sb.sfrigola_core.domains.categories.dto.view.CategoryPreviewAdminDto;
import com.sb.sfrigola_core.domains.categories.dto.view.CategoryPublicViewDto;
import com.sb.sfrigola_core.domains.categories.dto.view.CategoryTranslationAdminDto;
import com.sb.sfrigola_core.domains.categories.entity.Category;
import com.sb.sfrigola_core.domains.categories.entity.CategoryTranslation;
import com.sb.sfrigola_core.domains.categories.exception.*;
import com.sb.sfrigola_core.domains.categories.repository.ICategoryRepository;
import com.sb.sfrigola_core.domains.categories.service.ICategoryService;
import com.sb.sfrigola_core.domains.languages.entity.Language;
import com.sb.sfrigola_core.domains.languages.service.ILanguageDomainBridgeService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements ICategoryService {

    private final ICategoryRepository categoryRepository;
    private final ILanguageDomainBridgeService languageDomainBridgeService;

    @Override
    public SCPagedResult<CategoryPublicViewDto> getAll(SCFilterQuery<Void> filterQuery, @NonNull String locale) {
        languageDomainBridgeService.validateLocaleIsActiveOrThrow(locale);

        var pageable = SCPaginationUtils.toPageable(filterQuery, true);

        // Step 1: Fetch the IDs of active categories for the given locale
        // Remove Category that are inactive or translation that are not in the given locale
        var categoryIds = categoryRepository.findIdsByLocaleAndIsActiveAndSearchKeyAsc(locale,true,null, pageable);

        // Step 2: Fetch and restore the ordered ID sequence from step 1
        if(categoryIds.hasContent()) {
            var ids = categoryIds.getContent();
            Map<Long, Category> byId = categoryRepository.findByIdsWithSpecificTranslation(ids, locale)
                    .stream().collect(Collectors.toMap(Category::getId, c -> c));
            List<Category> ordered = ids.stream().map(byId::get).filter(Objects::nonNull).toList();
            return new SCPagedResult<>(
                    ordered.stream().map(this::toDto).toList(),
                    SCPaginationUtils.toPagedOption(categoryIds)
            );
        }

        return SCPagedResult.empty();

    }


    @Override
    public SCPagedResult<CategoryPreviewAdminDto> getAllAdmin(SCFilterQuery<Void> filterQuery, @NonNull String locale, Boolean isActive) {
        Map<String, String> activeLanguagesMap = languageDomainBridgeService.getAllActiveLanguagesSimpleMap();

        // LOCALE CHECK
        languageDomainBridgeService.validateLocaleIsActiveByActiveLanguagesMapKeysOrThrow(activeLanguagesMap.keySet(), locale);

        // SORT SWITCHER
        // CASE: Sort is ASC call query with GOUP-BY ASC
        // CASE: Sort is DESC call query with GOUP-BY DESC
        boolean descending = filterQuery.sort() != null && !filterQuery.sort().isAsc();


        // STEP 1: Obtain ids
        // Locale is always required: ALL categories are returned regardless of translation coverage,
        // the given locale only drives the preview translation (LEFT JOIN — null when missing).
        var pageable = SCPaginationUtils.toPageable(filterQuery, true);
        Page<Long> categoryIds = descending
                ? categoryRepository.findIdsByLocaleAndIsActiveAndSearchKeyDesc(locale, isActive, filterQuery.searchKey(), pageable)
                : categoryRepository.findIdsByLocaleAndIsActiveAndSearchKeyAsc(locale, isActive, filterQuery.searchKey(), pageable);

        if (categoryIds.hasContent()) {
            var ids = categoryIds.getContent();
            Map<Long, Category> byId = categoryRepository.findByIdsWithAllTranslations(ids)
                    .stream().collect(Collectors.toMap(Category::getId, c -> c));
            List<Category> ordered = ids.stream().map(byId::get).filter(Objects::nonNull).toList();

            return new SCPagedResult<>(
                    ordered.stream().map(category -> {
                        CategoryTranslation categoryTranslation = category.getTranslations().stream()
                                .filter(t -> t.getLanguage().getCode().equals(locale))
                                .findFirst().orElse(null);

                        return toAdminDto(category, categoryTranslation, activeLanguagesMap);
                    }).toList(),
                    SCPaginationUtils.toPagedOption(categoryIds)
            );
        }
        return SCPagedResult.empty();

    }

    @Override
    public CategoryDetailsAdminDto getByPublicIdAdmin(UUID publicId, @NonNull String locale) {
        languageDomainBridgeService.validateLocaleIsActiveOrThrow(locale);

        var category = categoryRepository.findByPublicIdWithAllTranslation(publicId)
                .orElseThrow(() -> new NoCategoryFoundException(publicId));

        // Filter down to the requested locale; null if no translation exists for it
        var translation = category.getTranslations().stream()
                .filter(t -> t.getLanguage().getCode().equals(locale))
                .findFirst().orElse(null);

        return toAdminDetailsDto(category, translation);
    }

    @Override
    @Transactional
    public CategoryPreviewAdminDto createNewCategory(AddCategoryDto addCategoryDto, @Nullable UUID parentPublicId, @NonNull String locale) {
        // SLUG CHECK: If slug already exists, throw an exception
        if (categoryRepository.existsBySlug(addCategoryDto.slug()))
            throw new CategorySlugAlreadyExistsException(addCategoryDto.slug());

        // TRANSLATION CHEKS:
        // 1) No duplicated translation
        // 2) A new category must have all active languages covered in translation, otherwise it is not valid
        var activeLanguagesMap = languageDomainBridgeService.getActiveLanguageEntitiesMap();

        var activeCodeSet = activeLanguagesMap.keySet().stream().map(String::toLowerCase).collect(Collectors.toSet());
        Set<String> seenLocales = new HashSet<>();
        addCategoryDto.translations().forEach(t -> {
            if (!seenLocales.add(t.langCode()))
                throw new DuplicateCategoryLocaleException(t.langCode());
        });

        if(!activeCodeSet.containsAll(seenLocales) || activeCodeSet.size() != seenLocales.size())
            throw new MissingCategoryLocalesException();

        // PARENT CHECK: If parentPublicId is present, check if it exists
        Category parentCategory = null;
        if (parentPublicId != null) {
            parentCategory = categoryRepository.findByPublicId(parentPublicId)
                    .orElseThrow(() -> new NoCategoryFoundException(parentPublicId));
        }

        Category category = new Category();

        // Set Translation
        ArrayList<CategoryTranslation> translations = addCategoryDto.translations().stream()
                .map(t -> {
                    Language lang = languageDomainBridgeService.getLangFromEntitiesMapFromKeyOrThrow(activeLanguagesMap, t.langCode());
                    return this.toCategoryTranslation(t, lang);
                })
                .collect(Collectors.toCollection(ArrayList::new));
        translations.forEach(t -> t.setCategory(category));

        if (parentCategory != null)
            category.setParent(parentCategory);

        Long parentId = parentCategory != null ? parentCategory.getId() : null;
        short sortOrder = (short) (categoryRepository.findMaxSortOrderInGroup(parentId) + 1);
        category.setSortOrder(sortOrder);

        // Set Other parameters
        category.setSlug(addCategoryDto.slug());
        category.setActive(addCategoryDto.isActive());
        category.setTranslations(translations);

        categoryRepository.save(category);

        return toAdminDto(
                category,
                translations.stream().filter(t -> t.getLanguage().getCode().equals(locale)).findFirst().orElse(null),
                languageDomainBridgeService.toSimpleLanguagesMap(activeLanguagesMap)
        );
    }

    @Override
    @Transactional
    public CategoryPreviewAdminDto updateCategory(UpdateCategoryDto updateCategoryDto, UUID publicId) {

        // ID CHECK: If the public id passed match an existing Category
        var categoryToUpdate = categoryRepository.findByPublicIdWithAllTranslation(publicId)
                .orElseThrow(() -> new NoCategoryFoundException(publicId));

        // SLUG CHECK: If slug changed and the new one already exists, throw an exception
        if (!categoryToUpdate.getSlug().equals(updateCategoryDto.slug()) && categoryRepository.existsBySlug(updateCategoryDto.slug()))
            throw new CategorySlugAlreadyExistsException(updateCategoryDto.slug());

        // TRANSLATION CHECK: Update translation is of an active locale
        var activeLangMap = languageDomainBridgeService.getActiveLanguageEntitiesMap();
        Language lang = languageDomainBridgeService.getLangFromEntitiesMapFromKeyOrThrow(activeLangMap, updateCategoryDto.specificTranslation().langCode());

        var categoryTranslationToUpdate = categoryToUpdate.getTranslations().stream()
                .filter(t -> t.getLanguage().getCode().equals(updateCategoryDto.specificTranslation().langCode()))
                .findFirst()
                .orElse(null);

        if(categoryTranslationToUpdate == null) {
            // New Translation
            categoryTranslationToUpdate = toCategoryTranslation(updateCategoryDto.specificTranslation(), lang);
            categoryTranslationToUpdate.setCategory(categoryToUpdate);
            categoryToUpdate.getTranslations().add(categoryTranslationToUpdate);
        }else {
            // Update existing translation — only touch fields that actually changed
            if (!categoryTranslationToUpdate.getName().equals(updateCategoryDto.specificTranslation().name()))
                categoryTranslationToUpdate.setName(updateCategoryDto.specificTranslation().name());
            if (!Objects.equals(categoryTranslationToUpdate.getDescription(), updateCategoryDto.specificTranslation().description()))
                categoryTranslationToUpdate.setDescription(updateCategoryDto.specificTranslation().description());
        }

        if(!categoryToUpdate.getSlug().equals(updateCategoryDto.slug()))
            categoryToUpdate.setSlug(updateCategoryDto.slug());

        if(categoryToUpdate.isActive() != updateCategoryDto.isActive())
            categoryToUpdate.setActive(updateCategoryDto.isActive());

        return toAdminDto(categoryToUpdate, categoryTranslationToUpdate, languageDomainBridgeService.toSimpleLanguagesMap(activeLangMap));
    }

    @Override
    @Transactional
    public UUID deleteCategory(UUID publicId) {
        // ID CHECK: If the public id passed match an existing Category
        var categoryToDelete = categoryRepository.findByPublicId(publicId)
                .orElseThrow(() -> new NoCategoryFoundException(publicId));

        // CHILDREN CHECK: Block delete if category still has children — caller must reassign/delete them first
        if (categoryRepository.existsByParentId(categoryToDelete.getId()))
            throw new CategoryHasChildrenException(publicId);

        categoryRepository.delete(categoryToDelete);
        return categoryToDelete.getPublicId();
    }

    @Override
    @Transactional
    public List<CategoryPreviewAdminDto> reorderCategories(ReorderedCategoriesTreeDto reorderDto) {
        // PARENT CHECK: If parentPublicId is present, check if it exists
        Long parentId = null;
        if(reorderDto.parentPublicId() != null) {
            Category parent = categoryRepository.findByPublicId(reorderDto.parentPublicId())
                    .orElseThrow(() -> new NoCategoryFoundException(reorderDto.parentPublicId()));
            parentId = parent.getId();
        }

        // GROUP SWITCHER
        // CASE 1: parentID == null
        // RESULT: obtain categories of main root
        // CASE2 : parentID != null
        // RESULT: obtain children categories of a root categories

        List<Category> groupOfCategoryToSort = parentId == null ? categoryRepository.findByParentIsNull() : categoryRepository.findByParentId(parentId);

        // GROUP CHECK: reorder input must match exactly the categories in the target group
        Set<UUID> dbIds = groupOfCategoryToSort.stream().map(Category::getPublicId).collect(Collectors.toSet());
        Set<UUID> inputIds = new HashSet<>(reorderDto.orderedPublicIds());

        if(!dbIds.equals(inputIds))
            throw new CategoryReorderMismatchException();

        // Map publicId -> entity for lookup during reordering
        Map<UUID, Category> byPublicId = groupOfCategoryToSort.stream().collect(Collectors.toMap(Category::getPublicId, c -> c));
        List<Category> ordered = new ArrayList<>();

        // Reorder categories
        for(int i =0; i< reorderDto.orderedPublicIds().size(); i++) {
            UUID publicId = reorderDto.orderedPublicIds().get(i);
            Category category = byPublicId.get(publicId);
            category.setSortOrder((short) i);
            ordered.add(category);
        }

        var activeLanguagesMap = languageDomainBridgeService.getAllActiveLanguagesSimpleMap();
        return ordered.stream()
                .map(c -> toAdminDto(c, null, activeLanguagesMap))
                .toList();
    }

    // =========================================================
    // PRIVATE
    // =========================================================

    private CategoryPublicViewDto toDto(Category category) {
        // We have only one item in the list of transaction
        // The first query filter it
        var translation = category.getTranslations().getFirst();
        return new CategoryPublicViewDto(
                category.getPublicId(),
                category.getSlug(),
                category.getParent() != null ? category.getParent().getPublicId() : null,
                category.getSortOrder(),
                category.isActive(),
                translation.getName(),
                translation.getDescription()
        );
    }

    private CategoryPreviewAdminDto toAdminDto(Category category, @Nullable CategoryTranslation translationPreview, Map<String, String> activeLanguagesMap) {
        // Obtain a Map of Translation for a specific category, but only for active languages
        Map<String, String> translatedLanguages = languageDomainBridgeService.buildTranslatedLanguagesMap(category.getTranslations(), activeLanguagesMap);

        // Obtain Translation Preview for a specific Lang
        CategoryTranslationAdminDto categoryPreviewTranslationAdminDto = new CategoryTranslationAdminDto(
                translationPreview != null ? translationPreview.getName() : null,
                translationPreview != null ? translationPreview.getDescription() : null
        );

        return new CategoryPreviewAdminDto(
                category.getPublicId(),
                category.getSlug(),
                category.getParent() != null ? category.getParent().getPublicId() : null,
                category.getSortOrder(),
                category.isActive(),
                categoryPreviewTranslationAdminDto,
                translatedLanguages
        );
    }

    private CategoryDetailsAdminDto toAdminDetailsDto(Category category, @Nullable CategoryTranslation translation) {
        var specificTranslation = new CategoryTranslationAdminDto(
                translation != null ? translation.getName() : null,
                translation != null ? translation.getDescription() : null
        );
        return new CategoryDetailsAdminDto(
                category.getPublicId(),
                category.getSlug(),
                category.getParent() != null ? category.getParent().getPublicId() : null,
                category.getSortOrder(),
                category.isActive(),
                specificTranslation
        );
    }

    private CategoryTranslation toCategoryTranslation(UpsetCategoryTranslationDto upsetCategoryTranslationDto, Language lang) {
        CategoryTranslation translation = new CategoryTranslation();
        translation.setLanguage(lang);
        translation.setName(upsetCategoryTranslationDto.name());
        translation.setDescription(upsetCategoryTranslationDto.description());
        return translation;
    }

}

