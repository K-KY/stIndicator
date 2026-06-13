package st.indicator.stindicator.domain.entity;

public enum ManagedStopUpdateReason {
    PRICE_TRIGGER_REACHED,
    PNL_TRIGGER_REACHED,
    MANUAL_UPDATE,
    MIGRATION,
    SYSTEM_RESTORE
}
