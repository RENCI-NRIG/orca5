package orca.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.LinkedList;

import orca.embed.workflow.Domain;
import orca.ndl.*;
import orca.ndl.elements.Device;
import orca.ndl.elements.NetworkConnection;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.ontology.OntModel;

public class MappingHandler {

    protected RequestMapping mapper;

    protected OntModel idm; // original physical model

    protected Domain domain;

    protected DomainResources domainResources;

    protected OntModel requestModel;

    protected Hashtable<String, NetworkConnection> requestMap;

    protected Hashtable<String, LinkedList<Device>> domainConnectionList;

    protected Logger logger;

    public MappingHandler() {

    }

    public MappingHandler(String substrateFile) throws IOException {
        domain = new Domain(substrateFile);

        init();
    }

    public MappingHandler(OntModel model) throws IOException {

        domain = new Domain(model);

        init();
    }

    public void init() throws IOException {
        String model_str = domain.getModel().toString();

        requestMap = new Hashtable<String, NetworkConnection>();

        OutputStream out = new ByteArrayOutputStream();
        domain.getModel().write(out);
        domainResources = domain.getDomainResources(out.toString());

        idm = domain.getModel();

        ByteArrayInputStream stream = new ByteArrayInputStream(out.toString().getBytes());

        mapper = new RequestMapping(stream);

        logger = domain.logger;
        mapper.setLogger(logger);
    }

    public Hashtable<String, LinkedList<Device>> getDomainConnectionList() {
        return domainConnectionList;
    }

    public void setDomainConnectionList(Hashtable<String, LinkedList<Device>> connectionList) {
        this.domainConnectionList = connectionList;
    }

    public OntModel getRequestModel() {
        return requestModel;
    }

    public void setRequestModel(OntModel requestModel) {
        this.requestModel = requestModel;
    }

    public Hashtable<String, NetworkConnection> getRequestMap() {
        return requestMap;
    }

    public void setRequestMap(Hashtable<String, NetworkConnection> requestMap) {
        this.requestMap = requestMap;
    }

    public RequestMapping getMapper() {
        return mapper;
    }

    public void setMapper(RequestMapping mapper) {
        this.mapper = mapper;
    }

    public OntModel getIdm() {
        return idm;
    }

    public void setIdm(OntModel idm) {
        this.idm = idm;
    }

    public String getCurrentRequestURI() {
        return mapper.getRequestURI();
    }

    public NetworkConnection getConnection(String uri) {
        return requestMap.get(uri);
    }

    public NetworkConnection getLastConnection() {
        return requestMap.get(getCurrentRequestURI());
    }

}
