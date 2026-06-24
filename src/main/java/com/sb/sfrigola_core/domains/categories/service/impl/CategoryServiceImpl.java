package com.sb.sfrigola_core.domains.categories.service.impl;

import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.common.models.contracts.SCPagedResult;
import com.sb.sfrigola_core.common.util.SCPaginationUtils;
import com.sb.sfrigola_core.domains.categories.dto.CategoryDto;
import com.sb.sfrigola_core.domains.categories.entity.Category;
import com.sb.sfrigola_core.domains.categories.repository.CategoryRepository;
import com.sb.sfrigola_core.domains.categories.service.ICategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements ICategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public SCPagedResult<CategoryDto> getAll(SCFilterQuery<Void> filterQuery, String locale) {
        var pageable = SCPaginationUtils.toPageable(filterQuery);

        // Step 1: Fetch the IDs of active categories for the given locale
        // Remove Category that are inactive or translation that are not in the given locale
        var categoryIds = categoryRepository.findActiveIdsByLocale(locale, pageable);

        // Step 2: Fetch correct localized category only there are ids
        if(categoryIds.hasContent()) {
            List<Category> localizedCategories = categoryRepository.findByIdsWithTranslation(categoryIds.getContent(), locale);
            return new SCPagedResult<>(
                    localizedCategories.stream().map(this::toDto).toList(),
                    SCPaginationUtils.toPagedOption(categoryIds)
            );
        }

        return SCPagedResult.empty();

    }

    private CategoryDto toDto(Category category) {
        // We have only one item in the list of transaction
        // The first query filter it
        var transaction = category.getTranslations().getFirst();
        return new CategoryDto(
                category.getPublicId(),
                category.getSlug(),
                category.getParent() != null ? category.getParent().getPublicId() : null,
                category.getSortOrder(),
                category.isActive(),
                transaction.getName(),
                transaction.getDescription()
        );
    }
}
