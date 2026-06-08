package st.indicator.stindicator.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원가입 요청")
public class AuthRegisterRequestDto {
    @Schema(description = "로그인과 메일 인증에 사용할 이메일", example = "user@example.com")
    private String email;

    @Schema(description = "로그인 비밀번호")
    private String password;

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}
