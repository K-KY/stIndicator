package st.indicator.stindicator.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 요청")
public class AuthLoginRequestDto {
    @Schema(description = "가입 이메일", example = "user@example.com")
    private String email;

    @Schema(description = "비밀번호")
    private String password;

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}
