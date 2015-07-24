import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import orca.boot.beans.Actor;
import orca.boot.beans.Configuration;
import orca.shirako.api.IActor;
import orca.shirako.api.IAuthority;
import orca.shirako.api.IBroker;
import orca.shirako.api.IProxy;
import orca.shirako.registry.ActorRegistry;

import org.apache.axis.EngineConfiguration;
import org.apache.axis.client.AdminClient;
import org.apache.axis.configuration.FileProvider;
import org.apache.axis.utils.Admin;

// XXX: revise!!! the path is wrong
public static final String PropetyClientConfig = "orca/soap/wsdd/deployFilterClient.wsdd";

    /*
     * =======================================================================
     * SOAP Handlers
     * =======================================================================
     */

    /**
     * Produces the wsdd file for the specified actor
     * @param actorName The name of the service
     * @param inputFile Path to the template wsdd
     * @param outputFile Path to the output wsdd
     */
    private void wsddHelper(String actorName, URL inputFile, String outputFile) throws Exception
    {
        logger.info("Creating a custom wsdd file for " + actorName + " from " + inputFile.toString());
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer trans = factory.newTransformer(new StreamSource(this.getClass().getClassLoader().getResource("orca/soap/wsdd/transform.xsl").openStream()));

        trans.setParameter("serviceName", actorName);
        trans.transform(new StreamSource(inputFile.openStream()), new StreamResult(outputFile));
    }

    /**
     * Creates a custom deployment descriptor
     * @param wrapper The actor
     * @param folder Where to store the desriptor
     * @throws Exception
     */
    public String createDeploymentWSDD(String name, File folder) throws Exception
    {
        IActor actor = ActorRegistry.getActor(name);

        if (actor == null) {
            throw new Exception("Internal error: could not find actor");
        }

        URL url;

        if (actor instanceof IBroker) {
            url = this.getClass().getClassLoader().getResource("orca/soap/wsdd/deployAgent.wsdd");
        } else {
            if (actor instanceof IAuthority) {
                url = this.getClass().getClassLoader().getResource("orca/soap/wsdd/deployAuthority.wsdd");
            } else {
                url = this.getClass().getClassLoader().getResource("orca/soap/wsdd/deployServiceManager.wsdd");
            }
        }
        String file = folder.getAbsolutePath() + "/" + actor.getName();
        wsddHelper(actor.getName(), url, file);
        return file;
    }

    /**
     * Creates a custom undeployment descriptor
     * @param wrapper The actor
     * @param folder Where to store the desriptor
     * @throws Exception
     */
    public String createUndeploymentWSDD(String name, File folder) throws Exception
    {
        IActor actor = ActorRegistry.getActor(name);

        if (actor == null) {
            throw new Exception("Internal error: could not find actor");
        }

        URL url = this.getClass().getClassLoader().getResource("orca/soap/wsdd/undeployAgent.wsdd");
        String file = folder.getAbsolutePath() + "/" + actor.getName();
        wsddHelper(actor.getName(), url, file);
        return file;
    }

    /**
     * Prepares the temporary folder
     */
    private File prepareFolder()
    {
        File folder;
        folder = new File(pathPrefix + "/wsdd");

        if (folder.exists()) {
            folder.delete();
        }

        folder.mkdir();

        logger.info("Created wsdd folder: " + folder.getAbsolutePath());
        return folder;
    }

    /**
     * Prepares all deployment descriptors
     * @param config
     * @return
     * @throws Exception
     */
    private void prepareDeploymentWSDD(Configuration config, File folder) throws Exception
    {
        if (config.getActors() != null) {
            Iterator iter = config.getActors().getActor().iterator();

            while (iter.hasNext()) {
                createDeploymentWSDD(((Actor) iter.next()).getName(), folder);
            }
        }
    }

    private void prepareDeploymentWSDD(File folder) throws Exception
    {
        IActor[] actors = ActorRegistry.getActors();
        if (actors != null) {
            for (int i = 0; i < actors.length; i++) {
                createDeploymentWSDD(actors[i].getName(), folder);
            }
        }
    }

    /**
     * Prepares all undeployment descriptos
     * @param folder
     * @throws Exception
     */
    private void prepareUndeploymentWSDD(File folder) throws Exception
    {
        // when undeploying, we can use any of the undeployment files as a
        // template
        URL url = this.getClass().getClassLoader().getResource("orca/soap/wsdd/undeployAgent.wsdd");

        IActor[] actors = ActorRegistry.getActors();

        for (int i = 0; i < actors.length; i++) {
            IActor actor = actors[i];
            wsddHelper(actor.getName(), url, folder.getAbsolutePath() + "/" + actor.getName());
        }
    }

    /**
     * Deploys a service using the admin client
     * @param file Path to the deployment descriptor
     * @throws Exception
     */
    public void deployService(String file) throws Exception
    {
        logger.info("About to deploy service using " + file);
        String location = (String) locations.get(IProxy.ProxyTypeSoap);
        String[] args = new String[4];
        args[0] = new String("-l " + location + "/servlet/AxisServlet");
        // XXX: These two parameters might need to be part of the configuration
        args[1] = new String("-u aydan");
        args[2] = new String("-w aydan");
        args[3] = file;
        AdminClient admin = new AdminClient();
        AdminClient.setDefaultConfiguration(getEngineConfiguration());
        admin.process(args);
        logger.info("Service deployed successfully");
    }

    public EngineConfiguration getEngineConfiguration() throws Exception
    {
        URL url = this.getClass().getClassLoader().getResource(PropetyClientConfig);
        if (url == null) {
            throw new Exception("Cannof load: " + PropetyClientConfig);
        }
        FileProvider fp = new FileProvider(url.openStream());
        return fp;
    }

    /**
     * Deploys all services
     * @param f
     * @throws Exception
     */
    private void deployServices(File f) throws Exception
    {
        File[] files = f.listFiles();

        for (int i = 0; i < files.length; i++) {
            deployService(files[i].getAbsolutePath());
            files[i].delete();
        }

    }

    /**
     * Prepares the deployment descriptor for the security filter
     * @return
     * @throws Exception
     */
    private String extractFilterConfiguration() throws Exception
    {
        File f = prepareFolder();
        URL url = this.getClass().getClassLoader().getResource("orca/soap/wsdd/deployFilterClient.wsdd");
        String result = f.getAbsolutePath() + "/" + "deployFilterClient.wsdd";
        InputStream fis = url.openStream();
        FileOutputStream fos = new FileOutputStream(result);
        byte[] buf = new byte[1024];
        int i = 0;
        while ((i = fis.read(buf)) != -1) {
            fos.write(buf, 0, i);
        }
        fis.close();
        fos.close();
        return result;
    }

    /**
     * Configures the client-side of axis
     * @throws Exception
     */
    private void deployClientConfig() throws Exception
    {
        System.out.println("Deploying client filter");
        // URL url =
        // this.getClass().getClassLoader().getResource("soap/wsdd/deployFilterClient.wsdd");

        String[] args = new String[2];
        args[0] = "client";
        // args[1] = url.getPath();
        args[1] = extractFilterConfiguration();
        Admin.main(args);
    }
    
    
