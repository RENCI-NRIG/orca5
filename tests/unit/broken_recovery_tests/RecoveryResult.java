package orca.tests.unit.recovery;

import orca.shirako.common.ReservationState;

public class RecoveryResult {
    public int code;
    public ReservationState state;

    public RecoveryResult(final int code, final ReservationState state) {
        this.code = code;
        this.state = state;
    }
}
