package com.sb.sfrigola_core.domains.ingredients.entity;

import com.sb.sfrigola_core.common.entity.BaseEntity;
import com.sb.sfrigola_core.domains.languages.entity.Language;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
        name = "ingredient_translations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"ingredient_id", "locale"})
)
@Getter
@Setter
public class IngredientTranslation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locale", referencedColumnName = "code", nullable = false)
    private Language language;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

}
