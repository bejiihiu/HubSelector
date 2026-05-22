package kz.bejiihiu.hub.telemetry;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class TelemetryService {
	private final AtomicLong inventoryOpens = new AtomicLong();
	private final AtomicLong smartJoinClicks = new AtomicLong();
	private final AtomicLong connectAttempts = new AtomicLong();
	private final AtomicLong connectSuccess = new AtomicLong();
	private final AtomicLong connectFailures = new AtomicLong();
	private final AtomicLong totalConnectLatencyMs = new AtomicLong();
	private final Map<ConnectFailureReason, AtomicLong> failuresByReason = new EnumMap<>(ConnectFailureReason.class);

	public TelemetryService() {
		for (ConnectFailureReason reason : ConnectFailureReason.values()) {
			this.failuresByReason.put(reason, new AtomicLong());
		}
	}

	public void onInventoryOpen() { this.inventoryOpens.incrementAndGet(); }
	public void onSmartJoinClick() { this.smartJoinClicks.incrementAndGet(); }
	public void onConnectAttempt() { this.connectAttempts.incrementAndGet(); }
	public void onConnectSuccess(long latencyMs) {
		this.connectSuccess.incrementAndGet();
		this.totalConnectLatencyMs.addAndGet(Math.max(0L, latencyMs));
	}
	public void onConnectFailure(ConnectFailureReason reason) {
		this.connectFailures.incrementAndGet();
		this.failuresByReason.getOrDefault(reason, new AtomicLong()).incrementAndGet();
	}

	public String summary() {
		long success = this.connectSuccess.get();
		long avg = success == 0L ? 0L : this.totalConnectLatencyMs.get() / success;
		StringBuilder reasons = new StringBuilder();
		for (Map.Entry<ConnectFailureReason, AtomicLong> entry : this.failuresByReason.entrySet()) {
			reasons.append(entry.getKey().name()).append('=').append(entry.getValue().get()).append(' ');
		}
		return "opens=" + this.inventoryOpens.get()
				+ " smartJoinClicks=" + this.smartJoinClicks.get()
				+ " connectAttempts=" + this.connectAttempts.get()
				+ " connectSuccess=" + success
				+ " connectFailures=" + this.connectFailures.get()
				+ " avgConnectMs=" + avg
				+ " failuresByReason=[" + reasons.toString().trim() + ']';
	}
}
