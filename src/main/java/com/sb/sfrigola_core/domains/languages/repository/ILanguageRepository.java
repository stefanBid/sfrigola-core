package com.sb.sfrigola_core.domains.languages.repository;

import com.sb.sfrigola_core.domains.languages.entity.Language;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ILanguageRepository extends JpaRepository<Language, Long> {

    Page<Language> findAllByIsActiveTrue(Pageable pageable);
    List<Language> findAllByIsActiveTrue();
    boolean existsByCode(String code);
    boolean existsByCodeAndIsActiveTrue(String code);
}