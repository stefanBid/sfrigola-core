package com.sb.sfrigola_core.domains.ingredients.controller;

import com.sb.sfrigola_core.domains.ingredients.service.IIngredientService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ingredients")
@RequiredArgsConstructor
@Validated
public class IngredientController {

    private final IIngredientService ingredientService;

}
