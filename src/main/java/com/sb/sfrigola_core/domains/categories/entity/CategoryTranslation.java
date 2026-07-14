package com.sb.sfrigola_core.domains.categories.entity;

import com.sb.sfrigola_core.common.entity.BaseEntity;
import com.sb.sfrigola_core.common.interfaces.ISCTranslationEntity;
import com.sb.sfrigola_core.domains.languages.entity.Language;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
        name = "category_translations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"category_id", "locale"})
)
@Getter
@Setter
public class CategoryTranslation extends BaseEntity implements ISCTranslationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locale", referencedColumnName = "code", nullable = false)
    private Language language;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

}