package net.exogeni.orca.ndl.elements;

import net.exogeni.orca.util.persistence.PersistenceUtils;
import net.exogeni.orca.util.persistence.Persistent;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.ontology.OntModel;

public class SwitchingAction extends SwitchMatrix {

    @Persistent
    protected String defaultAction;
    @Persistent
    protected String startTime, endTime;
    @Persistent
    private float label_ID;
    @Persistent
    private Label label;
    @Persistent
    private long bw;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(super.toString());

        sb.append("Action: " + defaultAction + " from " + startTime + " to " + endTime + "\n");
        sb.append("Label: " + label + "/" + label_ID + "/" + bw + "\n");

        return sb.toString();
    }

    public long getBw() {
        return bw;
    }

    public void setBw(long bw) {
        this.bw = bw;
    }

    public Label getLabel() {
        return label;
    }

    public void setLabel(Label label) {
        this.label = label;
        if (label != null)
            label_ID = label.label;
    }

    public SwitchingAction() {
        super();
        defaultAction = Action.Temporary.toString();
    }

    public SwitchingAction(OntModel m) {
        super();
        model = m;
        defaultAction = Action.Temporary.toString();
    }

    public SwitchingAction(String layer, String time1, String time2) {
        super();
        this.atLayer = layer;
        this.startTime = time1;
        this.endTime = time2;
        label_ID = -1;
        defaultAction = Action.Temporary.toString();
        bw = 0;
    }

    public void print(Logger logger) {
        super.print(logger);
        logger.info("Time:" + startTime + "-" + endTime);
        logger.info("Action:" + defaultAction);
        logger.info("Label_ID:" + label_ID);
        logger.info("Bandwidth:" + bw);
    }

    public String getDefaultAction() {
        return defaultAction;
    }

    public void setDefaultAction(String defaultAction) {
        this.defaultAction = defaultAction;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public float getLabel_ID() {
        return label_ID;
    }

    public void setLabel_ID(float label_ID) {
        this.label_ID = label_ID;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }
}
