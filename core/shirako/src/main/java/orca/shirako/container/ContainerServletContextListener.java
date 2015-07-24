package orca.shirako.container;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;


public class ContainerServletContextListener implements ServletContextListener {
    public ContainerServletContextListener() {
        System.out.println("Orca servlet context listener created");
    }

    public void contextInitialized(ServletContextEvent event) {
        System.out.println("Orca servlet context listener started initialization");
        try {
            Globals.Log.info("Orca web application starting");
            ServletContext sc = event.getServletContext();
            try {
                Globals.ServletContext = sc;
                Globals.start();
            } catch (Exception e) {
                Globals.Log.error("An error occurred while initializing the Orca web application", e);
                System.err.println(e.toString());
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            System.out.println("Failed to initialize context: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void contextDestroyed(ServletContextEvent event) {
        System.out.println("Orca context about to be destroyed");
        Globals.stop();
    }
}
