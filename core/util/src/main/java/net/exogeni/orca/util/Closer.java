package net.exogeni.orca.util;

import java.io.Closeable;
import java.io.IOException;


public class Closer {
	public static void close(Closeable s) {
		try {
			s.close();
		} catch (IOException e){
		}
	}
}
