package orca.controllers;

public class OrcaControllerException extends Exception {

    public OrcaControllerException(String msg) {
        super("OrcaControllerException: " + msg);
    }

}
