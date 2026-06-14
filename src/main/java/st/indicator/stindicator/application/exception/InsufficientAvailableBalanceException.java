package st.indicator.stindicator.application.exception;

import java.math.BigDecimal;

/**
 * ATR 주문에 필요한 증거금이 Binance 선물 가용 잔액을 초과한 경우 발생한다.
 */
public class InsufficientAvailableBalanceException extends RuntimeException {
    private final BigDecimal availableBalance;
    private final BigDecimal requiredMargin;
    private final BigDecimal shortage;

    public InsufficientAvailableBalanceException(BigDecimal availableBalance, BigDecimal requiredMargin) {
        super("필요 증거금이 가용 잔액을 초과했습니다.");
        this.availableBalance = availableBalance;
        this.requiredMargin = requiredMargin;
        this.shortage = requiredMargin.subtract(availableBalance).max(BigDecimal.ZERO);
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public BigDecimal getRequiredMargin() {
        return requiredMargin;
    }

    public BigDecimal getShortage() {
        return shortage;
    }
}
