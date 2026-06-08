package st.indicator.stindicator.application.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import st.indicator.stindicator.domain.entity.User;
import st.indicator.stindicator.infra.connector.entity.EmailVerificationEntity;
import st.indicator.stindicator.infra.connector.entity.UserEntity;
import st.indicator.stindicator.infra.connector.repository.EmailVerificationJpaRepository;
import st.indicator.stindicator.infra.connector.repository.UserJpaRepository;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AuthService {
    private final UserJpaRepository userJpaRepository;
    private final EmailVerificationJpaRepository emailVerificationJpaRepository;
    private final PasswordHashService passwordHashService;
    private final JavaMailSender mailSender;

    public AuthService(UserJpaRepository userJpaRepository,
                       EmailVerificationJpaRepository emailVerificationJpaRepository,
                       PasswordHashService passwordHashService,
                       JavaMailSender mailSender) {
        this.userJpaRepository = userJpaRepository;
        this.emailVerificationJpaRepository = emailVerificationJpaRepository;
        this.passwordHashService = passwordHashService;
        this.mailSender = mailSender;
    }

    @Transactional
    public User register(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        String passwordHash = passwordHashService.hash(password);
        userJpaRepository.findByEmail(normalizedEmail)
                .ifPresent(user -> {
                    if (user.isEmailVerified()) {
                        throw new IllegalArgumentException("이미 가입된 이메일입니다.");
                    }
                });

        UserEntity user = userJpaRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> userJpaRepository.save(new UserEntity(
                        null,
                        normalizedEmail,
                        passwordHash,
                        false,
                        null,
                        null
                )));
        if (!user.isEmailVerified()) {
            user.updatePasswordHash(passwordHash);
            user = userJpaRepository.save(user);
        }

        sendVerification(user);
        return user.toDomain();
    }

    @Transactional
    public User verify(String email, String code) {
        UserEntity user = userJpaRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new IllegalArgumentException("가입 정보를 찾을 수 없습니다."));
        EmailVerificationEntity verification = emailVerificationJpaRepository
                .findFirstByUser_IdAndCodeAndUsedFalseOrderByCreatedAtDesc(user.getId(), code)
                .orElseThrow(() -> new IllegalArgumentException("인증 코드가 올바르지 않습니다."));

        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("인증 코드가 만료되었습니다.");
        }

        verification.markUsed();
        user.verifyEmail();
        return userJpaRepository.save(user).toDomain();
    }

    public User login(String email, String password) {
        UserEntity user = userJpaRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));
        if (!user.isEmailVerified()) {
            throw new IllegalArgumentException("메일 인증이 필요합니다.");
        }
        if (!passwordHashService.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        return user.toDomain();
    }

    public Optional<User> findById(Long userId) {
        return userJpaRepository.findById(userId).map(UserEntity::toDomain);
    }

    private void sendVerification(UserEntity user) {
        String code = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
        emailVerificationJpaRepository.save(new EmailVerificationEntity(user, code, LocalDateTime.now().plusMinutes(10)));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("[stIndicator] 이메일 인증 코드");
        message.setText("인증 코드: " + code + "\n10분 안에 입력해주세요.");
        mailSender.send(message);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("이메일은 필수입니다.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
