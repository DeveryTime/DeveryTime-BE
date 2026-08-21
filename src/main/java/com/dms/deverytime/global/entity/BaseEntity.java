package com.dms.deverytime.global.entity;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.LastModifiedDate;
import java.time.LocalDateTime;

@Getter
@MappedSuperclass
public abstract class BaseEntity extends BaseCreatedEntity{

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
