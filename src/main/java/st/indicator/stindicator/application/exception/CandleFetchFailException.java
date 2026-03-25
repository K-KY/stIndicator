package st.indicator.stindicator.application.exception;

public class CandleFetchFailException extends RuntimeException {
    public CandleFetchFailException(Exception e, String message) {
        super(message, e);
    }
}
