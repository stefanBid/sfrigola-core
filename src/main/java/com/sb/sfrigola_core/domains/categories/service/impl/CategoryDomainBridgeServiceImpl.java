package com.sb.sfrigola_core.domains.categories.service.impl;

import com.sb.sfrigola_core.domains.categories.entity.Category;
import com.sb.sfrigola_core.domains.categories.exception.NoCategoryFoundException;
import com.sb.sfrigola_core.domains.categories.repository.ICategoryRepository;
import com.sb.sfrigola_core.domains.categories.service.ICategoryDomainBridgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CategoryDomainBridgeServiceImpl implements ICategoryDomainBridgeService {

    private final ICategoryRepository categoryRepository;

    @Override
    public Category getCategoryEntityByPublicIdOrThrow(UUID publicId) {
        return categoryRepository.findByPublicId(publicId).orElseThrow(
                () -> new NoCategoryFoundException(publicId)
        );
    }

    @Override
    public List<Category> getChildrenCategories(Category category) {
        return categoryRepository.findByParentId(category.getId());
    }
}
