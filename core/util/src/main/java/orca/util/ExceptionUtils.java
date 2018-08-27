package orca.util;

public class ExceptionUtils
{
    /**
     * Converts the given stack trace into a string
     * @param trace Stack trace
     * @return returns stack trace string
     */
    public static String getStackTraceString(StackTraceElement[] trace)
    {
        StringBuffer sb = new StringBuffer();

        sb.append("Exception stack trace: \n");

        for (int i = 0; i < trace.length; i++) {
            sb.append(trace[i].toString());
            sb.append("\n");
        }

        return sb.toString();
    }
}
