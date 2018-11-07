package orca.ndl;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;

public interface INdlModifyModelListener extends INdlSimpleModelListener {

    /**
     * Identify the resource modify element
     * 
     * @param i i
     * @param name name 
     * @param m m
     */
    public void ndlModifyReservation(Resource i, Literal name, OntModel m);

    public static enum ModifyType {
        ADD, MODIFY, REMOVE, MODIFYREMOVE, INCREASE
    }

    /**
     * Identify modify elements and there types and subjects
     * 
     * @param i i
     * @param subject subject
     * @param t t
     * @param object object
     * @param modifyUnit modifyUnit
     * @param m m
     */
    public void ndlModifyElement(Resource i, Resource subject, ModifyType t, Resource object, int modifyUnit,
            OntModel m);
}
