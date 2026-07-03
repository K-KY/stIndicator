package st.indicator.stindicator.presentation.dto;

/** 지속 상태와 구분하여 특정 캔들에서 한 번 발생하는 시그널 이벤트다. */
public enum SignalEventType {
    GOLDEN_CROSS,
    DEAD_CROSS,
    OVERSOLD_RECOVERY,
    OVERBOUGHT_REJECTION,
    BULLISH_MIDDLE_CROSS,
    BEARISH_MIDDLE_CROSS,
    BULLISH_RECLAIM,
    BEARISH_REJECTION
}
