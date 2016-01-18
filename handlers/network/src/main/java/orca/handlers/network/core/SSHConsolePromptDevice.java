package orca.handlers.network.core;

import java.io.EOFException;
import java.io.IOException;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringEscapeUtils;


/**
 * A device that shows a predictable prompt over an SSH connection.
 *
 * This class makes use of the fact that while there is no mechanism in the SSH
 * protocol to determine the end of a command after a shell we can recognize
 * a fixed prompt pattern instead of waiting until command output has stopped.
 */
public abstract class SSHConsolePromptDevice extends SSHConsoleDevice {

    protected abstract String getPromptPattern();
    private final Pattern promptPattern;

    public SSHConsolePromptDevice(String deviceAddress, String uid, String password) {
        super(deviceAddress, uid, password);

        promptPattern = Pattern.compile(getPromptPattern());
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
                    sendCommand(cmd);
                }
                logger.debug("Command sent");

                // Check for EOF, if so no need for response
                if (response != null && response.equals("eof")) {
                    return;
                }

                // TODO: support "expect" properly for cases where it shouldn't
                // be the prompt. This does not seem to be the case now in
                // practice.
                if (!isEmulationEnabled())
                    discardUntilPattern(promptPattern, Integer.parseInt(timeout));
            } catch (EOFException e) {
                throw new CommandException("Connection lost to device.", e);
            } catch (IOException e) {
                throw new CommandException(e);
            }
        }
    }

    /**
     * Clear initial response buffer.
     *
     * Waits for at least 2 seconds of output and then matches the device
     * specific prompt.
     * 
     * XXX: this override is a compatilibity hack so that connect() does not
     * need to be modified in the parent class. This should be fixed once the
     * code is stabilized.
     */
    @Override
    protected byte[] getOutput(int timeout) throws IOException {
        if (!isEmulationEnabled())
            discardUntilPattern(promptPattern, 2000);

        return null;
    }

    /**
     * Discard all output until a line with a pattern occurs.
     */
    protected void discardUntilPattern(Pattern pat, int timeout) throws IOException {
        int conditions;
        String buffer = "";
        int offset = 0;
        boolean found = false;

        while (!found) {
            String read = readOutput(timeout);
            logger.debug("SSH output: " + StringEscapeUtils.escapeJava(read));

            // Stop if no progress
            if (read.equals(""))
                break;

            buffer += read;

            // Skip over any existing lines
            int end = buffer.lastIndexOf('\n');
            if (end >= 0) {
                // Search/preserve partial output
                buffer = buffer.substring(end + 1);
            }

            found = pat.matcher(buffer).matches();
        }

        if (!found) {
            throw new IOException("Expected response not received.");
        }
    }
}
