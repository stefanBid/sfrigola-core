package com.sb.sfrigola_core.domains.ingredients.service.impl;

import com.sb.sfrigola_core.domains.ingredients.repository.IIngredientRepository;
import com.sb.sfrigola_core.domains.ingredients.service.IIngredientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IngredientServiceImpl implements IIngredientService {

    private final IIngredientRepository ingredientRepository;

}
