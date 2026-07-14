package com.sb.sfrigola_core.domains.recipes.entity;

import com.sb.sfrigola_core.common.entity.BaseEntity;
import com.sb.sfrigola_core.common.interfaces.ISCTranslationEntity;
import com.sb.sfrigola_core.domains.languages.entity.Language;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
        name = "recipe_translations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"recipe_id", "locale"})
)
@Getter
@Setter
public class RecipeTranslation extends BaseEntity implements ISCTranslationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locale", referencedColumnName = "code", nullable = false)
    private Language language;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "instructions", nullable = false, columnDefinition = "TEXT")
    private String instructions;

}
