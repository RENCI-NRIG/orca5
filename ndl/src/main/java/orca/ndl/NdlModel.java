package orca.ndl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import orca.ndl.util.ModelFolders;

import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.LocationMapper;
import com.hp.hpl.jena.util.LocatorURL;
import com.hp.hpl.jena.util.TypedStream;

/**
 * Class that encapsulates NDL Model handling
 * 
 * @author ibaldin
 *
 */
public class NdlModel {

    // map from
    public static final String[] orcaSchemaFiles = { "collections.owl", "compute.owl", "domain.owl", "dtn.owl",
            "ec2.owl", "ethernet.owl", "eucalyptus.owl", "exogeni.owl", "geni.owl", "ip4.owl", "itu-grid.owl",
            "kansei.owl", "layer.owl", "location.owl", "manifest.owl", "modify.owl", "openflow.owl", "planetlab.owl",
            "protogeni.owl", "request.owl", "storage.owl", "tcp.owl", "topology.owl", "app-color.owl" };

    public static final String[] orcaSubstrateFiles = { "orca.rdf", "ben.rdf", "ben-dtn.rdf", "ben-6509.rdf" };

    public static final Map<String, String> externalSchemas;

    static {
        Map<String, String> m = new HashMap<String, String>();
        m.put("http://www.w3.org/2006/time", "time.owl");
        externalSchemas = Collections.unmodifiableMap(m);
    }

    // model types we allow
    public enum ModelType {
        InMemory, TdbEphemeral, TdbPersistent
    };

    /**
     * Jena does not handle java jar:file: URLs, so we need this locator
     * 
     * @author ibaldin
     *
     */
    public static class LocatorJarURL extends LocatorURL {
        public LocatorJarURL() {
            super();
        }

        @Override
        public TypedStream open(String filenameOrURI) {
            try {
                URL u = new URL(filenameOrURI);
                if (filenameOrURI.startsWith("jar:")) {
                    JarURLConnection jarConnection = (JarURLConnection) u.openConnection();
                    return new TypedStream(jarConnection.getInputStream());
                }
                return new TypedStream(u.openStream());
            } catch (MalformedURLException e) {
                ;
            } catch (IOException i) {
                ;
            }

            return super.open(filenameOrURI);
        }

        @Override
        public String getName() {
            return ("LocatorJarURL");
        }

    }

    /**
     * Set common redirections for a specific document manager
     * 
     * @param dm dm
     */
    public static void setJenaRedirections(OntDocumentManager dm) {

        // ClassLoader cl = NdlCommons.class.getClassLoader();
        // ClassLoader cl = ClassLoader.getSystemClassLoader();
        ClassLoader cl = NdlCommons.class.getProtectionDomain().getClassLoader();

        FileManager fm = new FileManager();
        // deep copy standard location mapper
        fm.setLocationMapper(new LocationMapper(OntDocumentManager.getInstance().getFileManager().getLocationMapper()));
        fm.addLocator(new NdlModel.LocatorJarURL());
        fm.setModelCaching(false);
        dm.setFileManager(fm);
        dm.setCacheModels(false);

        for (String s : NdlModel.orcaSchemaFiles) {
            dm.addAltEntry(NdlCommons.ORCA_NS + s, cl.getResource(NdlCommons.ORCA_NDL_SCHEMA + s).toString());
        }

        for (String s : NdlModel.orcaSubstrateFiles) {
            dm.addAltEntry(NdlCommons.ORCA_NS + s, cl.getResource(NdlCommons.ORCA_NDL_SUBSTRATE + s).toString());
        }

        // deal with odd ones we didn't create (time etc)
        for (String s : NdlModel.externalSchemas.keySet()) {
            dm.addAltEntry(s, cl.getResource(NdlCommons.ORCA_NDL_SCHEMA + NdlModel.externalSchemas.get(s)).toString());
        }
    }

    /**
     * Close model and remove its disk space
     * 
     * @param m m
     */
    public static void closeModel(OntModel m) {
        if (m == null)
            return;
        if (m.supportsTransactions())
            m.commit();
        m.close();
        // close the dataset and remove disk space
        // if it was allocated
        // doesn't matter ephemeral or persistent
        ModelFolders.getInstance().remove(m);
    }

    public static OntModelSpec getOntModelSpec(OntModelSpec spec, boolean uniqueDM) {
        if (spec == null)
            spec = OntModelSpec.OWL_MEM;

        OntModelSpec s = new OntModelSpec(spec);

        if (uniqueDM) {
            OntDocumentManager odm = new OntDocumentManager();
            setJenaRedirections(odm);
            odm.setProcessImports(true);
            s.setDocumentManager(odm);
        } else {
            setJenaRedirections(s.getDocumentManager());
            s.getDocumentManager().setProcessImports(true);
        }
        return s;
    }

    /**
     * Create a new model. If spec is null, OntModelSpec.OWL_MEM - simple in-memory model will be built. For inference
     * use OntModelSpec.RDFS_MEM_TRANS_INF. Optionally create a model with a new DocumentManager.
     * 
     * @param modelStream modelStream
     * @param spec spec
     * @param uniqueDM
     *            - unique document manager (usually yes)
     * @param t 
     *            - in-memory, TDB ephemeral or persistent
     * @param folderName
     *            - optionally, the name of the folder where to save TDB (temprorary will be created if null)
     * @return OntModel
     * @throws NdlException in case of error
     */
    public static OntModel getModelFromStream(InputStream modelStream, OntModelSpec spec, boolean uniqueDM, ModelType t,
            String folderName) throws NdlException {
        NdlCommons.getNdlLogger().info("NdlModel::getModelFromStream(): Creating " + t + " model in " + folderName);
        assert (modelStream != null);
        if (spec == null)
            spec = OntModelSpec.OWL_MEM;

        OntModelSpec s = new OntModelSpec(spec);

        if (uniqueDM) {
            OntDocumentManager odm = new OntDocumentManager();
            setJenaRedirections(odm);
            odm.setProcessImports(true);
            s.setDocumentManager(odm);
        } else {
            setJenaRedirections(s.getDocumentManager());
            s.getDocumentManager().setProcessImports(true);
        }

        // create file-backed TDB storage if requested
        OntModel model = null;

        try {
            switch (t) {
            case TdbEphemeral:
                if (globalTDB) {
                    File dir = null;
                    dir = ModelFolders.getInstance().createTempDirectory(folderName);
                    if (dir == null)
                        throw new NdlException("Unable to create temporary model folder in " + folderName);
                    Dataset ds = TDBFactory.createDataset(dir.getAbsolutePath());
                    Model fileModel = ds.getDefaultModel();
                    model = ModelFactory.createOntologyModel(s, fileModel);
                    ModelFolders.getInstance().put(model, ds, dir.getAbsolutePath(), true);
                } else
                    model = ModelFactory.createOntologyModel(s);
                break;
            case TdbPersistent:
                if (globalTDB && (folderName != null)) {
                    File dir = null;
                    dir = ModelFolders.getInstance().createNamedDirectory(folderName);
                    if (dir == null)
                        throw new NdlException("Unable to create persistent model folder in " + folderName);
                    Dataset ds = TDBFactory.createDataset(dir.getAbsolutePath());
                    Model fileModel = ds.getDefaultModel();
                    model = ModelFactory.createOntologyModel(s, fileModel);
                    ModelFolders.getInstance().put(model, ds, dir.getAbsolutePath(), false);
                } else
                    model = ModelFactory.createOntologyModel(s);
                break;
            case InMemory:
                model = ModelFactory.createOntologyModel(s);
                break;
            }

            model.read(modelStream, "");
        } catch (Exception e) {
            closeModel(model);
            throw new NdlException("NdlException unable to create model: " + e.getMessage());
        }

        try {
            modelStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return model;
    }

    /**
     * Get an in-memory model from specified stream
     * 
     * @param modelStream modelStream
     * @param spec
     *            - model spec
     * @param uniqueDM
     *            - create a unique document manager (usually yes)
     * @return OntModel
     * @throws NdlException in case of error
     */
    public static OntModel getModelFromStream(InputStream modelStream, OntModelSpec spec, boolean uniqueDM)
            throws NdlException {
        return getModelFromStream(modelStream, spec, uniqueDM, ModelType.InMemory, null);
    }

    /**
     * Get an in-memory model from file. If spec is null, OntModelSpec.OWL_MEM - simple in memory model will be built.
     * For inference use OntModelSpec.RDFS_MEM_TRANS_INF. Optionally create a model with a new DocumentManager.
     * 
     * @param aFile aFile 
     * @param spec spec
     * @param uniqueDM uniqueDM
     * @return OntModel
     * @throws NdlException in case of error
     */
    public static OntModel getModelFromFile(String aFile, OntModelSpec spec, boolean uniqueDM) throws NdlException {
        return getModelFromFile(aFile, spec, uniqueDM, ModelType.InMemory, null);
    }

    /**
     * Get a model from file. If spec is null, OntModelSpec.OWL_MEM - simple in memory model will be built. For
     * inference use OntModelSpec.RDFS_MEM_TRANS_INF. Optionally create a model with a new DocumentManager.
     * 
     * @param aFile aFile
     * @param spec spec 
     * @param uniqueDM uniqueDM
     * @param t 
     *            - in-memory, TDB persistent or ephemeral
     * @param folderName
     *            - for TDB
     * @return OntModel
     * @throws NdlException in case of error
     */
    public static OntModel getModelFromFile(String aFile, OntModelSpec spec, boolean uniqueDM, ModelType t,
            String folderName) throws NdlException {
        NdlCommons.getNdlLogger().info("NdlModel::getModelFromFile(): Creating model from " + aFile);
        InputStream in = FileManager.get().openNoMap(aFile);
        if (in == null) {
            throw new IllegalArgumentException("File: " + aFile + " not found");
        }

        OntModel model = getModelFromStream(in, spec, uniqueDM, t, folderName);
        return model;
    }

    /**
     * Create a request model. If spec is null, OntModelSpec.OWL_MEM - simple in memory model will be built. For
     * inference use OntModelSpec.RDFS_MEM_TRANS_INF. Optionally create a model with a new DocumentManager. If TDB is
     * indicated, folder name can be specified (this will make the model non-ephemeral - persistent across shutdowns)
     * 
     * @param modelStream modelStream
     * @param spec spec
     * @param uniqueDM uniqueDM
     *            - need a unique DocumentManager (usually yes)
     * @param t 
     *            - in-memory, TDB ephemeral or persistent
     * @param folderName
     *            - optional name of the folder for TDB
     * @return OntModel
     * @throws NdlException in case of error
     */
    public static OntModel getRequestModelFromStream(InputStream modelStream, OntModelSpec spec, boolean uniqueDM,
            ModelType t, String folderName) throws NdlException {
        NdlCommons.getNdlLogger().info("NdlModel::getRequestModelFromStream(): creating " + t + " model in " + folderName);
        assert (modelStream != null);
        FileManager fm = new FileManager();
        // deep copy standard location mapper
        fm.setLocationMapper(new LocationMapper(OntDocumentManager.getInstance().getFileManager().getLocationMapper()));
        fm.addLocator(new NdlModel.LocatorJarURL());

        ClassLoader cl = NdlCommons.class.getProtectionDomain().getClassLoader();
        URL reqUrl = cl.getResource(NdlCommons.ORCA_NDL_SCHEMA + "request.owl");

        Model schemaModel = null;
        File dir = null;
        Dataset ds = null;

        switch (t) {
        case TdbEphemeral:
            if (globalTDB) {
                dir = ModelFolders.getInstance().createTempDirectory(folderName);
                if (dir == null)
                    throw new NdlException("Unable to create temporary model folder in " + folderName);
                Model tmpSchemaModel = fm.loadModel(reqUrl.toString());
                ds = TDBFactory.createDataset(dir.getAbsolutePath());
                schemaModel = ds.getDefaultModel();
                schemaModel.add(tmpSchemaModel);
                tmpSchemaModel.close();
            } else
                schemaModel = fm.loadModel(reqUrl.toString());
            break;
        case TdbPersistent:
            if (globalTDB && (folderName != null)) {
                dir = ModelFolders.getInstance().createNamedDirectory(folderName);
                if (dir == null)
                    throw new NdlException("Unable to create persistent model folder in " + folderName);
                Model tmpSchemaModel = fm.loadModel(reqUrl.toString());
                ds = TDBFactory.createDataset(dir.getAbsolutePath());
                schemaModel = ds.getDefaultModel();
                schemaModel.add(tmpSchemaModel);
                tmpSchemaModel.close();
            } else
                schemaModel = fm.loadModel(reqUrl.toString());
            break;
        case InMemory:
            schemaModel = fm.loadModel(reqUrl.toString());
            break;
        }

        try {
            schemaModel.read(modelStream, "");
        } catch (Exception e) {
            if (schemaModel.supportsTransactions())
                schemaModel.commit();
            schemaModel.close();
            if (dir != null)
                ModelFolders.deleteFolder(dir.getAbsolutePath());
            throw new NdlException("NdlException, unable to create request model: " + e.getMessage());
        }

        if (spec == null)
            spec = OntModelSpec.OWL_MEM;

        // modify default spec recipe
        OntModelSpec s = new OntModelSpec(spec);

        if (uniqueDM) {
            OntDocumentManager odm = new OntDocumentManager();
            setJenaRedirections(odm);
            odm.setProcessImports(true);
            s.setDocumentManager(odm);
        } else {
            setJenaRedirections(s.getDocumentManager());
            s.getDocumentManager().setProcessImports(true);
        }

        OntModel requestModel = ModelFactory.createOntologyModel(s, schemaModel);

        try {
            if (dir != null) {
                ModelFolders.getInstance().put(requestModel, ds, dir.getAbsolutePath(),
                        (t == ModelType.TdbPersistent ? false : true));
            }
            modelStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new NdlException("Unable to get request model from stream due to: " + e);
        }
        return requestModel;
    }

    /**
     * Create an in-memory request model. If spec is null, OntModelSpec.OWL_MEM - simple in memory model will be built.
     * For inference use OntModelSpec.RDFS_MEM_TRANS_INF. Optionally create a model with a new DocumentManager.
     * 
     * @param modelStream modelStream
     * @param spec spec
     * @param uniqueDM uniqueDM
     * @return OntModel
     * @throws NdlException in case of error
     */
    public static OntModel getRequestModelFromStream(InputStream modelStream, OntModelSpec spec, boolean uniqueDM)
            throws NdlException {
        return getRequestModelFromStream(modelStream, spec, uniqueDM, ModelType.InMemory, null);
    }

    /**
     * Create a blank model
     * 
     * @param spec spec
     * @param uniqueDM uniqueDM
     *            - with unique document manager
     * @param t 
     *            - in-memory, TDB persistent or ephemeral
     * @param folderName
     *            - optionally (null permitted) specify the name of the folder to store the model
     * @return OntModel
     * @throws NdlException in case of error
     */
    public static OntModel createModel(OntModelSpec spec, boolean uniqueDM, ModelType t, String folderName)
            throws NdlException {
        NdlCommons.getNdlLogger().info("NdlModel::createModel(): creating blank " + t + " model in " + folderName);
        if (spec == null)
            spec = OntModelSpec.OWL_MEM;

        OntModelSpec s = new OntModelSpec(spec);

        if (uniqueDM) {
            OntDocumentManager odm = new OntDocumentManager();
            setJenaRedirections(odm);
            odm.setProcessImports(true);
            s.setDocumentManager(odm);
        } else {
            setJenaRedirections(s.getDocumentManager());
            s.getDocumentManager().setProcessImports(true);
        }

        // create file-backed TDB storage if requested
        OntModel model = null;
        try {
            switch (t) {
            case TdbEphemeral:
                if (globalTDB) {
                    File dir = null;
                    dir = ModelFolders.getInstance().createTempDirectory(folderName);
                    if (dir == null)
                        throw new NdlException("Unable to create temporary model folder in " + folderName);
                    Dataset ds = TDBFactory.createDataset(dir.getAbsolutePath());
                    Model fileModel = ds.getDefaultModel();
                    model = ModelFactory.createOntologyModel(s, fileModel);
                    ModelFolders.getInstance().put(model, ds, dir.getAbsolutePath(), true);
                } else
                    model = ModelFactory.createOntologyModel(s);
                break;
            case TdbPersistent:
                if (globalTDB && (folderName != null)) {
                    File dir = null;
                    dir = ModelFolders.getInstance().createNamedDirectory(folderName);
                    if (dir == null)
                        throw new NdlException("Unable to create persistent model folder in " + folderName);
                    Dataset ds = TDBFactory.createDataset(dir.getAbsolutePath());
                    Model fileModel = ds.getDefaultModel();
                    model = ModelFactory.createOntologyModel(s, fileModel);
                    ModelFolders.getInstance().put(model, ds, dir.getAbsolutePath(), false);
                } else
                    model = ModelFactory.createOntologyModel(s);
                break;
            case InMemory:
                model = ModelFactory.createOntologyModel(s);
                break;
            }
        } catch (Exception e) {
            throw new NdlException("Unable to create blank model due to: " + e);
        }
        return model;
    }

    /**
     * Create a blank in-memory model
     * 
     * @param spec spec
     * @param uniqueDM uniqueDM
     * @return OntModel
     * @throws NdlException in case of error
     */
    public static OntModel createModel(OntModelSpec spec, boolean uniqueDM) throws NdlException {
        return createModel(spec, uniqueDM, ModelType.InMemory, null);
    }

    /**
     * Initialize and return an in-memory model from RDF/XML String. If spec is null, OntModelSpec.OWL_MEM - simple in
     * memory model will be built. For inference use OntModelSpec.RDFS_MEM_TRANS_INF. Optionally create a model with a
     * new DocumentManager.
     * 
     * @param s s
     * @param spec spec 
     * @param uniqueDM uniqueDM
     *            - use a unique DM
     * @return OntModel
     * @throws NdlException in case of error
     */
    public static OntModel getModelFromString(String s, OntModelSpec spec, boolean uniqueDM) throws NdlException {
        assert (s != null);
        ByteArrayInputStream modelStream = new ByteArrayInputStream(s.getBytes());
        OntModel model = getModelFromStream(modelStream, spec, uniqueDM, ModelType.InMemory, null);

        return model;
    }

    /**
     * Initialize and return a model from RDF/XML String. If spec is null, OntModelSpec.OWL_MEM - simple in memory model
     * will be built. For inference use OntModelSpec.RDFS_MEM_TRANS_INF. Optionally create a model with a new
     * DocumentManager.
     * 
     * @param s s
     * @param spec spec
     * @param uniqueDM
     *            - use a unique DM
     * @param t
     *            ModelType (in-memory, tdb ephemeral or persistent)
     * @param folderName
     *            for tdb models
     * @return OntModel
     * @throws NdlException in case of error
     */
    public static OntModel getModelFromString(String s, OntModelSpec spec, boolean uniqueDM, ModelType t,
            String folderName) throws NdlException {
        assert (s != null);
        ByteArrayInputStream modelStream = new ByteArrayInputStream(s.getBytes());
        OntModel model = getModelFromStream(modelStream, spec, uniqueDM, t, folderName);

        return model;
    }

    /**
     * Create a TDB backed model from pre-existing store
     * 
     * @param d
     *            - directory where TDB store is
     * @param s
     *            - OntModelSpec for the model
     * @return OntModel
     * @throws NdlException in case of error
     */
    public static OntModel getModelFromTDB(String d, OntModelSpec s) throws NdlException {
        assert (d != null);
        try {
            Dataset dataset = TDBFactory.createDataset(d);
            Model fileModel = dataset.getDefaultModel();
            OntModel mm = ModelFactory.createOntologyModel(s, fileModel);
            File tf = new File(d);
            ModelFolders.getInstance().put(mm, dataset, tf.getAbsolutePath(), false);
            return mm;
        } catch (Exception e) {
            e.printStackTrace();
            throw new NdlException("Unable to get model from TDB: " + e);
        }
    }

    // set to true to enable using TDB
    static private boolean globalTDB = true;

    /**
     * Set global DocumentManager redirections for Jena not to look for schema files on the internet, but to use files
     * in this package instead.
     */
    private static boolean globalRedirections = false;

    public static void setGlobalJenaRedirections() {

        // idempotent
        if (NdlModel.globalRedirections)
            return;
        NdlModel.globalRedirections = true;

        // ClassLoader cl = NdlCommons.class.getClassLoader();
        // ClassLoader cl = ClassLoader.getSystemClassLoader();
        ClassLoader cl = NdlCommons.class.getProtectionDomain().getClassLoader();

        OntDocumentManager dm = OntDocumentManager.getInstance();
        dm.getFileManager().addLocator(new NdlModel.LocatorJarURL());

        for (String s : NdlModel.orcaSchemaFiles) {
            dm.addAltEntry(NdlCommons.ORCA_NS + s, cl.getResource(NdlCommons.ORCA_NDL_SCHEMA + s).toString());
        }

        for (String s : NdlModel.orcaSubstrateFiles) {
            dm.addAltEntry(NdlCommons.ORCA_NS + s, cl.getResource(NdlCommons.ORCA_NDL_SUBSTRATE + s).toString());
        }

        // deal with odd ones we didn't create (time etc)
        for (String s : NdlModel.externalSchemas.keySet()) {
            dm.addAltEntry(s, cl.getResource(NdlCommons.ORCA_NDL_SCHEMA + NdlModel.externalSchemas.get(s)).toString());
        }
    }

}
