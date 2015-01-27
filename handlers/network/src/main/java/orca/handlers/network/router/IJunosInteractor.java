package orca.handlers.network.router;

public interface IJunosInteractor {
    //
    // functions that do the sending
    //
    public void sendHandshakeAndLogin();
    public void sendCommit();
    public void sendEndSessionRequest ();
    public void sendCloseJunoscript();
    public void sendConfigurationUpdate();
}
