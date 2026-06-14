package st.indicator.stindicator.application.service;

import st.indicator.stindicator.application.dto.AtrOrderPreview;
import st.indicator.stindicator.application.exception.InsufficientAvailableBalanceException;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class ManagedTradeRiskPolicy {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private ManagedTradeRiskPolicy() {
    }

    static void requireSufficientBalance(AtrOrderPreview preview) {
        if (preview == null
                || preview.getAvailableBalance() == null
                || preview.getRequiredMargin() == null) {
            throw new IllegalArgumentException("가용 잔액과 필요 증거금 계산값이 필요합니다.");
        }
        if (preview.getRequiredMargin().compareTo(preview.getAvailableBalance()) > 0) {
            throw new InsufficientAvailableBalanceException(
                    preview.getAvailableBalance(),
                    preview.getRequiredMargin()
            );
        }
    }

    static boolean pnlStopMatched(BigDecimal unrealizedPnl, BigDecimal requiredMargin,
                                  BigDecimal stopPercent, BigDecimal possibleLoss) {
        if (unrealizedPnl == null || requiredMargin == null || requiredMargin.signum() <= 0) {
            return false;
        }
        boolean amountMatched = possibleLoss != null
                && possibleLoss.signum() > 0
                && unrealizedPnl.compareTo(possibleLoss.negate()) <= 0;
        boolean percentMatched = stopPercent != null
                && stopPercent.signum() > 0
                && marginPnlPercent(unrealizedPnl, requiredMargin).compareTo(stopPercent.negate()) <= 0;
        return amountMatched || percentMatched;
    }

    static BigDecimal marginPnlPercent(BigDecimal unrealizedPnl, BigDecimal requiredMargin) {
        if (unrealizedPnl == null || requiredMargin == null || requiredMargin.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return unrealizedPnl.divide(requiredMargin, 10, RoundingMode.HALF_UP).multiply(ONE_HUNDRED);
    }
}
