package st.indicator.stindicator.domain.entity;

import java.time.LocalDateTime;

/**
 * 세션 로그인과 사용자별 시장 데이터 구독을 식별하는 사용자 도메인 엔티티다.
 */
public class User {
    private final Long id;
    private final String email;
    private final boolean emailVerified;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public User(Long id, String email, boolean emailVerified, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.email = email;
        this.emailVerified = emailVerified;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
