package com.sb.sfrigola_core.domains.categories.service.impl;

import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.common.models.contracts.SCPagedResult;
import com.sb.sfrigola_core.common.util.SCPaginationUtils;
import com.sb.sfrigola_core.domains.categories.dto.*;
import com.sb.sfrigola_core.domains.categories.entity.Category;
import com.sb.sfrigola_core.domains.categories.entity.CategoryTranslation;
import com.sb.sfrigola_core.domains.categories.exception.CategorySlugAlreadyExistsException;
import com.sb.sfrigola_core.domains.categories.exception.DuplicateCategoryLocaleException;
import com.sb.sfrigola_core.domains.categories.exception.InvalidCategoryLocaleException;
import com.sb.sfrigola_core.domains.categories.exception.NoCategoryFoundException;
import com.sb.sfrigola_core.domains.categories.repository.ICategoryRepository;
import com.sb.sfrigola_core.domains.categories.service.ICategoryService;
import com.sb.sfrigola_core.domains.languages.entity.Language;
import com.sb.sfrigola_core.domains.languages.service.ILanguageDomainBridgeService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
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
        var pageable = SCPaginationUtils.toPageable(filterQuery);

        // Step 1: Fetch the IDs of active categories for the given locale
        // Remove Category that are inactive or translation that are not in the given locale
        var categoryIds = categoryRepository.findIdsByLocaleAndIsActiveAndSearchKey(locale,true,null, pageable);

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
        var pageable = SCPaginationUtils.toPageable(filterQuery);
        var totalActiveLanguages = languageDomainBridgeService.getAllActiveLanguages().size();

        var categoryIds = categoryRepository.findIdsByLocaleAndIsActiveAndSearchKey(locale, isActive, filterQuery.searchKey(), pageable);

        if(categoryIds.hasContent()) {
            var ids = categoryIds.getContent();
            Map<Long, Category> byId = categoryRepository.findByIdsWithAllTranslations(ids)
                    .stream().collect(Collectors.toMap(Category::getId, c -> c));
            List<Category> ordered = ids.stream().map(byId::get).filter(Objects::nonNull).toList();
            return new SCPagedResult<>(
                    ordered.stream().map(category -> toAdminDto(category, locale, totalActiveLanguages)).toList(),
                    SCPaginationUtils.toPagedOption(categoryIds)
            );
        }

        return SCPagedResult.empty();
    }

    @Override
    public CategoryDetailsAdminDto getByPublicIdAdmin(UUID publicId, String locale) {
        var activeLanguages = languageDomainBridgeService.getAllActiveLanguages();
        var category = categoryRepository.findByPublicIdWithAllTranslation(publicId)
                .orElseThrow(() -> new NoCategoryFoundException(publicId));
        // Preparing details for this category
        int totalLocalization = category.getTranslations().size();
        int totalMissingLocalization = activeLanguages.size() - totalLocalization;
        ArrayList<CategoryTranslationDto> missingTranslation = new ArrayList<>();

        // Populate missing translation only if there are missing languages
        if(totalMissingLocalization > 0) {
            category.getTranslations().forEach(t -> activeLanguages.removeIf(l -> l.code().equals(t.getLanguage().getCode())));
            activeLanguages.forEach(l -> missingTranslation.add(new CategoryTranslationDto(l.code(), l.name(), null, null)));
        }

        return toAdminDetailsDto(category,totalLocalization,totalMissingLocalization, missingTranslation, locale);

    }

    @Override
    @Transactional
    public CategoryPreviewAdminDto createNewCategory(CategoryUpsertDto categoryUpsertDto, @Nullable UUID parentPublicId) {
        // Guard for existing Slug
        if (categoryRepository.existsBySlug(categoryUpsertDto.slug()))
            throw new CategorySlugAlreadyExistsException(categoryUpsertDto.slug());

        // Guard for duplicate locale in input — fail fast before building entities
        Set<String> seenLocales = new HashSet<>();
        categoryUpsertDto.translations().forEach(t -> {
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
        ArrayList<CategoryTranslation> translations = categoryUpsertDto.translations().stream()
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
        category.setSlug(categoryUpsertDto.slug());
        category.setActive(categoryUpsertDto.isActive());
        category.setTranslations(translations);

        categoryRepository.save(category);

        String previewLocale = categoryUpsertDto.translations().getFirst().langCode();
        return toAdminDto(category, previewLocale, activeLanguageMap.size());
    }

    @Override
    @Transactional
    public CategoryPreviewAdminDto updateCategory(CategoryUpsertDto categoryUpsertDto, UUID publicId) {
        // Guard for check if category with passed public ID does exist
        var categoryToUpdate = categoryRepository.findByPublicIdWithAllTranslation(publicId)
                .orElseThrow(() -> new NoCategoryFoundException(publicId));

        // Guard for existing Slug

        if (!categoryToUpdate.getSlug().equals(categoryUpsertDto.slug()) && categoryRepository.existsBySlug(categoryUpsertDto.slug()))
            throw new CategorySlugAlreadyExistsException(categoryUpsertDto.slug());

        // Guard for duplicate locale in input — fail fast before building entities
        Set<String> seenLocales = new HashSet<>();
        categoryUpsertDto.translations().forEach(t -> {
            if (!seenLocales.add(t.langCode()))
                throw new DuplicateCategoryLocaleException(t.langCode());
        });

        var activeLangMap = languageDomainBridgeService.getActiveLanguageEntitiesMap();

        ArrayList<CategoryTranslation> translations = categoryUpsertDto.translations().stream()
                .map(t -> {
                    Language lang = activeLangMap.get(t.langCode());
                    if (lang == null) throw new InvalidCategoryLocaleException(t.langCode());
                    return this.toCategoryTranslation(t, lang);
                })
                .collect(Collectors.toCollection(ArrayList::new));

        categoryToUpdate.setTranslations(translations);

        if(!categoryUpsertDto.slug().equals(categoryToUpdate.getSlug()))
            categoryToUpdate.setSlug(categoryUpsertDto.slug());

        if(categoryUpsertDto.isActive() != categoryToUpdate.isActive())
            categoryToUpdate.setActive(categoryUpsertDto.isActive());

        String previewLocale = categoryUpsertDto.translations().getFirst().langCode();
        return toAdminDto(categoryToUpdate, previewLocale, activeLangMap.size());
    }

    @Override
    public CategoryPreviewAdminDto deleteCategory(UUID publicId) {
        throw new UnsupportedOperationException("Not yet implemented");
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

    private CategoryPreviewAdminDto toAdminDto(Category category, String locale, int totalActiveLanguages) {
        var translationCount = category.getTranslations().size();
        var extractedPreviewForSpecificLang = category.getTranslations().stream().filter(t -> t.getLanguage().getCode().equals(locale)).findFirst().orElse(null);
        return new CategoryPreviewAdminDto(
                category.getPublicId(),
                category.getSlug(),
                category.getParent() != null ? category.getParent().getPublicId() : null,
                category.getSortOrder(),
                category.isActive(),
                extractedPreviewForSpecificLang != null ? extractedPreviewForSpecificLang.getName() : null,
                extractedPreviewForSpecificLang != null ? extractedPreviewForSpecificLang.getDescription() : null,
                translationCount,
                totalActiveLanguages - translationCount
        );
    }

    private CategoryDetailsAdminDto toAdminDetailsDto(Category category, int totalLocalization, int totalMissingLocalization, List<CategoryTranslationDto> missingTranslation, String locale) {
        var extractedPreviewForSpecificLang = category.getTranslations().stream().filter(t -> t.getLanguage().getCode().equals(locale)).findFirst().orElse(null);
        return new CategoryDetailsAdminDto(
                category.getPublicId(),
                category.getSlug(),
                category.getParent() != null ? category.getParent().getPublicId() : null,
                category.getSortOrder(),
                category.isActive(),
                extractedPreviewForSpecificLang != null ? extractedPreviewForSpecificLang.getName() : null,
                extractedPreviewForSpecificLang != null ? extractedPreviewForSpecificLang.getDescription() : null,
                totalLocalization,
                totalMissingLocalization,
                category.getTranslations().stream().map(this::toCategoryTranslationDto).toList(),
                missingTranslation
        );

    }


    private CategoryTranslationDto toCategoryTranslationDto(CategoryTranslation translation) {
        return new CategoryTranslationDto(translation.getLanguage().getCode(), translation.getLanguage().getName(), translation.getName(), translation.getDescription());
    }

    private CategoryTranslation toCategoryTranslation(CategoryTranslationInputDto translationInputDto, Language lang) {
        CategoryTranslation translation = new CategoryTranslation();
        translation.setLanguage(lang);
        translation.setName(translationInputDto.name());
        translation.setDescription(translationInputDto.description());
        return translation;
    }

}

