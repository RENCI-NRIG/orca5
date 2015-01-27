This folder will contain tools to bootstrap a shirako
test-bed. Currently the plan for these tools is:

1. Configuration files

   A particular testbed configuration specifies:
   - the shirako actors (actors.xml)
   - the mapping of actors to containers (containers.xml)
   - the toplogy of actor communication (topology.xml)

   After preprocessing these files we generate configuration per
   container (container1.xml, container2.xml, etc.). A container
   configuration file contains the following information:

   - which actors are located within this container
   - for each actor within the container:
     - instantiation information (pulled out from actors.xml)
     - all actors to which it has a link
   - for each link:
         - the public/private key of the actor
         - the public key of the other actor
         - the url of the serviceEndpoint of the other actor
           (if it happens that both actors are in the same virtual
            machine, the actors will be configured to use local
            communication)

   We also need to generate deployment descriptors for each shirako
   service. We can do this by using a common template and
   parameterizing it for the actor. The configuration should have
   the name of the actor. All other information can be retrieved from
   the actor-registry.
                               
            
2. Preprocessing tools

   Input: actors.xml, containers.xml, topology.xml
   Output: container1.xml, container2.xml, etc.

3. Basic infrastructure:

   Bootstrap service manager: recieves the container configuration
   files and requests the required virtual machines from an
   Agent+Authority

   Agent+Authority service: receives requests for
   resources. Instantiates virtual machines. Sets them up with the ssh
   public key of the service manager.

   Having received access to the virtual machines. The service manager
   uploads a shell script to each virtual machine and runs the script.

4. Container setup script

   - install tomcat: copy it off nfs/web
   - configure tomcat to allow administrative access from the service manager
   - start tomcat

5. Container instantiation

   1. Deploy the shirako WAR
   2. Call the ConfigServlet to upload the configuration
   3. Send the deployment descripts for all services to axis

6. ContainerBootstrapper class

   -- singleton
   -- reads the container configuration
   -- instantiates and registers all necessary objects


    
