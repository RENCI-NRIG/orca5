package orca.embed.policyhelpers;

import com.hp.hpl.jena.rdf.model.Resource;

import orca.ndl.INdlModifyModelListener;
import orca.ndl.INdlModifyModelListener.ModifyType;

public class ModifyElement {
    Resource sub, obj;
    INdlModifyModelListener.ModifyType modType = null;
    int modifyUnits;

    public ModifyElement(Resource s, ModifyType t, Resource o, int number) {
        sub = s;
        modType = t;
        obj = o;

        modifyUnits = number;
    }

    public Resource getSub() {
        return sub;
    }

    public void setSub(Resource sub) {
        this.sub = sub;
    }

    public Resource getObj() {
        return obj;
    }

    public void setObj(Resource obj) {
        this.obj = obj;
    }

    public INdlModifyModelListener.ModifyType getModType() {
        return modType;
    }

    public void setModType(INdlModifyModelListener.ModifyType modType) {
        this.modType = modType;
    }

    public int getModifyUnits() {
        return modifyUnits;
    }

    public void setModifyUnits(int modifyUnits) {
        this.modifyUnits = modifyUnits;
    }

}