package com.sb.sfrigola_core.domains.recipes.service.impl;

import com.sb.sfrigola_core.domains.recipes.entity.Recipe;
import com.sb.sfrigola_core.domains.recipes.exception.NoRecipeFoundException;
import com.sb.sfrigola_core.domains.recipes.repository.IRecipeRepository;
import com.sb.sfrigola_core.domains.recipes.service.IRecipeDomainBridgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecipeDomainBridgeServiceImpl implements IRecipeDomainBridgeService {

    private final IRecipeRepository recipeRepository;

    @Override
    public Recipe getRecipeEntityByPublicIdOrThrow(UUID publicId) {
        return recipeRepository.findByPublicId(publicId).orElseThrow(
                () -> new NoRecipeFoundException(publicId)
        );
    }

    @Override
    public List<Recipe> getRecipesByIdsWithLocale(List<Long> ids, String locale) {
        return recipeRepository.findByIdsWithSpecificTranslation(ids, locale);
    }
}
