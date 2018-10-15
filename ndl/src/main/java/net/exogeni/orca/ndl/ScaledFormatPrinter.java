/**
 * Lifted from StackOverflow
 * http://stackoverflow.com/questions/5036470/automatically-format-a-measurement-into-engineering-units-in-java
 * 
 */
package net.exogeni.orca.ndl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Class made for printing scaled values of integers. Example: you can print value of bandwidth=10000000 as
 * System.out.println(new ScaledFormatPrinter(bandwidth, "bps")) and you will see 10Mbps as the result.
 * 
 * Works for positive and negative exponents
 *
 */
public class ScaledFormatPrinter {

    public static final Map<Integer, String> prefixes;
    static {
        Map<Integer, String> tempPrefixes = new HashMap<Integer, String>();
        tempPrefixes.put(0, "");
        tempPrefixes.put(3, "k");
        tempPrefixes.put(6, "M");
        tempPrefixes.put(9, "G");
        tempPrefixes.put(12, "T");
        tempPrefixes.put(15, "P");
        tempPrefixes.put(-3, "m");
        tempPrefixes.put(-6, "u");
        tempPrefixes.put(-9, "n");
        prefixes = Collections.unmodifiableMap(tempPrefixes);
    }

    String type;
    double value;

    /**
     * Create an instance of a ScaledFormatPrinter. Type is any string containing measurement units e.g. "hz" or "s" or
     * "bps". Example: you can print value of bandwidth=10000000 as System.out.println(new
     * ScaledFormatPrinter(bandwidth, "bps")) and you will see 10Mbps as the result.
     * 
     * @param value value
     * @param type type
     */
    public ScaledFormatPrinter(double value, String type) {
        this.value = value;
        this.type = type;
    }

    public String toString() {
        double tval = value;
        int order = 0;
        while (tval >= 1000.0) {
            tval /= 1000.0;
            order += 3;
        }
        while (tval < 1.0) {
            tval *= 1000.0;
            order -= 3;
        }
        return tval + prefixes.get(order) + type;
    }
}
