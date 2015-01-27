package orca.ndl;

import java.util.Set;
import java.util.TreeSet;

public class TestOrderedPair {

	private static class OrderedPair1 {
		private final String one;
		
		public OrderedPair1(String o, String t) {
			if (o.compareTo(t) > 0) { 
				one = o + t;
			} else {
				one = t + o;
			}
		}
		
		public boolean equals(OrderedPair1 op) {
			System.out.println("Calling equals on " + this + " and " + op);
			if (one.equals(op.one))
				return true;
			return false;
		}
		
		public String toString() {
			return one;
		}
		
	}
	
	private static class OrderedPair implements Comparable {
		private final String one, two;
		public OrderedPair(String o, String t) {
			if (o.compareTo(t) > 0) { 
				one = o; two = t;
			} else {
				two = o; one = t;
			}
		}
		public boolean equals(OrderedPair op) {
			if (one.equals(op.one) && two.equals(op.two))
				return true;
			return false;
		}
		
		public int hashCode() {
			return one.hashCode() + two.hashCode();
		}
		
		public String toString() {
			return "one=" + one + " two=" + two;
		}
		
		public int compareTo(Object pp) {
			if (pp instanceof OrderedPair) {
				OrderedPair p = (OrderedPair)pp;
				if (one.equals(p.one) && two.equals(p.two))
					return 0;
				if (one.compareTo(p.one) > 0)
					return 1;
				if ((one.compareTo(p.one) == 0) && (two.compareTo(p.two) > 0))
					return 1;
				return -1;
			} 
			return -1;
		}
		
	}
	public static void main(String[] argv) {
		
		Set<OrderedPair> s = new TreeSet<OrderedPair>();
		
		OrderedPair p1 = new OrderedPair("a", "b");
		
		OrderedPair p2 = new OrderedPair("b", "a");
		
		OrderedPair p3 = new OrderedPair("a", "b");
		
		System.out.println(p1);
		System.out.println(p2);
		
		if (p1.equals(p2))
			System.out.println("Equals");
		else
			System.out.println("Not equals");
		
		System.out.println("Adding");
		s.add(p1);
		
		System.out.println("Checking p1");
		if (s.contains(p1)) 
			System.out.println("Contains p1");
		else
			System.out.println("Not contains p1");
		
		System.out.println("Checking p2");
		if (s.contains(p2)) 
			System.out.println("Contains p2");
		else
			System.out.println("Not contains p2");
		
		System.out.println("Checking p3");
		if (s.contains(p3)) 
			System.out.println("Contains p3");
		else
			System.out.println("Not contains p3");
	}
}
