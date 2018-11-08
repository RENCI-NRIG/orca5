package orca.handlers.network.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.apache.commons.net.telnet.TelnetClient;

public abstract class TelnetConsoleDevice extends ConsoleDevice {
    private TelnetClient telnet = null;
    private BufferedReader response = null;
    private OutputStreamWriter send = null;
    protected int telnetPort = 21;
    private boolean emulationConnected = false;

    public TelnetConsoleDevice(String deviceAddress, String uid, String password) {
        super(deviceAddress, uid, password);
    }

    public boolean isConnected() {
        if (isEmulationEnabled()) {
            return emulationConnected;
        }
        return (telnet != null && telnet.isConnected());
    }

    public void connect() throws CommandException {
        logger.debug("Connecting to: " + deviceAddress);
        if (!isEmulationEnabled()) {
            telnet = new TelnetClient();

            try {
                telnet.setDefaultTimeout(5000);
                telnet.connect(deviceAddress, telnetPort);
            } catch (IOException e) {
                throw new CommandException("Could not connect to " + deviceAddress, e);
            }

            if (!telnet.isConnected()) {
                throw new CommandException("Could not connect to " + deviceAddress);
            }

            response = new BufferedReader(new java.io.InputStreamReader(telnet.getInputStream()));
            send = new OutputStreamWriter(telnet.getOutputStream());
        }
        logger.debug("Connected to: " + deviceAddress);
    }

    public void disconnect() {
        try {
            if (telnet != null) {
                telnet.disconnect();
            }
        } catch (Exception e) {
            logger.error("An error occurred during disconnect: " + e);
        } finally {
            response = null;
            send = null;
            telnet = null;
        }
    }

    public void executeCommand(String cmd, String response, String timeout) throws CommandException {
        logger.debug("Executing command: " + cmd + " expected response: " + response);

        logger.debug("Sending command");
        if (!isEmulationEnabled()) {
            if ((null == response) || (null == send)) {
                throw new CommandException("Connection not configured correctly.");
            }
            try {
                send.write(cmd);
                send.flush();
            } catch (IOException e) {
                logger.error("Error sending command: " + cmd + ".", e);
                throw new CommandException("failed: " + cmd, e);
            }
        }
        logger.debug("Command sent");

        if (response.equals("eof") || isEmulationEnabled()) {
            return;
        }

        if (!isEmulationEnabled()) {
            boolean timedout = false;
            // FIXME: how does this work?
            while (!timedout) {
                try {
                    String str = this.response.readLine();
                    logger.debug("RESPONSE: " + str);
                } catch (IOException e) {
                    timedout = true;
                }
            }
        }
        // FIXME: we do not check the response!!!

        // try {
        // Thread.currentThread().sleep(Long.parseLong(timeout));
        // }catch(IOException e){
        // logger.debug(e.getMessage());
        // logger.debug("Error reading response from device.");
        // throw new CommandException(e);
        // }catch(InterruptedException e) {
        // logger.debug(e.getMessage());
        // throw new CommandException(e);
        // }
    }

}
