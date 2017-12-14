package orca.tests.unit.recovery;

import java.util.HashSet;

import orca.shirako.common.ReservationState;

public class StateToCheck {
    public ReservationState state;
    public HashSet<ReservationState> expectedImmediate = new HashSet<ReservationState>();
    public HashSet<ReservationState> expectedExpired = new HashSet<ReservationState>();

    public StateToCheck(ReservationState state) {
        this.state = state;
    }

    public String getString(HashSet<ReservationState> set) {
        StringBuffer sb = new StringBuffer();
        sb.append("{");
        for (ReservationState s : set) {
            if (sb.length() > 1) {
                sb.append(",");
            }
            sb.append(s.toString());
        }
        sb.append("}");
        return sb.toString();
    }
}