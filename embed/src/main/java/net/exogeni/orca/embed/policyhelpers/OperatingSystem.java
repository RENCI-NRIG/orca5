package net.exogeni.orca.embed.policyhelpers;

public enum OperatingSystem {
    LINUX, WINDOWS, UNKNOWN;

    private static OperatingSystem currentSystem = UNKNOWN;

    public static OperatingSystem system() {
        if (currentSystem == UNKNOWN) {
            String osname = System.getProperty("os.name");
            if (osname.startsWith("Linux"))
                currentSystem = LINUX;
            else if (osname.startsWith("Windows"))
                currentSystem = WINDOWS;
            else
                currentSystem = UNKNOWN;
        }

        return currentSystem;

    }
}
