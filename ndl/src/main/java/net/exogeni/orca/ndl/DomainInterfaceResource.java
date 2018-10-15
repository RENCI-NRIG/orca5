package net.exogeni.orca.ndl;

import java.util.LinkedList;

import net.exogeni.orca.ndl.elements.LabelSet;

public class DomainInterfaceResource extends DomainResource {
    protected LinkedList<LabelSet> labelSet;

    public DomainInterfaceResource(String iface) {
        super(iface);
        labelSet = new LinkedList<LabelSet>();
    }

    public LinkedList<LabelSet> getLabelSet() {
        return labelSet;
    }

    public void setLabelSet(LinkedList<LabelSet> labelSet) {
        this.labelSet = labelSet;
    }

}
