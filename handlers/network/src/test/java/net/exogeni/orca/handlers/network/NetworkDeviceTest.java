package net.exogeni.orca.handlers.network;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import junit.framework.TestCase;
import net.exogeni.orca.handlers.network.core.INetworkDevice;
import net.exogeni.orca.util.PropList;

import org.apache.log4j.PropertyConfigurator;

public abstract class NetworkDeviceTest extends TestCase {
    public static final String PropertyEmulation = "emulation";

    protected Properties props;

    public NetworkDeviceTest() {
        try {
            PropertyConfigurator.configure("config/log4j.properties");
            props = new Properties();
            props.load(new FileInputStream("ant/tests.properties"));
            File f = new File("ant/user.tests.properties");
            if (f.exists()) {
                Properties user = new Properties();
                user.load(f.toURI().toURL().openStream());
                PropList.mergeProperties(user, props);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void configureDevice(INetworkDevice device) {
        String temp = props.getProperty(PropertyEmulation);
        if (temp != null) {
            temp = temp.trim();
            if (temp.equals("true")) {
                device.enableEmulation();
            }
        }
    }
}
