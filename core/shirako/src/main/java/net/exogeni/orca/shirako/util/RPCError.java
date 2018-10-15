package net.exogeni.orca.shirako.util;

public enum RPCError {  
    Unknown,
    Authorization,
    InvalidRequest,
    LocalError,
    RemoteError,
    NetworkError,
    Timeout;

    public static RPCError convert(int ordinal) {
        for (RPCError s : values()){
            if (s.ordinal() == ordinal) {
                return s;
            }
        }
        throw new RuntimeException("Ordinal does not have a matching enum value: " + ordinal);
    }


};
