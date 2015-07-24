package orca.ndl;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.xerces.dom.DOMInputImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSParser;
import org.w3c.dom.ls.LSSerializer;

import com.hp.hpl.jena.rdf.model.Resource;
import com.sun.org.apache.xml.internal.resolver.helpers.PublicId;

/** 
 * A few helper functions to go from NDL to RSpec
 * @author ibaldin
 *
 */
@SuppressWarnings("restriction")
public class NdlToRSpecHelper {
	private static final String UNKNOWN = "unknown";
	public static String CM_URN_PATTERN = "urn:publicid:IDN+exogeni.net:@+authority+am";
	public static String CONTROLLER_URL_PATTERN = "https://@-hn.exogeni.net:11443/orca/xmlrpc";
//	public static String SLIVER_URN_PATTERN = "urn:publicid:IDN+exogeni.net+sliver+%";
//	public static String SLIVER_URN_EXT_PATTERN = "urn:publicid:IDN+exogeni.net:@+sliver+%";
	public static String COMPONENT_URN_PATTERN = "urn:publicid:IDN+exogeni.net:@+^+%";
	public static String SLIVER_URN_PATTERN = "urn:publicid:IDN+@+^+%";
	public static String COMPONENT_URN_GLOBAL_PATTERN = "urn:publicid:IDN+exogeni.net+^+%";
	public static String UNKNOWN_COMPONENT_URN = "urn:publicid:IDN+exogeni.net+component+unknown";
	public static String UNKNOWN_CM_URN = "urn:publicid:IDN+exogeni.net:unknown+authority+cm";
	public static String UNKNOWN_SLIVER_URN = "urn:publicid:IDN+exogeni.net+sliver+unknown";
	private static Pattern pattern = Pattern.compile("http://.+/([\\w-]+).rdf#\\1/.+"), pattern1 = Pattern.compile("http://.+/([\\w-]+).rdf#.+"),
			urlPattern = Pattern.compile("https://([\\w]+)-hn.exogeni.net:[\\d]+/orca/xmlrpc");
	
	// for exceptions from the rule
	private static final Map<String, String> mapUrlToCm;
	
	static {
		Map<String, String> tmpM = new HashMap<String, String>();
		tmpM.put("https://geni-test.renci.org:11443/orca/xmlrpc", "exogeni.net:testvmsite");
		tmpM.put("https://geni.renci.org:11443/orca/xmlrpc", "exogeni.net");
		tmpM.put("https://uva-nl-hn.exogeni.net:11443/orca/xmlrpc", "exogeni.net:uvanlvmsite");

		mapUrlToCm = Collections.unmodifiableMap(tmpM);
	}
	
	public static String getControllerForUrl(String url) {
		if (mapUrlToCm.containsKey(url))
			return mapUrlToCm.get(url);
		else {
			Matcher matcher = urlPattern.matcher(url);
			if (matcher.matches()) {
				return "exogeni.net:" + matcher.group(1) + "vmsite";
			} else 
				return "exogeni.net:unknownvmsite";
		}
	}
	
	/**
	 * Decode a string that may have been urlencoded
	 * @param s
	 * @return
	 */
	private static String decodeUrlString(String s) {
		if (s == null)
			return s;
		try {
			return URLDecoder.decode(s, "UTF-8");
		} catch (UnsupportedEncodingException uee) {
			return s;
		}
	}
	
	/**
	 * Exception domains for which exo controller is the only valid choice
	 */
	private static final String[] exoDomains = { "nlr", "ion", "ben" };
	/**
	 * Guess controller URL from domain name
	 * @param domainUrl
	 * @return
	 */
	public static String getControllerForSite(String domainUrl) {
		domainUrl = decodeUrlString(domainUrl);
		
		Matcher matcher = pattern.matcher(domainUrl);
		
		if (!matcher.matches() || (matcher.groupCount() != 1))
			return null;
		
		String shortDomain = matcher.group(1).replaceAll("Net", "").replaceAll("vmsite", "");
		for(String special: exoDomains) {
			if (special.equalsIgnoreCase(shortDomain))
				return null;
		}
		
		return CONTROLLER_URL_PATTERN.replaceFirst("@", shortDomain);
	}
	
	/**
	 * Get a urn for CM from a domain URL like so:
	 * domainUrl = http://geni-orca.renci.org/owl/rcivmsite.rdf#rcivmsite/Domain
	 * cmUrn = urn:publicid:IDN+rcivmsite+authority+cm
	 * @param domainUrl
	 * @return
	 */
	public static String cmFromDomain(String domainUrl) {
		String ret = UNKNOWN_CM_URN;
		
		domainUrl = decodeUrlString(domainUrl);
		
		Matcher matcher = pattern.matcher(domainUrl);
		
		if (!matcher.matches() || (matcher.groupCount() != 1))
			return ret;

		return CM_URN_PATTERN.replaceFirst("@", matcher.group(1));
	}
	
	/**
	 * Similar to cmFromDomain, except generates ids (e.g. for stitching extension)
	 * @param domainUrl
	 * @return
	 */
	public static String idFromDomain(String domainUrl) {
		String ret = UNKNOWN;
		
		domainUrl = decodeUrlString(domainUrl);
		
		Matcher matcher = pattern.matcher(domainUrl);
		
		if (!matcher.matches() || (matcher.groupCount() != 1))
			return ret;
		
		return "exogeni.net:" + matcher.group(1);
	}
	
	/**
	 * Assumes pattern xxx.rdf#xxx:switch:port:port:port:type
	 * @param name
	 * @return
	 */
	private static String massageName(final String n) {
		String name = n.replaceAll("[#/]", ":");
		// assuming the pattern xxx.rdf:xxx:switch:port:port:port:type
		String[] names=name.split(":");
		String tName = "";
		for(int i=1; i < names.length - 1; i++) 
			tName += names[i] + ":";
		// remove last :
		if (names.length > 1)
			tName = tName.substring(0, tName.length() - 1);
		else
			tName = names[0];
		
		return name;
	}
	
	public static enum UrnType { 
		Node("node"), 
		Link("link"), 
		StitchPort("stitchport"),
		Sliver("sliver"),
		Interface("interface");
		
		private final String name;
		private UrnType(String n) {
			name = n;
		}
		
		public String toString() {
			return name;
		}
	};
	
	/**
	 * Generate a component id from a URL and a domain URL
	 * @param domainUrl
	 * @param type - [node, link, interface]
	 * @param name - unique name
	 * @return
	 */
	public static String cidUrnFromUrl(String domainUrl, UrnType type, String name) {
		String ret = UNKNOWN_COMPONENT_URN;
		
		domainUrl = decodeUrlString(domainUrl);
		
		Matcher matcher = null;
		
		if (domainUrl != null)
			matcher = pattern.matcher(domainUrl);
		
		if (name == null)
			return ret;
		
		String sName = name;
		if ((UrnType.Interface == type) || (UrnType.Link == type) || (UrnType.StitchPort == type)) {
			name = massageName(name);
			// strip off site.rdf:
			sName = name.replaceFirst(".+.rdf:", "");
		}
		
		if (UrnType.Sliver == type) {
			sName = name.replaceAll("[#/]", ":");
		}
		
		if (domainUrl == null) 
			return COMPONENT_URN_GLOBAL_PATTERN.replaceFirst("\\^", type.toString()).replaceFirst("%", sName);
		
		if ((matcher != null) && !matcher.matches()) {
			Matcher matcher1 = pattern1.matcher(domainUrl);
			if (matcher1.matches()) {
				return COMPONENT_URN_PATTERN.replaceFirst("@", matcher1.group(1)).replaceFirst("\\^", type.toString()).replaceFirst("%", sName);
			} else 
				// no domain
				return COMPONENT_URN_GLOBAL_PATTERN.replaceFirst("\\^", type.toString()).replaceFirst("%", sName);
		}
		
		return COMPONENT_URN_PATTERN.replaceFirst("@", matcher.group(1)).replaceFirst("\\^", type.toString()).replaceFirst("%", sName);
	}
	
	/**
	 * Generate a component id from a URL and a domain URL and concatenation of names
	 * @param domainUrl
	 * @param type - [node, link, interface]
	 * @param name1 - unique name
	 * @param name2 - unique name
	 * @return
	 */
	public static String cidUrnFromUrl(String domainUrl, UrnType type, String name1, String name2) {
		String ret = UNKNOWN_COMPONENT_URN;
		
		domainUrl = decodeUrlString(domainUrl);
		
		Matcher matcher = pattern.matcher(domainUrl);
		
		if ((name1 == null) || (name2 == null))
			return ret;
		
		String sName1 = name1, sName2 = name2;
		if ((UrnType.Interface == type) || (UrnType.Link == type) || (UrnType.StitchPort == type)) {
			name1 = massageName(name1);
			// strip off site.rdf:
			sName1 = name1.replaceFirst(".+.rdf:", "");
			name2 = massageName(name2);
			// strip off site.rdf:
			sName2 = name2.replaceFirst(".+.rdf:", "");
		}
		
		
		if (UrnType.Sliver == type) {
			sName1 = name1.replaceAll("[#/]", ":");
			sName2 = name2.replaceAll("[#/]", ":");
		}
		
		if (!matcher.matches()) {
			Matcher matcher1 = pattern1.matcher(domainUrl);
			if (matcher1.matches()) {
				return COMPONENT_URN_PATTERN.replaceFirst("@", matcher1.group(1)).replaceFirst("\\^", type.toString()).replaceFirst("%", sName1 + "_" + sName2);
			} else 
				return ret;
		}

		return COMPONENT_URN_PATTERN.replaceFirst("@", matcher.group(1)).replaceFirst("\\^", type.toString()).replaceFirst("%", sName1 + "_" + sName2);
	}
	
	/**
	 * Check if there is a specific domain in the urn
	 * @param urn
	 * @param against
	 * @return
	 */
	public static boolean specificDomainCheck(String urn, String against) {
		String pid = PublicId.decodeURN(urn);
		String cd = pid.split(" ")[1];
		return cd.equalsIgnoreCase(against);
	}
	
	/**
	 * Check that the domain matches what is expected (e.g. dom=exogeni.net)
	 * @param urn
	 * @param dom
	 * @return
	 */
	public static boolean domainNameSpaceCheck(String urn, Set<String> dom) {
		String pid = PublicId.decodeURN(urn);
		String cd = pid.split(" ")[1];
		
		for (String d: dom) {
			if ((cd != null) && (cd.startsWith(d)))
				return true;
		}
		return false;
	}
	
	/**
	 * Generate a domain name space according to convention
	 * @param urn
	 * @return
	 */
	public static String generateDomainNameSpace(String urn) {
		String pid = PublicId.decodeURN(urn);
		String dom = pid.split(" ")[1];
		if (dom.split("//").length == 2)
			return dom.split("//")[1] + ".rdf";
		return dom + ".rdf";
	}

	/** generate a site name from domain name URN
	 * 
	 * @param domain
	 * @return
	 */
	public static String generateSiteName(String urn) {
		String pid = PublicId.decodeURN(urn);
		String site = pid.split(" ")[1];
		if (site.split("//").length == 2)
			return site.split("//")[1];
		return site;
	}
	
	/**
	 * Based on urn that has exogeni.net:xxxsite, generate NDL domain xxxsite.rdf#xxxsite
	 * @param urn
	 * @return
	 */
	public static String generateDomainName(String urn) {
		// blah.rdf#blah is the scheme (for vm site domains only!)
		return generateDomainNameSpace(urn) + "#" + generateSiteName(urn);
	}
	
	
//	/**
//	 * Convert a resource URL into a sliver URN.
//	 * @param url
//	 * @return
//	 */
//	public static String sliverUrnFromURL(String url) {
//		if (url == null)
//			return UNKNOWN_SLIVER_URN;
//		
//		url = decodeUrlString(url);
//		
//		String uniq = StringUtils.removeStart(url, NdlCommons.ORCA_NS);
//		if (uniq != null)
//			return SLIVER_URN_PATTERN.replaceFirst("%", uniq);
//		return UNKNOWN_SLIVER_URN;
//	}
//	
//	/**
//	 * Convert a resource URL into a sliver URN (using domain info)
//	 * @param url
//	 * @param dom - either a URL of a domain or a simple name (e.g. rcivmsite)
//	 * @return
//	 */
//	public static String sliverUrnFromURL(String url, String dom) {
//		if (url == null)
//			return UNKNOWN_SLIVER_URN;
//		
//		if (dom == null)
//			return sliverUrnFromURL(url);
//		
//		url = decodeUrlString(url);
//		dom = decodeUrlString(dom);
//		
//		String uniq = StringUtils.removeStart(url, NdlCommons.ORCA_NS);
//		if (uniq == null)
//			return UNKNOWN_SLIVER_URN;
//		
//		String simpleDom = dom;
//		if (dom.startsWith("http://")) {
//			Matcher matcher = pattern.matcher(dom);
//			// if no identifiable domain
//			if (!matcher.matches() || (matcher.groupCount() != 1))
//				return sliverUrnFromURL(url);
//			
//			simpleDom = matcher.group(1);
//		}
//	
//		return SLIVER_URN_EXT_PATTERN.replaceFirst("%", uniq).replaceFirst("@", simpleDom);
//		
//	}
	
	/**
	 * Get sliver URN from sliver name and rack id
	 * @param sid - sliver name
	 * @param rack - exogeni.net:rcivmsite
	 */
	public static String sliverUrnFromRack(String sid, String rack) {

		String sName = sid.replaceAll("[#/]", ":");
		
		return SLIVER_URN_PATTERN.replaceFirst("@", rack).replaceFirst("\\^", UrnType.Sliver.toString()).replaceFirst("%", sName);
	}
	
	/**
	 * Convert a sliver urn into a proper URL. 
	 * @param urn
	 * @return
	 */
	public static String sliverUrlFromURN(String urn) {
		String url = NdlCommons.ORCA_NS;
		
		if (urn == null)
			return url + UNKNOWN;
		
		String pid = PublicId.decodeURN(urn);
		
		String uniq = pid.split(" ")[3];
		if (uniq != null)
			return url + uniq;
		
		return url + UNKNOWN;
	}
	
	/**
	 * Convert a interfacer urn into a proper URL. 
	 * @param urn 
	 * @return
	 */
	public static String interfaceUrlFromURN(String urn) {
		String url = NdlCommons.ORCA_NS;
		
		if (urn == null)
			return url + UNKNOWN;
		
		String pid = PublicId.decodeURN(urn);
		String domain = generateDomainNameSpace(urn);
		String uniq = pid.split(" ")[3];
		if (uniq != null) {
			return url + domain+ "#" + uniq.replace("//", "/");
		}
		return url + domain + "#" + UNKNOWN;
	}
	
	/**
	 * sometimes getLocalName is not good enough
	 * so we strip off orca name space and call it a day
	 */
	public static String getTrueName(Resource r) {
		if (r == null)
			return null;
		
		String shortName = StringUtils.removeStart(r.getURI(), NdlCommons.ORCA_NS);
		return decodeUrlString(shortName);
	}
	
	/**
	 * Equivalent to getTrueName(r.getURI());
	 * @param r
	 * @return
	 */
	public static String getTrueName(String r) {
		if (r == null)
			return null;
		
		String shortName = StringUtils.removeStart(r, NdlCommons.ORCA_NS);
		return decodeUrlString(shortName);
	}

	/**
	 * ORCA equivalent of client id comes in the form of <GUID>#<client id>
	 * so get the client id from that
	 */
	public static String getClientId(Resource r) {
		String ret = getTrueName(r);
		
		return getClientId(ret);
	}
	
	/**
	 * Sometimes we only have a string (not the resource) containing
	 * the client id as above
	 * @param s
	 * @return
	 */
	public static String getClientId(String s) {
		String ret = s;
		
		String[] sp = ret.split("#");
		if (sp.length == 2)
			ret = sp[1];
		
		return ret;
	}
	
	/**
	 * Turn a DOM node into string, with or without namespace declarations,
	 * with or without <xml header
	 * @param n
	 * @param withNS
	 * @param withHeader
	 * @return
	 * @throws Exception
	 */
	public static String domToString(Node n, boolean withNS, boolean withHeader) throws Exception {
		DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
		DOMImplementationLS impl = 
				(DOMImplementationLS)registry.getDOMImplementation("LS");
		LSSerializer writer = impl.createLSSerializer();
		String str = writer.writeToString(n);
		
		if (!withHeader) {
			str = stripXmlHead(str);
		}
		
		if (withNS)
			return str;
		else
			return stripXmlNs(str);
	}
	
	/**
	 * Strip <?  ?> header from XML string
	 * @param s
	 * @return
	 */
	public static String stripXmlHead(String s) {
		return s.replaceAll("<\\?.*\\?>", "");
	}
	
	/**
	 * Strip namespace declarations from XML string
	 * @return
	 */
	public static String stripXmlNs(String s) {
		return s.replaceAll("xmlns.*?(\"|\').*?(\"|\')", "");
	}
	
	/**
	 * Parse a string into a DOM and return a NodeList 
	 * @param s
	 * @return
	 */
	public static NodeList parseXmlToDOM(String s) {
		try {
			DOMImplementationRegistry registry = 
			    DOMImplementationRegistry.newInstance();

			DOMImplementationLS impl = 
			    (DOMImplementationLS)registry.getDOMImplementation("LS");

			LSParser builder = impl.createLSParser(
			    DOMImplementationLS.MODE_SYNCHRONOUS, null);
				
			LSInput lsi = new DOMInputImpl(null, null, null, s, null);
			Document document = builder.parse(lsi);
			
			return document.getChildNodes();
		} catch (Exception e) {
			return null;
		}
	}
	
	public static void main(String[] args) {
	
		String dom = "http://geni-orca.renci.org/owl/rcivmsite.rdf#rcivmsite/Domain";
		System.out.println("1. Result for " + dom + ": " + cmFromDomain(dom));
		
		String url = "http://geni-orca.renci.org/owl/40b1a531-ebda-4bf5-a595-d5873bab78f6#VLAN0-Node1";
		System.out.println("2. Original URL: " + url);
//		String urn = sliverUrnFromURL(url, dom);
//		System.out.println("3. Result for URL to URN: " + urn);
//		System.out.println("4. Result for URN to URL: " + sliverUrlFromURN(urn));
		
//		urn = sliverUrnFromURL(url, "rcivmsite");
//		System.out.println("5. Result for URL to URN: " + urn);
		
		String link = "http://geni-orca.renci.org/owl/bbnNet.rdf#BbnNet/IBM/G8052/TenGigabitEthernet/1/0/ethernet";
		
		System.out.println("6. Result for link url " + link + ": " + cidUrnFromUrl(link, UrnType.Link, "some-link"));
		 
		System.out.println("7. " + cidUrnFromUrl("http://geni-orca.renci.org/owl/ben-6509.rdf#Duke/Cisco/6509/TenGigabitEthernet/gB/1/ethernet", 
				UrnType.Interface, 
				"ben-6509.rdf#Duke/Cisco/6509/TenGigabitEthernet/gB/1/ethernet"));
		
		System.out.println("Sliver ID: " + sliverUrnFromRack("mySliver", "exogeni.net:rcivmsite"));
		
		String controller="https://ncsu2-hn.exogeni.net:11443/orca/xmlrpc";
		System.out.println("Site acronym from URL: " + controller + " is " + getControllerForUrl(controller));
		
	}
}
