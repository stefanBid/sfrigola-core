package com.sb.sfrigola_core.domains.languages.repository;

import com.sb.sfrigola_core.domains.languages.entity.Language;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ILanguageRepository extends JpaRepository<Language, Long> {

    Page<Language> findAllByIsActiveTrue(Pageable pageable);
    boolean existsByCode(String code);
}