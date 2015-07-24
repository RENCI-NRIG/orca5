package orca.manage;

import orca.manage.beans.EventMng;

public interface IOrcaEventHandler {
	public void handle(EventMng e);
	public void error(OrcaError error);
}