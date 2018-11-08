package orca.handlers.network.core;

public interface IDevice {
    /**
     * Establishes a connection with the device.
     * 
     * @throws CommandException in case of error
     */
    public void connect() throws CommandException;

    /**
     * Disconnects from the device.
     */
    public void disconnect();

    /**
     * Returns true if a connection to the device exists.
     * 
     * @return true or false
     */
    public boolean isConnected();

    /**
     * Sets the device name.
     * 
     * @param name name
     */
    public void setName(String name);

    /**
     * Gets the device name
     * 
     * @return name
     */
    public String getName();

    public void enableEmulation();

    public void disableEmulation();

    public boolean isEmulationEnabled();
}
