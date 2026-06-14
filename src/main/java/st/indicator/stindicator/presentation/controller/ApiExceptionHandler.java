package st.indicator.stindicator.presentation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import st.indicator.stindicator.application.exception.InsufficientAvailableBalanceException;

import java.math.BigDecimal;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InsufficientAvailableBalanceException.class)
    public ResponseEntity<InsufficientBalanceError> handleInsufficientBalance(
            InsufficientAvailableBalanceException exception
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new InsufficientBalanceError(
                "INSUFFICIENT_AVAILABLE_BALANCE",
                exception.getMessage(),
                exception.getAvailableBalance(),
                exception.getRequiredMargin(),
                exception.getShortage()
        ));
    }

    public record InsufficientBalanceError(
            String code,
            String message,
            BigDecimal availableBalance,
            BigDecimal requiredMargin,
            BigDecimal shortage
    ) {
    }
}
