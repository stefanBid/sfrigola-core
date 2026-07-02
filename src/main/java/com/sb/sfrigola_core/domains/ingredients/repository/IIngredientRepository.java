package com.sb.sfrigola_core.domains.ingredients.repository;

import com.sb.sfrigola_core.domains.ingredients.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IIngredientRepository extends JpaRepository<Ingredient, Long> {


}
