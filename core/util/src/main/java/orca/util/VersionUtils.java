package orca.util;

public class VersionUtils {
	public static final String ORCA_VERSION_STRING = "ORCA 5.0 Eastsound: ";

	public static final String buildVersion = (VersionUtils.class.getPackage().getImplementationVersion() != null ? ORCA_VERSION_STRING
			+ VersionUtils.class.getPackage().getImplementationVersion()
			: ORCA_VERSION_STRING);

	
	public static void main(String [] args){
		System.out.println(buildVersion);
	}
}
