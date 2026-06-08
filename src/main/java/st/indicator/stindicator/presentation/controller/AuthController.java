package st.indicator.stindicator.presentation.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.RestController;
import st.indicator.stindicator.application.service.AuthService;
import st.indicator.stindicator.application.service.SessionUser;
import st.indicator.stindicator.domain.entity.User;
import st.indicator.stindicator.presentation.dto.AuthLoginRequestDto;
import st.indicator.stindicator.presentation.dto.AuthRegisterRequestDto;
import st.indicator.stindicator.presentation.dto.AuthUserResponseDto;
import st.indicator.stindicator.presentation.dto.AuthVerifyRequestDto;

@RestController
public class AuthController implements AuthApi {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public AuthUserResponseDto register(AuthRegisterRequestDto request) {
        return AuthUserResponseDto.from(authService.register(request.getEmail(), request.getPassword()));
    }

    @Override
    public AuthUserResponseDto verify(AuthVerifyRequestDto request) {
        return AuthUserResponseDto.from(authService.verify(request.getEmail(), request.getCode()));
    }

    @Override
    public AuthUserResponseDto login(AuthLoginRequestDto request, HttpSession session) {
        User user = authService.login(request.getEmail(), request.getPassword());
        session.setAttribute(SessionUser.USER_ID, user.getId());
        session.setAttribute(SessionUser.USER_EMAIL, user.getEmail());
        return AuthUserResponseDto.from(user);
    }

    @Override
    public void logout(HttpSession session) {
        session.invalidate();
    }

    @Override
    public AuthUserResponseDto me(HttpSession session) {
        Object userId = session.getAttribute(SessionUser.USER_ID);
        if (!(userId instanceof Long id)) {
            return null;
        }
        return authService.findById(id)
                .map(AuthUserResponseDto::from)
                .orElse(null);
    }
}
