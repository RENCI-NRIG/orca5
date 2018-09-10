package net.exogeni.orca.ndl;

import java.util.LinkedList;

import net.exogeni.orca.ndl.elements.LabelSet;
import net.exogeni.orca.util.persistence.Persistable;
import net.exogeni.orca.util.persistence.Persistent;

public class DomainResource implements Persistable {

    @Persistent
    protected String iface;
    @Persistent
    protected Long bandwidth = 0L;
    @Persistent
    protected LinkedList<LabelSet> label_list;
    @Persistent
    protected Integer numLabel = 0;

    public DomainResource(String iface) {
        this.iface = iface;
    }

    public String getInterface() {
        return iface;
    }

    public void setBandwidth(long bandwidth) {
        this.bandwidth = bandwidth;
    }

    public long getBandwidth() {
        return bandwidth;
    }

    public void reserveBandwidth(long bw) {
        bandwidth -= bw;
    }

    public void releaseBandwidth(long bw) {
        bandwidth += bw;
    }

    public LinkedList<LabelSet> getLabel_list() {
        return label_list;
    }

    public void setLabel_list(LinkedList<LabelSet> label_list) {
        this.label_list = label_list;
        this.countNumLabel();
    }

    public void setLabel(LabelSet label) {
        if (label == null)
            return;
        if (label_list == null) {
            label_list = new LinkedList<LabelSet>();
        }
        label_list.add(label);
        this.countNumLabel();
    }

    public void countNumLabel() {
        if (label_list == null)
            return;
        int size = 0;
        for (LabelSet ls : label_list) {
            size = ls.getLabelRangeSize();
            numLabel = numLabel + size;
        }
    }

    public int getNumLabel() {
        return numLabel;
    }

    public void decreaseNumLabel() {
        if (numLabel > 0)
            numLabel--;
    }

    public void increaseNumLabel() {
        numLabel++;
    }

    @Override
    public String toString() {
        return "[" + iface + ",bw=" + bandwidth + ", tag=" + numLabel + ", label list" + label_list + "]";
    }
}
