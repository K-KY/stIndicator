package st.indicator.stindicator.presentation.ws.service;

import st.indicator.stindicator.domain.entity.PositionMonitor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class PositionMonitorRuntimeState {
    private final AtomicReference<PositionMonitor> snapshot;
    private final AtomicBoolean closeTriggered = new AtomicBoolean(false);
    private final AtomicLong lastEventTime = new AtomicLong(0L);

    public PositionMonitorRuntimeState(PositionMonitor snapshot) {
        this.snapshot = new AtomicReference<>(snapshot);
    }

    public AtomicReference<PositionMonitor> snapshot() {
        return snapshot;
    }

    public AtomicBoolean closeTriggered() {
        return closeTriggered;
    }

    public AtomicLong lastEventTime() {
        return lastEventTime;
    }
}
