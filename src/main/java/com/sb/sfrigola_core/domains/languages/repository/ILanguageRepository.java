package com.sb.sfrigola_core.domains.languages.repository;

import com.sb.sfrigola_core.domains.languages.entity.Language;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ILanguageRepository extends JpaRepository<Language, Long> {

    @Cacheable(value = "languages", key = "'allLanguages'")
    @Override
    List<Language> findAll();


    @Cacheable(value = "languages", key = "'activeLanguages'")
    List<Language> findAllByIsActiveTrue();
    boolean existsByCode(String code);
    boolean existsByCodeAndIsActiveTrue(String code);
}