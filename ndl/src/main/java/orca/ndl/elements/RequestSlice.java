package orca.ndl.elements;

public class RequestSlice {
    // is it openflow (and what version [null means non-of])
    private String ofNeededVersion = null;
    private String ofUserEmail = null;
    private String ofSlicePass = null;
    private String ofCtrlUrl = null;

    // public static RequestSlice getInstance() {
    // if (instance == null) {
    // instance = new RequestSlice();
    // }
    // return instance;
    // }

    public String getOfNeededVersion() {
        return ofNeededVersion;
    }

    public void setOfNeededVersion(String ofNeededVersion) {
        this.ofNeededVersion = ofNeededVersion;
    }

    public String getOfUserEmail() {
        return ofUserEmail;
    }

    public void setOfUserEmail(String ofUserEmail) {
        this.ofUserEmail = ofUserEmail;
    }

    public String getOfSlicePass() {
        return ofSlicePass;
    }

    public void setOfSlicePass(String ofSlicePass) {
        this.ofSlicePass = ofSlicePass;
    }

    public String getOfCtrlUrl() {
        return ofCtrlUrl;
    }

    public void setOfCtrlUrl(String ofCtrlUrl) {
        this.ofCtrlUrl = ofCtrlUrl;
    }

    public void setNoOF() {
        ofNeededVersion = null;
    }

    public void setOFVersion(String v) {
        if ("1.0".equals(v) || "1.1".equals(v) || "1.2".equals(v))
            ofNeededVersion = v;
    }

}
