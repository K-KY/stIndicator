package st.indicator.stindicator.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import st.indicator.stindicator.presentation.dto.AuthLoginRequestDto;
import st.indicator.stindicator.presentation.dto.AuthRegisterRequestDto;
import st.indicator.stindicator.presentation.dto.AuthUserResponseDto;
import st.indicator.stindicator.presentation.dto.AuthVerifyRequestDto;

@Tag(name = "Auth", description = "세션 기반 회원가입, 메일 인증, 로그인 API")
@RequestMapping("/api/auth")
public interface AuthApi {
    @PostMapping("/register")
    @Operation(summary = "회원가입", description = "이메일과 비밀번호로 회원을 만들고 인증 메일을 발송한다.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "회원가입 요청 성공")})
    AuthUserResponseDto register(@RequestBody AuthRegisterRequestDto request);

    @PostMapping("/verify")
    @Operation(summary = "메일 인증", description = "메일로 받은 인증 코드를 확인해 계정을 활성화한다.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "메일 인증 성공")})
    AuthUserResponseDto verify(@RequestBody AuthVerifyRequestDto request);

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "메일 인증이 완료된 사용자로 세션 로그인을 수행한다.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "로그인 성공")})
    AuthUserResponseDto login(@RequestBody AuthLoginRequestDto request, HttpSession session);

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "현재 세션을 무효화한다.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "로그아웃 성공")})
    void logout(HttpSession session);

    @GetMapping("/me")
    @Operation(summary = "현재 사용자 조회", description = "현재 세션에 로그인된 사용자를 조회한다.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "세션 사용자 조회 성공")})
    AuthUserResponseDto me(HttpSession session);
}
