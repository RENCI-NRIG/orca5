package orca.ndl.elements;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.jwhoisserver.utils.InetIP2UBI;
import net.jwhoisserver.utils.InetNetwork;
import net.jwhoisserver.utils.InetNetworkException;
import orca.ndl.NdlCommons;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;

public class IPAddress extends Label {
	public String address;
	public String netmask;
	public String cidr;
	public IPAddress base_IP;
	
	private static final String IP_PATTERN = "^([\\d]+)[.]([\\d]+)[.]([\\d]+)[.]([\\d]+)$";
	private static final Pattern ipPattern = Pattern.compile(IP_PATTERN);
	private static final String CIDR_PATTERN = "^([\\d]+)[.]([\\d]+)[.]([\\d]+)[.]([\\d]+)/([\\d]+)$";
	private static final Pattern cidrPattern = Pattern.compile(CIDR_PATTERN);
	
	public IPAddress(String a,String m) throws UnknownHostException, InetNetworkException{
		address = a;
		netmask = m;
		type = "IPAddress";
		if(netmask==null)
			netmask = "255.255.255.0";	
		InetAddress addr1_netmask = InetAddress.getByName(netmask);
		if(a!=null){
			InetNetwork addr1_network = new InetNetwork(address,netmask);
			cidr = addr1_network.networkIdentifierCIDR();
		}
		base_IP=this;
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString());
		sb.append(";cidr="+cidr);
		return sb.toString();
	}
	
	public String getCIDR(){
		return cidr;
	}
	
	public String getCIDRAddress(){
		if(null != cidr && cidr.indexOf("/")>=0){
			return cidr.split("/")[0];
		}
		return cidr;
	}
	
	public String getCIDRNetmask(){
		if(cidr.indexOf("/")>=0){
			return cidr.split("/")[1];
		}
		return "24";
	}
	
	public IPAddress getNewIpAddress(OntModel om,String start_ip,String netmask, String label_uri, int i) throws UnknownHostException, InetNetworkException{
		if(i<0)
			return this;
		String new_address=null;
		if(start_ip!=null){
			InetAddress ip = InetAddress.getByName(base_IP.address);
			BigInteger biIP=InetIP2UBI.convertIP2UBI(ip.getAddress());
			BigInteger nbiIP = biIP.add(new BigInteger(new Integer(i).toString()));
			InetAddress nIP = InetIP2UBI.convertUBI2IP(nbiIP,4);
			new_address = nIP.toString().substring(1);
		}
		IPAddress new_ip = new IPAddress(new_address,netmask);

		String old_url = label_uri;
		String url= old_url+"/"+String.valueOf(i);
		Individual new_ip_rs=om.createIndividual(url, NdlCommons.IPAddressOntClass);
		OntResource new_ip_ont = om.getOntResource(new_ip_rs);
		if(new_address!=null){
			new_ip_ont.addProperty(NdlCommons.layerLabelIdProperty, new_ip.getCIDR());
			new_ip_ont.addProperty(NdlCommons.ip4NetmaskProperty,netmask);
		}
		new_ip.setResource(new_ip_ont);
		return new_ip;
	}
	
	public IPAddress getNewIpAddress(OntModel om, int i) throws UnknownHostException, InetNetworkException{
		if(i==0)
			return this;
		// NOTE: I'm assuming that I can retrieve the label resource based on its URI from 'om' /ib 
		IPAddress new_ip = getNewIpAddress(om, address, netmask, label_uri, i);
		new_ip.base_IP = this;
		return new_ip;
	}
	
	private static boolean checkOctet(String i) {
		try {
			int ii = Integer.parseInt(i);
			if ((ii < 0) || (ii > 255)) 
				return false;
			return true;
		} catch(NumberFormatException nfe) {
			return false;
		}
	}
	
	private static boolean checkNetmask(String i) {
		try {
			int ii = Integer.parseInt(i);
			if ((ii < 0) || (ii > 32)) 
				return false;
			return true;
		} catch(NumberFormatException nfe) {
			return false;
		}
	}
	
	/**
	 * Validate IP dotted notation W.X.Y.Z
	 * @param ip
	 * @return
	 */
	public static boolean validateIP(String ip) {
		Matcher m = ipPattern.matcher(ip);
		if (m.find()) {
			//System.out.println(m.group(1) + "-" + m.group(2) + "-" + m.group(3) + "-" + m.group(4));
			return checkOctet(m.group(1)) && checkOctet(m.group(2)) && 
					checkOctet(m.group(3)) && checkOctet(m.group(4));
		} 
		return false;
	}
	
	/**
	 * Validate CIDR notation W.X.Y.Z/M
	 * @param ip
	 * @return
	 */
	public static boolean validateCIDR(String ip) {
		Matcher m = cidrPattern.matcher(ip);
		if (m.find()) {
			return checkOctet(m.group(1)) && checkOctet(m.group(2)) && 
					checkOctet(m.group(3)) && checkOctet(m.group(4)) && 
					checkNetmask(m.group(5));
		}
		return false;
	}
}
