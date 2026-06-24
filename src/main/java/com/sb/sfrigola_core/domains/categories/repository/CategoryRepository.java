package com.sb.sfrigola_core.domains.categories.repository;

import com.sb.sfrigola_core.domains.categories.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("""
        SELECT c.id FROM Category c
        JOIN c.translations t
        JOIN t.language l
        WHERE c.isActive = true AND l.code = :locale
    """)
    Page<Long> findActiveIdsByLocale(@Param("locale") String locale, Pageable pageable);

    @Query("""
        SELECT DISTINCT c FROM Category c
        LEFT JOIN FETCH c.parent
        JOIN FETCH c.translation t
        JOIN FETCH t.language l
        WHERE c.id IN :ids AND t.code = :locale
    """)
    List<Category> findByIdsWithTranslation(@Param("ids")List<Long> ids, @Param("locale") String locale);
}