package orca.handlers.network.core;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.oro.text.perl.MalformedPerl5PatternException;
import org.apache.oro.text.perl.Perl5Util;

import com.trilead.ssh2.ChannelCondition;
import com.trilead.ssh2.Connection;
import com.trilead.ssh2.Session;
import com.trilead.ssh2.StreamGobbler;

public abstract class SSHConsoleDevice extends ConsoleDevice {
    /**
     * The SSH connection.
     */
    private Connection conn;
    private Perl5Util util = new Perl5Util();
    private Session session;
    private InputStream stderr; // stderr from ssh
    private InputStream stdout; // stdout from ssh
    private OutputStream stdin; // stdin to ssh

    private boolean emulationConnected = false;

    public SSHConsoleDevice(String deviceAddress, String uid, String password) {
        super(deviceAddress, uid, password);
    }

    public boolean isConnected() {
        if (isEmulationEnabled()) {
            return emulationConnected;
        }

        return conn != null;
    }

    @Override
    public void executeCommand(String cmd, String response, String timeout) throws CommandException {
        if (cmd == null) {
            throw new CommandException("Empty command string.");
        } else {
            logger.debug("Executing command: " + cmd + " expected response: " + response);

            try {
                logger.debug("Sending command");
                if (!isEmulationEnabled()) {
                    byte[] cmdBytes = cmd.getBytes();
                    stdin.write(cmdBytes);
                    stdin.write('\n');
                    stdin.flush();
                }
                logger.debug("Command sent");

                // check for eof, if so no need for response
                // do not check the response if running under emulation
                if (response.equals("eof") || isEmulationEnabled()) {
                    return;
                }
                
                String output = null;
                String pattern = "/" + response + "/m";

                try {
                    output = new String(getOutput(Integer.parseInt(timeout)));
                    boolean properResponse = util.match(pattern, output);

                    // report if no proper response found
                    if (!properResponse) {
                        logger.error("Expected response not received for command: " + cmd);
                        logger.error("Unexpected response received was: " + output);
                        logger.error("Response expected was: " + pattern);
                        throw new CommandException("Expected response not received.");
                    }
                } catch (EOFException e) {
                    throw new CommandException("Connection lost to device.", e);
                }
            } catch (CommandException e) {
                throw e;
            } catch (IOException e) {
                throw new CommandException(e);
            } catch (MalformedPerl5PatternException e) {
                throw new CommandException(e);
            }
        }
    }

    public void connect() throws CommandException {
        try {
            logger.info("Connecting to: " + deviceAddress);
            if (!isEmulationEnabled()) {
                conn = new Connection(deviceAddress);
                conn.setTCPNoDelay(true);
                conn.connect();
            }
            logger.info("Connected to: " + deviceAddress);
            logger.debug("Sending authentication information...");
            if (!isEmulationEnabled()) {
                if (!conn.authenticateWithPassword(uid, password)) {
                    throw new CommandException("Authentication failed");
                }
            }
            logger.debug("Authentication successful");
            if (!isEmulationEnabled()) {
                session = conn.openSession();
                if (session == null) {
                    throw new CommandException("Unable to create connection session");
                }
                session.requestPTY("vt100");
                session.startShell();

                stdout = session.getStdout();
                if (stdout == null) {
                    throw new CommandException("Could not obtain stdout reader");
                }

                stderr = new StreamGobbler(session.getStderr());
                if (stderr == null) {
                    throw new CommandException("Could not obtain stderr reader");
                }

                stdin = session.getStdin();
                if (stdin == null) {
                    throw new CommandException("Could not obtain stdin writer");
                }
                getOutput(2000); // clear buffer
            } else {
                emulationConnected = true;
            }
            logger.debug("Connection to: " + deviceAddress + " setup successfully");
        } catch (IOException e) {
        	throw new CommandException(e);
        }
    }

    public void disconnect() {
        if (conn != null) {
            conn.close();
            conn = null;
        }
    }

    protected byte[] getOutput(int timeout) throws EOFException, IOException {
        int conditions = ChannelCondition.EOF;
        final int BUFF_SIZE = 8192;
        byte buffer[] = new byte[BUFF_SIZE];
        int offset = 0;

        do {
            conditions = session.waitForCondition(ChannelCondition.STDOUT_DATA | ChannelCondition.EOF, timeout);

            while (stdout.available() > 0) {
                int maxLen = BUFF_SIZE - offset;
                int result = stdout.read(buffer, offset, maxLen);

                // if we read chars then increase offset
                if (result > 0) {
                    offset += result;
                }
            }

        } while ((conditions & (ChannelCondition.TIMEOUT | ChannelCondition.EOF)) == 0); // keep
        // looping
        // till
        // timeout
        // or
        // EOF

        if (0 != (conditions & ChannelCondition.EOF)) {
            logger.error("EOF (response if any): " + (new String(buffer)));
            throw new EOFException("Connection to console lost.");
        }

        return buffer;
    }


    /**
     * Send a command over stdin
     */
    protected void sendCommand(String cmd) throws IOException {
        byte[] cmdBytes = cmd.getBytes();
        stdin.write(cmdBytes);
        stdin.write('\n');
        stdin.flush();
    }

    /**
     * Try reading some output from stdout.
     */
    protected String readOutput(int timeout) throws IOException {
        final int BUFF_SIZE = 8192;
        byte buffer[] = new byte[BUFF_SIZE];
        int offset = 0;
        int conditions = session.waitForCondition(ChannelCondition.STDOUT_DATA | ChannelCondition.EOF, timeout);
        if (0 != (conditions & ChannelCondition.EOF)) {
            throw new EOFException("Connection to console lost.");
        }
        while (stdout.available() > 0 && offset < BUFF_SIZE) {
            int maxLen = BUFF_SIZE - offset;
            int result = stdout.read(buffer, offset, maxLen);
            if (result > 0) {
                offset += result;
            }
        }
        return new String(buffer, 0, offset);
    }
}
