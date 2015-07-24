package orca.controllers.xmlrpc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * A white list is a collection of patterns or usernames that the incoming
 * user should match before being allowed further. Used in Orca's create slice to
 * match DNs or URNs from certs.
 * @author ibaldin
 *
 */
public class UserWhitelist {
	private static final String LINE_SEPARATOR = "line.separator";
	List<String> list;

	public UserWhitelist(String wl) {
		initialize(wl);
	}

	public UserWhitelist(File f) throws Exception {
		if (!f.exists()) 
			throw new Exception("Whitelist file " + f.getName() + " does not exist");
		
		initialize(fileToString(f));
	}
	
	private void initialize(String wl) {
		list = new ArrayList<String>(Arrays.asList(wl.split(System.getProperty(LINE_SEPARATOR))));
		Iterator<String> it = list.iterator();
		while(it.hasNext()) {
			String s = it.next();
			if (s.startsWith("#"))
				it.remove();
		}
	}
	
	public boolean onWhiteList(String s) {
		// see if s matches one of the patterns on whitelist

		if ((s == null) || (s.length() == 0))
			return false;

		s = s.trim();

		for (String pat: list) {
			if (s.indexOf(pat) > 0) {
				return true;
			}

			if (s.matches(pat)) {
				return true;
			}
		}

		return false;

	}

	public static String fileToString(File f) throws Exception {
		StringBuilder sb;

		BufferedReader bin = null;
		FileInputStream is = new FileInputStream(f);
		bin = new BufferedReader(new InputStreamReader(is, "UTF-8"));

		sb = new StringBuilder();
		String line = null;
		while((line = bin.readLine()) != null) {
			sb.append(line);
			// re-add line separator
			sb.append(System.getProperty(LINE_SEPARATOR));
		}

		bin.close();
		is.close();

		return sb.toString();
	}
}
