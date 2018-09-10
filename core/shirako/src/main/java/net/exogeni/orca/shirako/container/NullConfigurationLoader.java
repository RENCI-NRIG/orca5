package net.exogeni.orca.shirako.container;

import java.io.InputStream;

import net.exogeni.orca.shirako.common.ConfigurationException;
import net.exogeni.orca.shirako.container.api.IConfigurationLoader;

/**
 * Null implementation of IConfigurationLoader.
 * @author aydan
 *
 */
public class NullConfigurationLoader implements IConfigurationLoader
{
    public void process() throws ConfigurationException {
    }

    public void setConfiguration(String path) {
    }

    public void setConfiguration(byte[] bytes) {
    }

    public void setConfiguration(InputStream is) {
    }

    public void setAdd(boolean add) {
    }
}
