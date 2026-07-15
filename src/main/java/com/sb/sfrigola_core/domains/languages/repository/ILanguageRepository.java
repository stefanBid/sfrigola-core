package com.sb.sfrigola_core.domains.languages.repository;

import com.sb.sfrigola_core.domains.languages.entity.Language;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ILanguageRepository extends JpaRepository<Language, Long> {

    List<Language> findAllByIsActiveTrue();
    boolean existsByCode(String code);
    boolean existsByCodeAndIsActiveTrue(String code);
}