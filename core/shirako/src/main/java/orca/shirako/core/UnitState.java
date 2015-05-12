package orca.shirako.core;

public enum UnitState {
    DEFAULT, PRIMING, ACTIVE, MODIFYING, CLOSING, CLOSED, FAILED;
    
    /**
     * Returns true if the passed in state represents a pending state.
     * A state is pending if it is associated with an in progress operation.
     * Generally, all states that end in -ing are pending.
     * @param state
     * @return
     */
    public static boolean isPending(UnitState state){
        return (state == PRIMING || state == MODIFYING || state == CLOSING);
    }
    
    public static boolean isPendingModifying(UnitState state){
        return (state == MODIFYING);
    }
    
    public static UnitState convert(int ordinal) {
        for (UnitState s : values()){
            if (s.ordinal() == ordinal) {
                return s;
            }
        }
        throw new RuntimeException("Ordinal does not have a matching enum value: " + ordinal);
    }
}