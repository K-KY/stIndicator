package st.indicator.stindicator.infra.connector.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

// 종료된 관리 포지션에 사용자의 진입 이유와 매매 복기를 연결하는 기록 엔티티
@Entity
@Table(name = "managed_position_journal")
public class ManagedPositionJournalEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "managed_position_id", nullable = false, unique = true)
    private ManagedPositionEntity managedPosition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    private String entryReason;

    @Lob
    private String content;

    @Lob
    private String review;

    @Column(length = 500)
    private String tags;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public ManagedPositionJournalEntity() {
    }

    public static ManagedPositionJournalEntity create(ManagedPositionEntity position, Long userId,
                                                      String title, String entryReason,
                                                      String content, String review, String tags) {
        ManagedPositionJournalEntity entity = new ManagedPositionJournalEntity();
        entity.managedPosition = position;
        entity.user = userId == null ? null : UserEntity.reference(userId);
        entity.update(title, entryReason, content, review, tags);
        return entity;
    }

    public void update(String title, String entryReason, String content, String review, String tags) {
        this.title = title == null || title.isBlank() ? defaultTitle() : title;
        this.entryReason = entryReason;
        this.content = content;
        this.review = review;
        this.tags = tags;
    }

    private String defaultTitle() {
        if (managedPosition == null) {
            return "매매일지";
        }
        return managedPosition.getSymbol() + " " + managedPosition.getEntrySide() + " 매매일지";
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

    public Long getId() { return id; }
    public ManagedPositionEntity getManagedPosition() { return managedPosition; }
    public UserEntity getUser() { return user; }
    public String getTitle() { return title; }
    public String getEntryReason() { return entryReason; }
    public String getContent() { return content; }
    public String getReview() { return review; }
    public String getTags() { return tags; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
