package kz.bejiihiu.hub.connection;

import kz.bejiihiu.hub.telemetry.ConnectFailureReason;

public record ConnectResult(boolean success, ConnectFailureReason failureReason, String connectedService, String playerMessage) {
}
