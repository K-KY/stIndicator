package st.indicator.stindicator.infra.connector.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * 사용자별 차트 지표 설정을 저장하는 엔티티.
 * EMA/SMA 선 목록, 색상, 볼린저 밴드, VWAP 표시 여부 같은 프론트 차트 설정을 JSON 문자열로 보관한다.
 */
@Entity
@Table(name = "chart_indicator_setting", uniqueConstraints = {
        @UniqueConstraint(name = "uk_chart_indicator_setting_user", columnNames = {"user_id"})
})
public class ChartIndicatorSettingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Lob
    @Column(nullable = false)
    private String settingsJson;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public ChartIndicatorSettingEntity() {
    }

    public ChartIndicatorSettingEntity(Long id, UserEntity user, String settingsJson,
                                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.user = user;
        this.settingsJson = settingsJson;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ChartIndicatorSettingEntity create(Long userId, String settingsJson) {
        return new ChartIndicatorSettingEntity(null, UserEntity.reference(userId), settingsJson, null, null);
    }

    public void updateSettings(String settingsJson) {
        this.settingsJson = settingsJson;
    }

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getSettingsJson() {
        return settingsJson;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
