package com.sb.sfrigola_core.domains.ratings.entity;

import com.sb.sfrigola_core.common.entity.BaseEntity;
import com.sb.sfrigola_core.domains.recipes.entity.Recipe;
import com.sb.sfrigola_core.domains.users.entity.SCUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
        name = "ratings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "recipe_id"})
)
@Getter
@Setter
public class Rating extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private SCUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Column(name = "score", nullable = false)
    private Short score;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

}
