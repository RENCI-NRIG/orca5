package orca.policy.core.util;

import java.util.HashSet;
import java.util.StringTokenizer;

public class IPv4Set {
    public final String EntrySeparator = ",";
    public final String SubnetMark = "/";
    public final String RangeMark = "-";
    
    protected HashSet<Long> free = new HashSet<Long>();
    protected HashSet<Long> allocated = new HashSet<Long>();
    
    public IPv4Set() {
    }

    public IPv4Set(String list) throws IllegalArgumentException {
        add(list);
    }
    
    public void add(String list) throws IllegalArgumentException {
        StringTokenizer st = new StringTokenizer(list, EntrySeparator);
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (token.lastIndexOf(SubnetMark) > -1) {
                processSubnet(token);
            } else if (token.lastIndexOf(RangeMark) > -1) {
                processRange(token);
            } else {
                processSingle(token);
            }
        }
    }    
    
    protected void processSubnet(String token) throws IllegalArgumentException {
        String[] tokens = token.split(SubnetMark);
        if (tokens.length != 2) {
            throw new IllegalArgumentException("Invalid subnet: " + token);
        }
        long base = toIPv4(tokens[0]);
        int size = Integer.parseInt(tokens[1]);
        if (size < 16 || size > 32) {
            throw new IllegalArgumentException("Invalid subnet size: " + size);
        }
        long mask = 0;
        for (int i = 0; i < size; i++) {
            mask |= (1L << (31-i));
        }
        long start = base;
        long m = ~((int)mask &0xFFFFFFFF);
        long end = base | m;
        
        // last ip is broadcast?
        for (;start < end; start++) {
            free.add(new Long(start));
        }
    }
    
    protected void processRange(String token) throws IllegalArgumentException { 
        String tokens[] = token.split(RangeMark);
        if (tokens.length != 2) {
            throw new IllegalArgumentException("Invalid range: " + token);
        }
        String start = padIfNeeded(tokens[0]);
        long startIp = toIPv4(start);
        String end = tokens[1];
        if (end.indexOf(".") != -1) {
            // must be a full ip;
        } else {
            // just the last component
            end = start.substring(0, start.lastIndexOf(".")) + "." + end;            
        }
        long endIp = toIPv4(end);
        long size = endIp - startIp + 1;
        if (size < 0 || size > 65536) {
            throw new IllegalArgumentException("Range must be positive and less than 65536");
        }
        for (int i = 0; i < size; i++) {
            free.add(new Long(startIp + i));
        }
    }
    
    protected String padIfNeeded(String token) {
        int index = 0;
        int count = 0;
        index = token.indexOf(".");
        while (index != -1) {
            count++;
            index = token.indexOf(".", index+1);
        }
        for (int i = 0; i < 3 - count; i++) {
            token = token + ".0";
        }
        return token;
    }
    
    protected void processSingle(String token) throws IllegalArgumentException {
        free.add(new Long(toIPv4(token)));
    }
    
    
    public String allocate() {
        Long item = free.iterator().next();
        free.remove(item);
        allocated.add(item);
        return toString(item.longValue());
    }
        
    public void free(String ip) {
        long val = toIPv4(ip);
        Long l = new Long(val);
        allocated.remove(l);
        free.add(l);
    }
    
    public void reserve(String ip) {
        Long l = new Long(toIPv4(ip));
        free.remove(l);
        allocated.add(l);
    }
    
    public int getFreeCount() {
        return free.size();
    }
    
    public boolean isFree(String ip) {
        long i = toIPv4(ip);
        return free.contains(new Long(i));
    }
    
    public boolean isFree(long ip) {
        return free.contains(new Long(ip));
    }

    public boolean isAllocated(String ip) {
        long i = toIPv4(ip);
        return allocated.contains(new Long(i));
    }
    
    public boolean isAllocated(long ip) {
        return allocated.contains(new Long(ip));
    }

    public static long toIPv4(String ip) throws IllegalArgumentException {
        String[] tokens = ip.split("\\.");
        if (tokens.length != 4) {
            throw new IllegalArgumentException("Invalid ip address: " + ip);
        }
        long result = 0;
        try {
            for (int i = 0; i < tokens.length; i++) {
                long number = Integer.parseInt(tokens[i]);
                if (number < 0 || number > 255) {
                    throw new IllegalArgumentException("Invalid ip address: " + ip);
                }
                result = result | ((number & 0xFF) << ((3-i)*8));
            }
        } catch(NumberFormatException e) {
            throw new IllegalArgumentException("Invalid ip address: " + ip, e);
        }
        return result;
    }
    
    public static String toString(long ip) {
        return 
        ((ip >> 24) &0xFF) + "." + 
        ((ip >> 16) & 0xFF) + "." + 
        ((ip >> 8) & 0xFF)  + "." + 
        (ip & 0xFF);
    }
    
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	
    	sb.append("IP Free: " + free);
    	sb.append("IP allocated: " + allocated);
    	
    	return sb.toString();
    }
}