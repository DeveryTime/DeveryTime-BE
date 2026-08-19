package com.dms.devrytime.domain.auth.domain.emailverification;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "email_verifications")
public class EmailVerification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 6)
    private String code;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @Column(nullable = false)
    private int requestCount;

    @Column(nullable = false)
    private LocalDateTime requestWindowStartedAt;

    @Column(nullable = false)
    private boolean isVerified;

    @Column(nullable = false)
    private int verificationAttemptCount;

    @Builder
    public EmailVerification(String email, String code, LocalDateTime expiresAt,
                             LocalDateTime sentAt, LocalDateTime requestWindowStartedAt){
        this.email = email;
        this.code = code;
        this.expiresAt = expiresAt;
        this.sentAt = sentAt;
        this.isVerified = false;
        this.requestCount = 1;
        this.requestWindowStartedAt = requestWindowStartedAt;
        this.verificationAttemptCount = 0;
    }

    public void update(String code, LocalDateTime expiresAt, LocalDateTime sentAt){
        this.code = code;
        this.expiresAt = expiresAt;
        this.sentAt = sentAt;
        this.verificationAttemptCount = 0;
    }

    public void increaseRequestCount(){
        this.requestCount++;
    }

    public void increaseVerificationAttemptCount(){
        this.verificationAttemptCount++;
    }

    public void resetRequestWindow(LocalDateTime now){
        this.requestCount = 1;
        this.requestWindowStartedAt = now;
    }

    public void verify(){
        this.isVerified = true;
    }

}
