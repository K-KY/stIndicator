package st.indicator.stindicator.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "메일 인증 요청")
public class AuthVerifyRequestDto {
    @Schema(description = "인증할 이메일", example = "user@example.com")
    private String email;

    @Schema(description = "메일로 발송된 6자리 인증 코드", example = "123456")
    private String code;

    public String getEmail() {
        return email;
    }

    public String getCode() {
        return code;
    }
}
