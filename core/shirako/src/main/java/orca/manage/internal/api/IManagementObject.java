package orca.manage.internal.api;

import java.util.Properties;

import orca.util.ID;

public interface IManagementObject {
	public void initialize() throws Exception;
	public ID getID();
	public Properties save();
	public String getActorName();
}