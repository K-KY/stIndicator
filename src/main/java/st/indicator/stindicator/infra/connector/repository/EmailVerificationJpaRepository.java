package st.indicator.stindicator.infra.connector.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import st.indicator.stindicator.infra.connector.entity.EmailVerificationEntity;

import java.util.Optional;

public interface EmailVerificationJpaRepository extends JpaRepository<EmailVerificationEntity, Long> {
    Optional<EmailVerificationEntity> findFirstByUser_IdAndCodeAndUsedFalseOrderByCreatedAtDesc(Long userId, String code);
}
