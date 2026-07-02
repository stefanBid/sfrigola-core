package com.sb.sfrigola_core.domains.tags.entity;

import com.sb.sfrigola_core.common.entity.BaseEntity;
import com.sb.sfrigola_core.domains.tags.enums.TagScope;
import com.sb.sfrigola_core.domains.tags.enums.TagStatus;
import com.sb.sfrigola_core.domains.tags.enums.TagType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tags")
@Getter
@Setter
public class Tag extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @Column(name = "slug", nullable = false, unique = true, length = 100)
    private String slug;

    @Column(name = "type", nullable = false, columnDefinition = "tag_type")
    @ColumnTransformer(write = "?::tag_type")
    private TagType type;

    @Column(name = "scope", nullable = false, columnDefinition = "tag_scope")
    @ColumnTransformer(write = "?::tag_scope")
    private TagScope scope = TagScope.BOTH;

    @Column(name = "status", nullable = false, columnDefinition = "tag_status")
    @ColumnTransformer(write = "?::tag_status")
    private TagStatus status = TagStatus.APPROVED;

    @OneToMany(mappedBy = "tag", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TagTranslation> translations = new ArrayList<>();
}
