package orca.handlers.network.core;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.trilead.ssh2.ChannelCondition;
import com.trilead.ssh2.Connection;
import com.trilead.ssh2.Session;
import com.trilead.ssh2.StreamGobbler;

public abstract class SSHConsoleDevice extends ConsoleDevice {
    /**
     * The SSH connection.
     */
    private Connection conn;
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

                discardUntilPattern(Pattern.compile(response), Integer.parseInt(timeout));
            } catch (EOFException e) {
                throw new CommandException("Connection lost to device.", e);
            } catch (CommandException e) {
                throw e;
            } catch (IOException e) {
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
            } else {
                emulationConnected = true;
            }
            logger.debug("Connection to: " + deviceAddress + " setup successfully");
            if (!isEmulationEnabled()) {
                clearOutput();
            }
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


    /**
     * Called when the connection is established to clear any output that is
     * produced by the SSH console, such as banners and MOTDs.
     *
     * This standard implementation waits for at least 2 seconds of output.
     * You can override it to e.g. match for a specific prompt.
     */
    protected void clearOutput() throws EOFException, IOException, CommandException {
        discardOutput(2000);
    }


    /**
     * Try reading some output from stdout.
     */
    private String readNext(int timeout) throws EOFException, IOException {
        final int BUFF_SIZE = 8192;
        byte buffer[] = new byte[BUFF_SIZE];
        int conditions = session.waitForCondition(ChannelCondition.STDOUT_DATA | ChannelCondition.EOF, timeout);
        int offset = 0;
        if (0 != (conditions & ChannelCondition.EOF)) {
            throw new EOFException("Connection to console lost.");
        }
        while (stdout.available() > 0) {
            int maxLen = BUFF_SIZE - offset;
            int result = stdout.read(buffer, offset, maxLen);
            if (result > 0) {
                offset += result;
            }
        }
        return new String(buffer, 0, offset);
    }


    /**
     * Discard all output until a line with a pattern occurs.
     */
    protected void discardUntilPattern(Pattern pat, int timeout) throws EOFException, IOException, CommandException {
        int conditions;
        String buffer = "";
        int offset = 0;

        boolean found = false;

        while (!found) {
            String read = readNext(timeout);

            // Stop if no progression
            if (read.equals(""))
                break;

            buffer += read;
            int end = buffer.lastIndexOf('\n');

            if (end > 0) {
                Matcher m = pat.matcher(buffer);
                m.region(end, buffer.length());
                if (m.matches())
                    found = true;
                buffer = "";
            }
        }

        if (!found) {
            throw new CommandException("Expected response not received.");
        }
    }


    /**
     * Discards all output until timeout or EOF.
     */
    protected void discardOutput(int timeout) throws EOFException, IOException {
        readNext(timeout);
    }
}
