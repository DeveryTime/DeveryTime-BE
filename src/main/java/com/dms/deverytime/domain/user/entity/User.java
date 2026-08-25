package com.dms.deverytime.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(
        name = "uk_user", columnNames = {"school_year", "school_number"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private Integer schoolYear;

    @Column(nullable = false, length = 4)
    private String schoolNumber;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(nullable = false, length = 10, unique = true)
    private String username;

    private LocalDateTime usernameUpdatedAt;

    @Column(nullable = false)
    private String passwordHash;

    @Column(length = 500)
    private String profileImageUrl;

    @Builder
    public User(int schoolYear, String schoolNumber, String name, String email, String username, String passwordHash){
        this.schoolYear = schoolYear;
        this.schoolNumber = schoolNumber;
        this.name = name;
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public void usernameUpdate(String username){
        this.username = username;
        this.usernameUpdatedAt = LocalDateTime.now();
    }

    public void profileImgUpdate(String profileImageUrl){
        this.profileImageUrl = profileImageUrl;
    }
}
