package com.dms.deverytime.domain.category.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@Table(name = "category")
@Entity
@Getter
public class Category {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        //카테고리에 값 필수, 최대 50자, 중복 X
        @Column(nullable = false, length = 50, unique = true)
        private String name;

        public Category(String name) {
            this.name = name;
    }
}
