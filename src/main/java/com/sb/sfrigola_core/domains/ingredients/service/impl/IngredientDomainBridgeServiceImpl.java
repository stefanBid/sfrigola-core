package com.sb.sfrigola_core.domains.ingredients.service.impl;

import com.sb.sfrigola_core.domains.ingredients.entity.Ingredient;
import com.sb.sfrigola_core.domains.ingredients.exception.NoIngredientFoundException;
import com.sb.sfrigola_core.domains.ingredients.repository.IIngredientRepository;
import com.sb.sfrigola_core.domains.ingredients.service.IIngredientDomainBridgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IngredientDomainBridgeServiceImpl implements IIngredientDomainBridgeService {

    private final IIngredientRepository ingredientRepository;

    @Override
    public List<Ingredient> getIngredientsByPublicIds(List<UUID> publicIds) {
        Map<UUID, Ingredient> byPublicId = ingredientRepository.findByPublicIdIn(publicIds).stream()
                .collect(Collectors.toMap(Ingredient::getPublicId, i -> i));

        return publicIds.stream()
                .map(publicId -> {
                    Ingredient ingredient = byPublicId.get(publicId);
                    if (ingredient == null) throw new NoIngredientFoundException(publicId);
                    return ingredient;
                })
                .toList();
    }
}
