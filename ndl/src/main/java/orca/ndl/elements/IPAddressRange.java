package orca.ndl.elements;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.BitSet;

import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Resource;

import net.jwhoisserver.utils.InetIP2UBI;
import net.jwhoisserver.utils.InetNetwork;
import net.jwhoisserver.utils.InetNetworkException;

public class IPAddressRange {
	public
		IPAddress base_ip_addr;
		InetNetwork base_IP; 
		BigInteger bi_base_IP;
		BitSet bSet;
		String hostInterface;
		
	public IPAddressRange(BitSet set){
		bSet = set;
	}
		
	public IPAddressRange(String addr,String netmask, OntResource addr_rs) throws InetNetworkException, UnknownHostException{
		if(addr==null)
			return;
		if(netmask==null)
			netmask = "255.255.255.0";
		base_ip_addr = new IPAddress(addr,netmask);
		base_ip_addr.setResource(addr_rs);
		base_IP= new InetNetwork(addr,netmask);
		InetAddress base_IP_InetAddr = InetAddress.getByName(addr);
		bi_base_IP=InetIP2UBI.convertIP2UBI(base_IP_InetAddr.getAddress());
		BigInteger bi_max_IP=InetIP2UBI.convertIP2UBI(base_IP.getMaxHostAddressBytes());
		long range = bi_max_IP.longValue() - bi_base_IP.longValue();
		bSet = new BitSet( (int) range);
		bSet.set(0);
	}

	public String toString() {
		return base_ip_addr + " " + base_IP + " " + bi_base_IP + " " + hostInterface;
	}
	
	public void modify(String addr,String netmask, OntResource addr_rs) throws UnknownHostException, InetNetworkException{
		if(addr==null)
			return;
		if(bSet==null)
			return;
		if(base_ip_addr==null){
			base_ip_addr = new IPAddress(addr,netmask);
			base_ip_addr.setResource(addr_rs);
			base_IP= new InetNetwork(addr,netmask);
			InetAddress base_IP_InetAddr = InetAddress.getByName(addr);
			bi_base_IP=InetIP2UBI.convertIP2UBI(base_IP_InetAddr.getAddress());
		}
		InetAddress ip_InetAddr = InetAddress.getByName(addr);
		BigInteger bi_ip_str_IP=InetIP2UBI.convertIP2UBI(ip_InetAddr.getAddress());
		int position = (int) (bi_ip_str_IP.longValue() - bi_base_IP.longValue());
		if(position>=0)
			bSet.set(position);
	}
	
	public IPAddress getBase_ip_addr() {
		return base_ip_addr;
	}

	public void setBase_ip_addr(IPAddress base_ip_addr) {
		this.base_ip_addr = base_ip_addr;
	}

	public InetNetwork getBase_IP() {
		return base_IP;
	}

	public void setBase_IP(InetNetwork base_IP) {
		this.base_IP = base_IP;
	}

	public BigInteger getBi_base_IP() {
		return bi_base_IP;
	}

	public void setBi_base_IP(BigInteger bi_base_IP) {
		this.bi_base_IP = bi_base_IP;
	}

	public BitSet getbSet() {
		return bSet;
	}

	public void setbSet(BitSet bSet) {
		//System.out.println("0.CloudHandler: bSet="+bSet+";this.bSet="+this.bSet);
		if(bSet==null)
			return;
		else
			this.bSet.or(bSet);
	}

	public String getHostInterface() {
		return hostInterface;
	}

	public void setHostInterface(String hostInterface) {
		this.hostInterface = hostInterface;
	};
	
}
