package com.sb.sfrigola_core.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@MappedSuperclass
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class BaseEntityMinimal {
    @CreatedDate
    @Column(name="created_at", nullable= false, updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name="created_by", nullable= false, length = 50,  updatable = false)
    private String createdBy;
}
