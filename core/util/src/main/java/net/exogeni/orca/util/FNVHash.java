/*
 * Copyright (C) 2004-2007 Duke University. This software is distributed under
 * the terms of the Eclipse Public License Version 1.0 found in
 * the file named LICENSE.Eclipse, which was shipped with this distribution.
 * Any use, reproduction or distribution of this software constitutes
 * the recipient's acceptance of the Eclipse license terms.
 * This notice and the full text of the license must be included with any
 * distribution of this software.
 */

package net.exogeni.orca.util;

/**
 * Implements the 32-bit FNV-1a hash function. Intended for use in hashing guids to
 * make them easier to read in logs. Takes a string and returns a 32-bit hex
 * hashed representation.
 * 
 * Implementation based on description by the original authors found at 
 * http://isthe.com/chongo/tech/comp/fnv
 * 
 * @author varun@cs.duke.edu
 *
 */
public class FNVHash {
	private static final long FNVPrime = 16777619l;
	private static final long offsetBasis = 2166136261l;
	
	/**
	 * Takes a string and returns a 32-bit hex representation as a string
	 * @param str String to be hashed
	 * @return 32-bit hex representation
	 */
	public static String hash (final String str) {
		long hash = offsetBasis;
		for(int octet: str.getBytes()) {
			hash ^= octet;
			hash *= FNVPrime;
		}
		return Integer.toHexString((int)(0xffffffffl & hash)).toUpperCase();
	}
	
	public static void test() {
		String[] TestStrings = {"Test String 1", "Test String 1","test string 1", "TeSt sTring 1",
				"Lorem ipsum dolor sit amet, consectetur adipisicing elit, " +
				"sed do eiusmod tempor incididunt ut labore et dolore magna aliqua"};
		for (String string: TestStrings) {
			System.out.println(hash(string));
		}
	}
}
