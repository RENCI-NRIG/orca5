package orca.shirako.common.meta;


public enum ResourcePoolAttributeType {
    UNDEFINED, INTEGER, FLOAT, STRING, NDL, CLASS;
    
    public static ResourcePoolAttributeType convert(int ordinal) {
        for (ResourcePoolAttributeType s : values()){
            if (s.ordinal() == ordinal) {
                return s;
            }
        }
        throw new RuntimeException("Ordinal does not have a matching enum value: " + ordinal);
    }
}