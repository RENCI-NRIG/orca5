package orca.boot;


import java.io.File;

import orca.boot.beans.Configuration;
import orca.shirako.api.IProxy;


    /*
     * =======================================================================
     * Protocol Handlers
     * =======================================================================
     */

    /**
     * Deploys the services to the container
     * @param config The configuration file
     * @throws Exception
     */
    private void deploy(Configuration config) throws Exception
    {
        if (locations.get(IProxy.ProxyTypeSoap) != null) {
            File folder = prepareFolder();

            prepareDeploymentWSDD(config, folder);
            deployServices(folder);
            folder.delete();
            logger.info("All serviced deployed successfully");
        }
    }

    private void deploy() throws Exception
    {
        if (locations.get(IProxy.ProxyTypeSoap) != null) {
            File folder = prepareFolder();

            prepareDeploymentWSDD(folder);
            deployServices(folder);
            folder.delete();
            logger.info("All serviced deployed successfully");
        }
    }

    /**
     * Undeploys all deployed services
     * @throws Exception
     */
    private void undeploy() throws Exception
    {
        if (locations.get(IProxy.ProxyTypeSoap) != null) {
            File folder = prepareFolder();
            prepareUndeploymentWSDD(folder);
            deployServices(folder);
            folder.delete();
        }
    }

