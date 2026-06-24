package com.sb.sfrigola_core.domains.categories.repository;

import com.sb.sfrigola_core.domains.categories.entity.Category;
import com.sb.sfrigola_core.domains.categories.entity.CategoryTranslation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryTranslationRepository extends JpaRepository<CategoryTranslation, Long> {

    Optional<CategoryTranslation> findByCategoryAndLanguage_Code(Category category, String locale);

    void deleteByCategoryAndLanguage_Code(Category category, String locale);
}