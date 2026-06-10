package st.indicator.stindicator.presentation.dto;

// 종료된 관리 포지션에 연결할 매매일지 작성/수정 요청 DTO
public class ManagedPositionJournalRequestDto {
    private String title;
    private String entryReason;
    private String content;
    private String review;
    private String tags;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getEntryReason() {
        return entryReason;
    }

    public void setEntryReason(String entryReason) {
        this.entryReason = entryReason;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getReview() {
        return review;
    }

    public void setReview(String review) {
        this.review = review;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }
}
