package net.exogeni.orca.controllers.xmlrpc;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class XMLRPCDateTransport {

    private static final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss Z";

    public static String dateToString(final Date d) {
        SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        return sdf.format(d);
    }

    public static Date stringToDate(final String s) {
        SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        try {
            return sdf.parse(s.trim());
        } catch (ParseException pe) {
            return null;
        }
    }

    public static void main(String[] argv) {
        Date d = new Date();

        System.out.println("Date is " + d);
        System.out.println("Date is " + dateToString(d));
        System.out.println("Date is " + stringToDate(dateToString(d)));
    }
}
