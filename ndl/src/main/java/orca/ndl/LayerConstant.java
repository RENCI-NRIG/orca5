package orca.ndl;


public interface LayerConstant {
	public enum ConnectionType {crossconnect, link, subnetwork, network};
	
	public enum Direction {UNIDirectional, BIDirectional};
	
	public enum Prefix {dtn, ethernet, ip4};
	
	public enum LabelP {strand,ocg,odu,wavelength,vlan,localIPAddress};
	
	public enum Action {Temporary, VLANtag, CRS, Delete};
	
	public enum AvailableLabelSet {availableOCGSet, availableLambdaSet, available25GSet, availableODUSet,availableVLANSet, availableIPAddressSet};
	public enum UsedLabelSet {usedOCGSet, usedLambdaSet, used25GSet, usedODUSet,usedVLANSet, usedIPAddressSet};
	
	public enum Layer {
		IPNetworkElement (4,Prefix.ip4,Action.CRS,AvailableLabelSet.availableIPAddressSet,UsedLabelSet.usedIPAddressSet,LabelP.localIPAddress),
		EthernetNetworkElement (3,Prefix.ethernet,Action.VLANtag,AvailableLabelSet.availableVLANSet,UsedLabelSet.usedVLANSet,LabelP.vlan), 
		ODUNetworkElement (1,Prefix.dtn,Action.CRS,AvailableLabelSet.availableODUSet,UsedLabelSet.usedODUSet,LabelP.odu),
		LambdaNetworkElement (2,Prefix.dtn,Action.CRS,AvailableLabelSet.available25GSet,UsedLabelSet.used25GSet,LabelP.wavelength),
		OCGNetworkElement (1,Prefix.dtn,Action.CRS,AvailableLabelSet.availableLambdaSet,UsedLabelSet.usedLambdaSet,LabelP.ocg),
		FiberNetworkElement (0,Prefix.dtn,Action.CRS,AvailableLabelSet.availableOCGSet,UsedLabelSet.usedOCGSet,LabelP.strand);
		
		private final int rank;
		private final Prefix prefix;
		private final Action action;
		private final AvailableLabelSet aSet;
		private final UsedLabelSet uSet;
		private final LabelP label;
		Layer(int rank, Prefix prefix,Action action, AvailableLabelSet aSet, UsedLabelSet uSet, LabelP label){
			this.rank=rank;
			this.prefix=prefix;
			this.action=action;
			this.aSet=aSet;
			this.uSet=uSet;
			this.label=label;
		}
		public int rank(){return rank;};
		public Prefix getPrefix(){return prefix;};
		public Action getAction(){return action;};
		public AvailableLabelSet getASet(){return aSet;};
		public UsedLabelSet getUSet(){return uSet;};
		public LabelP getLabelP(){return label;};
	};
	
	public enum AdaptationProperty{
		OCG("OCG"),
		WDM("WDM"),
		TenGbaseR("TenGbase-R"),
		TaggedEthernet("Tagged-Ethernet");
	
		private final String string;
		
		AdaptationProperty(String string){
			this.string=string;
		}
	
		public String toString(){
			return string;
		}
	};
	
	public String [] color={"RED", "BLUE", "YElLOW", "GREEN", "ORANGE"};
	public enum ElementObjectProperty {hasInterface, interfaceOf, coonectedTo, linkTo};
	
}
