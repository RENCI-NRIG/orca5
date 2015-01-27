package orca.ndl.elements;

import java.util.BitSet;
import java.util.HashSet;

import orca.util.persistence.Persistable;
import orca.util.persistence.Persistent;

import com.hp.hpl.jena.rdf.model.Resource;

public class LabelSet extends HashSet<Label> implements Persistable {
	
	@Persistent
	private Label minLabel;

	@Persistent
	private Label maxLabel;
	
	@Persistent
	public String LabelType;
	
	@Persistent
	public Float setID;
	
	@Persistent
	public String resourceUri;
	
	@Persistent
	public Integer setSize;
	
	@Persistent 
	private BitSet usedBitSet;
	
	@Persistent
	private boolean isAllocatable = true;
		
	public LabelSet(){
		super();
		minLabel=null;
		maxLabel=null;
		usedBitSet=new BitSet(4001);
	}
	
	public LabelSet(Label min, Label max, Resource rs){
		super();
		minLabel=min;
		maxLabel=max;
		if(this.getMaxLabe_ID()==0)
			setSize=1;
		else
			setSize=(int) (this.getMaxLabe_ID()-this.getMinLabel_ID()+1);
		LabelType=min.type;
		resourceUri=rs.getURI();
		usedBitSet=new BitSet(4001);
	}
	
	@Override
	public String toString() {
		return "" + resourceUri + " size " + setSize + " type " + LabelType + " [" + (minLabel != null ? minLabel : "") + " " + (maxLabel != null ? maxLabel : "") + "]";
	}
	
	public boolean contains(Label label){
		if((label.label>=minLabel.label)&(label.label<=maxLabel.label)) return true;
		else return false;
	}
	
	public float getMinLabel_ID(){
		if(minLabel==null)
			return 0;
		return minLabel.label;
	}
	
	public float getMaxLabe_ID(){
		if(maxLabel==null)
			return 0;
		return maxLabel.label;
	}
	
	public int getLabelRangeSize(){
		if( (maxLabel==null) || (minLabel==null)) return 1;
		return (int) (maxLabel.label - minLabel.label+1);
	}

	public BitSet getUsedBitSet() {
		return usedBitSet;
	}

	public void setUsedBitSet(BitSet usedBitSet) {
		this.usedBitSet = usedBitSet;
	}
	
	public void setUsedBitSet(int usedBit) {
		this.usedBitSet.set(usedBit);
	}

	public String getLabelType() {
		return LabelType;
	}

	public void setLabelType(String labelType) {
		LabelType = labelType;
	}

	public Label getMaxLabel() {
		return maxLabel;
	}

	public void setMaxLabel(Label maxLabel) {
		this.maxLabel = maxLabel;
	}

	public Label getMinLabel() {
		return minLabel;
	}

	public void setMinLabel(Label minLabel) {
		this.minLabel = minLabel;
	}

	public float getSetID() {
		return setID;
	}

	public void setSetID(float setID) {
		this.setID = setID;
	}

	public int getSetSize() {
		return setSize;
	}

	public void setSetSize(int setSize) {
		this.setSize = setSize;
	}
	
	public void setNotAllocatable() {
		isAllocatable = false;
	}
	
	public boolean getIsAllocatable() {
		return isAllocatable;
	}
}
