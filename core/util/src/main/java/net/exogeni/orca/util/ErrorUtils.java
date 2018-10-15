package net.exogeni.orca.util;

public class ErrorUtils {

    public static String getStackTrace(Exception e) {
        return getStackTrace(e.getStackTrace());
    }
    
    public static String getStackTrace(StackTraceElement[] trace) {
        StringBuffer sb = new StringBuffer();

        sb.append("Exception stack trace: \n");

        for (int i = 0; i < trace.length; i++) {
            sb.append(trace[i].toString());
            sb.append("\n");
        }

        return sb.toString();
    }

}
