package st.indicator.stindicator.application.exception;

public class BalanceFetchFailException extends RuntimeException {
    public BalanceFetchFailException(Exception e, String message) {
        super(message, e);
    }
}
