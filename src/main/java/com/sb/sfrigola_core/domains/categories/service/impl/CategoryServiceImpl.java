package com.sb.sfrigola_core.domains.categories.service.impl;

import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.common.models.contracts.SCPagedResult;
import com.sb.sfrigola_core.common.util.SCPaginationUtils;
import com.sb.sfrigola_core.domains.categories.dto.*;
import com.sb.sfrigola_core.domains.categories.dto.admin.*;
import com.sb.sfrigola_core.domains.categories.entity.Category;
import com.sb.sfrigola_core.domains.categories.entity.CategoryTranslation;
import com.sb.sfrigola_core.domains.categories.exception.*;
import com.sb.sfrigola_core.domains.categories.repository.ICategoryRepository;
import com.sb.sfrigola_core.domains.categories.service.ICategoryService;
import com.sb.sfrigola_core.domains.languages.entity.Language;
import com.sb.sfrigola_core.domains.languages.service.ILanguageDomainBridgeService;
import lombok.RequiredArgsConstructor;
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
    public SCPagedResult<CategoryDto> getAll(SCFilterQuery<Void> filterQuery, String locale) {
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
    public SCPagedResult<CategoryPreviewAdminDto> getAllAdmin(SCFilterQuery<Void> filterQuery, String locale, Boolean isActive) {
        var totalActiveLanguages = languageDomainBridgeService.getAllActiveLanguages().size();

        // SORT SWITCHER
        // CASE: Sort is ASC call query with GOUP-BY ASC
        // CASE: Sort is DESC call query with GOUP-BY DESC
        boolean descending = filterQuery.sort() != null && !filterQuery.sort().isAsc();

        // LOCALE SWITCHER
        // CASE: Locale is null
        // RESULT: All categories returned; preview translation = first element of collection
        // CASE: Locale has value
        // RESULT: Only categories that have a translation for the given locale; preview = that specific translation
        boolean hasLocale = locale != null && !locale.isBlank();

        // STEP 1: Obtain ids
        var pageable = SCPaginationUtils.toPageable(filterQuery, true);
        Page<Long> categoryIds;

        if(descending)
            categoryIds = hasLocale
                    ? categoryRepository.findIdsByLocaleAndIsActiveAndSearchKeyDesc(locale, isActive, filterQuery.searchKey(), pageable)
                    : categoryRepository.findIdsByIsActiveAndSearchKeyDesc(isActive, filterQuery.searchKey(), pageable);
        else
            categoryIds = hasLocale
                    ? categoryRepository.findIdsByLocaleAndIsActiveAndSearchKeyAsc(locale, isActive, filterQuery.searchKey(), pageable)
                    : categoryRepository.findIdsByIsActiveAndSearchKeyAsc(isActive, filterQuery.searchKey(), pageable);


        if (categoryIds.hasContent()) {
            var ids = categoryIds.getContent();
            Map<Long, Category> byId = categoryRepository.findByIdsWithAllTranslations(ids)
                    .stream().collect(Collectors.toMap(Category::getId, c -> c));
            List<Category> ordered = ids.stream().map(byId::get).filter(Objects::nonNull).toList();

            return new SCPagedResult<>(
                    ordered.stream().map(category -> {
                        CategoryTranslation categoryTranslation;
                        if(locale != null)
                            categoryTranslation = category.getTranslations().stream().filter(t -> t.getLanguage().getCode().equals(locale)).findFirst().orElse(null);
                        else
                            categoryTranslation = category.getTranslations().stream().findFirst().orElse(null);

                        var translationPreview = new CategoryPreviewTranslationAdminDto(
                                categoryTranslation != null ? categoryTranslation.getName() : null,
                                categoryTranslation != null ? categoryTranslation.getDescription() : null
                        );
                        return toAdminDto(category, translationPreview, totalActiveLanguages);
                    }).toList(),
                    SCPaginationUtils.toPagedOption(categoryIds)
            );
        }
        return SCPagedResult.empty();

    }

    @Override
    public CategoryDetailsAdminDto getByPublicIdAdmin(UUID publicId) {
        var activeLanguages = new ArrayList<>(languageDomainBridgeService.getAllActiveLanguages());
        var category = categoryRepository.findByPublicIdWithAllTranslation(publicId)
                .orElseThrow(() -> new NoCategoryFoundException(publicId));
        // Preparing details for this category
        int totalLocalization = category.getTranslations().size();
        int totalMissingLocalization = activeLanguages.size() - totalLocalization;
        ArrayList<CategoryDetailsTranslationAdminDto> missingTranslation = new ArrayList<>();

        // Populate missing translation only if there are missing languages
        if(totalMissingLocalization > 0) {
            category.getTranslations().forEach(t -> activeLanguages.removeIf(l -> l.code().equals(t.getLanguage().getCode())));
            activeLanguages.forEach(l -> missingTranslation.add(new CategoryDetailsTranslationAdminDto(l.code(), l.name(), null, null)));
        }

        return toAdminDetailsDto(category, missingTranslation);

    }

    @Override
    @Transactional
    public CategoryPreviewAdminDto createNewCategory(CategoryInputDto inputDto, @Nullable UUID parentPublicId) {
        // Guard for existing Slug
        if (categoryRepository.existsBySlug(inputDto.slug()))
            throw new CategorySlugAlreadyExistsException(inputDto.slug());

        // Guard for duplicate locale in input — fail fast before building entities
        Set<String> seenLocales = new HashSet<>();
        inputDto.translations().forEach(t -> {
            if (!seenLocales.add(t.langCode()))
                throw new DuplicateCategoryLocaleException(t.langCode());
        });

        // Resolve parent early — fail fast before any entity construction
        Category parentCategory = null;
        if (parentPublicId != null) {
            parentCategory = categoryRepository.findByPublicId(parentPublicId)
                    .orElseThrow(() -> new NoCategoryFoundException(parentPublicId));
        }

        Category category = new Category();

        // Set Translation
        var activeLanguageMap = languageDomainBridgeService.getActiveLanguageEntitiesMap();
        ArrayList<CategoryTranslation> translations = inputDto.translations().stream()
                .map(t -> {
                    Language lang = activeLanguageMap.get(t.langCode());
                    if (lang == null) throw new InvalidCategoryLocaleException(t.langCode());
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
        category.setSlug(inputDto.slug());
        category.setActive(inputDto.isActive());
        category.setTranslations(translations);

        categoryRepository.save(category);

        var dataForTranslationPreview = inputDto.translations().stream().findFirst().orElse(null);
        var translationPreview = new CategoryPreviewTranslationAdminDto(
                dataForTranslationPreview != null ? dataForTranslationPreview.name() : null,
                dataForTranslationPreview != null ? dataForTranslationPreview.description() : null
        );

        return toAdminDto(category, translationPreview,activeLanguageMap.size());
    }

    @Override
    @Transactional
    public CategoryPreviewAdminDto updateCategory(CategoryInputDto inputDto, UUID publicId) {
        // Guard for check if category with passed public ID does exist
        var categoryToUpdate = categoryRepository.findByPublicIdWithAllTranslation(publicId)
                .orElseThrow(() -> new NoCategoryFoundException(publicId));

        // Guard for existing Slug

        if (!categoryToUpdate.getSlug().equals(inputDto.slug()) && categoryRepository.existsBySlug(inputDto.slug()))
            throw new CategorySlugAlreadyExistsException(inputDto.slug());

        // Guard for duplicate locale in input — fail fast before building entities
        Set<String> seenLocales = new HashSet<>();
        inputDto.translations().forEach(t -> {
            if (!seenLocales.add(t.langCode()))
                throw new DuplicateCategoryLocaleException(t.langCode());
        });

        var activeLangMap = languageDomainBridgeService.getActiveLanguageEntitiesMap();

        Map<String, CategoryTranslation> categoryToUpdateTranslationMapped = categoryToUpdate.getTranslations()
                .stream()
                .collect(Collectors.toMap(t -> t.getLanguage().getCode(), t -> t));

        List<CategoryTranslation> toRemove = new ArrayList<>();

        for (CategoryTranslationInputDto input : inputDto.translations()) {
            Language lang = activeLangMap.get(input.langCode());
            if (lang == null) throw new InvalidCategoryLocaleException(input.langCode());
            // If in an update you want to remove a translation for category you can send it into translation array input with name null
            boolean deleteSignal = input.name() == null || input.name().isBlank();
            CategoryTranslation translationExtracted = categoryToUpdateTranslationMapped.get(input.langCode());

            if (deleteSignal) {
                if (translationExtracted != null) toRemove.add(translationExtracted);
            } else if (translationExtracted != null) {
                if(translationExtracted.getName().equals(input.name()) && Objects.equals(translationExtracted.getDescription(), input.description())) continue;

                if(!translationExtracted.getName().equals(input.name()))
                    translationExtracted.setName(input.name());
                if(!Objects.equals(translationExtracted.getDescription(), input.description()))
                    translationExtracted.setDescription(input.description());
            } else {
                CategoryTranslation newT = toCategoryTranslation(input, lang);
                newT.setCategory(categoryToUpdate);
                categoryToUpdate.getTranslations().add(newT);
            }
        }

        categoryToUpdate.getTranslations().removeAll(toRemove);
        categoryToUpdate.setSlug(inputDto.slug());
        categoryToUpdate.setActive(inputDto.isActive());

        var dataForTranslationPreview = inputDto.translations().stream().findFirst().orElse(null);
        var translationPreview = new CategoryPreviewTranslationAdminDto(
                dataForTranslationPreview != null ? dataForTranslationPreview.name() : null,
                dataForTranslationPreview != null ? dataForTranslationPreview.description() : null
        );
        return toAdminDto(categoryToUpdate, translationPreview, activeLangMap.size());
    }

    @Override
    @Transactional
    public CategoryPreviewAdminDto deleteCategory(UUID publicId) {
        var categoryToDelete = categoryRepository.findByPublicIdWithAllTranslation(publicId)
                .orElseThrow(() -> new NoCategoryFoundException(publicId));

        if (categoryRepository.existsByParentId(categoryToDelete.getId()))
            throw new CategoryHasChildrenException(publicId);

        var totalActiveLanguages = languageDomainBridgeService.getAllActiveLanguages().size();
        var dataForTranslationPreview = categoryToDelete.getTranslations().stream().findFirst().orElse(null);
        var translationPreview = new CategoryPreviewTranslationAdminDto(
                dataForTranslationPreview != null ? dataForTranslationPreview.getName() : null,
                dataForTranslationPreview != null ? dataForTranslationPreview.getDescription() : null
        );

        categoryRepository.delete(categoryToDelete);
        return toAdminDto(categoryToDelete, translationPreview, totalActiveLanguages);
    }

    @Override
    @Transactional
    public List<CategoryPreviewAdminDto> reorderCategories(CategoryReorderInputDto reorderDto) {
        // Resolve Parent ID if is present
        Long parentId = null;
        if(reorderDto.parentPublicId() != null) {
            Category parent = categoryRepository.findByPublicId(reorderDto.parentPublicId())
                    .orElseThrow(() -> new NoCategoryFoundException(reorderDto.parentPublicId()));
            parentId = parent.getId();
        }

        // Parent ID Switcher
        // CASE 1: parentID == null
        // RESULT: obtain categories of main root
        // CASE2 : parentID != null
        // RESULT: obtain children categories of a root categories

        List<Category> groupOfCategoryToSort = parentId == null ? categoryRepository.findByParentIsNull() : categoryRepository.findByParentId(parentId);

        // Check if reorder input has exactly content of DB
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

        var totalActiveLanguages = languageDomainBridgeService.getAllActiveLanguages().size();
        return ordered.stream()
                .map(c -> toAdminDto(c, new
                        CategoryPreviewTranslationAdminDto(null,
                        null), totalActiveLanguages))
                .toList();
    }

    // =========================================================
    // PRIVATE
    // =========================================================

    private CategoryDto toDto(Category category) {
        // We have only one item in the list of transaction
        // The first query filter it
        var translation = category.getTranslations().getFirst();
        return new CategoryDto(
                category.getPublicId(),
                category.getSlug(),
                category.getParent() != null ? category.getParent().getPublicId() : null,
                category.getSortOrder(),
                category.isActive(),
                translation.getName(),
                translation.getDescription()
        );
    }

    private CategoryPreviewAdminDto toAdminDto(Category category, CategoryPreviewTranslationAdminDto translationPreview, int totalActiveLanguages) {
        var translationCount = category.getTranslations().size();
        return new CategoryPreviewAdminDto(
                category.getPublicId(),
                category.getSlug(),
                category.getParent() != null ? category.getParent().getPublicId() : null,
                category.getSortOrder(),
                category.isActive(),
                translationPreview,
                translationCount,
                totalActiveLanguages - translationCount
        );
    }

    private CategoryDetailsAdminDto toAdminDetailsDto(Category category, List<CategoryDetailsTranslationAdminDto> missingTranslation) {
        var extractedPreview = category.getTranslations().stream().findFirst().orElse(null);
        return new CategoryDetailsAdminDto(
                category.getPublicId(),
                category.getSlug(),
                category.getParent() != null ? category.getParent().getPublicId() : null,
                category.getSortOrder(),
                category.isActive(),
                extractedPreview != null ? extractedPreview.getName() : null,
                extractedPreview != null ? extractedPreview.getDescription() : null,
                category.getTranslations().stream().map(this::toCategoryTranslationDto).toList(),
                missingTranslation
        );

    }


    private CategoryDetailsTranslationAdminDto toCategoryTranslationDto(CategoryTranslation translation) {
        return new CategoryDetailsTranslationAdminDto(translation.getLanguage().getCode(), translation.getLanguage().getName(), translation.getName(), translation.getDescription());
    }

    private CategoryTranslation toCategoryTranslation(CategoryTranslationInputDto translationInputDto, Language lang) {
        CategoryTranslation translation = new CategoryTranslation();
        translation.setLanguage(lang);
        translation.setName(translationInputDto.name());
        translation.setDescription(translationInputDto.description());
        return translation;
    }

}

