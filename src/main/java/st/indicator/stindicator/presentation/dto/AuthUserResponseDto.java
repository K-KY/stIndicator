package st.indicator.stindicator.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import st.indicator.stindicator.domain.entity.User;

@Schema(description = "세션 사용자 응답")
public class AuthUserResponseDto {
    @Schema(description = "사용자 ID")
    private final Long id;

    @Schema(description = "이메일")
    private final String email;

    @Schema(description = "메일 인증 완료 여부")
    private final boolean emailVerified;

    public AuthUserResponseDto(Long id, String email, boolean emailVerified) {
        this.id = id;
        this.email = email;
        this.emailVerified = emailVerified;
    }

    public static AuthUserResponseDto from(User user) {
        return new AuthUserResponseDto(user.getId(), user.getEmail(), user.isEmailVerified());
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
}
