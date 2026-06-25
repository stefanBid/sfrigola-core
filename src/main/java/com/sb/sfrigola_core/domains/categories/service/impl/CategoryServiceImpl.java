package com.sb.sfrigola_core.domains.categories.service.impl;

import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.common.models.contracts.SCPagedResult;
import com.sb.sfrigola_core.common.util.SCPaginationUtils;
import com.sb.sfrigola_core.domains.categories.dto.CategoryDetailsAdminDto;
import com.sb.sfrigola_core.domains.categories.dto.CategoryDto;
import com.sb.sfrigola_core.domains.categories.dto.CategoryPreviewAdminDto;
import com.sb.sfrigola_core.domains.categories.dto.CategoryTranslationDto;
import com.sb.sfrigola_core.domains.categories.entity.Category;
import com.sb.sfrigola_core.domains.categories.entity.CategoryTranslation;
import com.sb.sfrigola_core.domains.categories.exception.NoCategoryFoundException;
import com.sb.sfrigola_core.domains.categories.repository.CategoryRepository;
import com.sb.sfrigola_core.domains.categories.service.ICategoryService;
import com.sb.sfrigola_core.domains.languages.service.ILanguageDomainBridgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements ICategoryService {

    private final CategoryRepository categoryRepository;
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
    public CategoryDetailsAdminDto getByPublicIdAdmin(String publicId, String locale) {
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

}

