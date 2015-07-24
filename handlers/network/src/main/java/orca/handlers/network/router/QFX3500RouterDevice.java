package orca.handlers.network.router;

import java.util.Properties;

import orca.handlers.network.core.CommandException;

public class QFX3500RouterDevice extends JuniperRouterDevice implements
		IMappingRouterDevice {

	public QFX3500RouterDevice(String device, String uid, String pass) {
		super(device, uid, pass, "/orca/handlers/network/router/junos/qfx3500");
	}
	
	public void mapVLANs(String sourceTag, String destinationTag, String port)
			throws CommandException {
		// form the command buffer
		commandBuffer = "";

		Properties props = getProperties();
        props.setProperty(PropertyVLANNm, PropertyVLANNamePrefix + sourceTag);
        props.setProperty(PropertySrcVLAN, sourceTag);
        props.setProperty(PropertyDstVLAN, destinationTag);
        props.setProperty(PropertyPort, port);
		
		String cmd = loadScript(MapVlansStub);
        
        commandBuffer = replaceVars(cmd, props);
        
        logger.debug("JUNOS Command buffer content: " + commandBuffer);
        
        genericUpdateCommand("mapVLAN");
	}

	public void unmapVLANs(String sourceTag, String destinationTag, String port)
			throws CommandException {
		// form the command buffer
		commandBuffer = "";
		
		String cmd = loadScript(UnmapVlansStub);
	
		Properties props = getProperties();
		props.setProperty(PropertyVLANNm, PropertyVLANNamePrefix + sourceTag);
        props.setProperty(PropertySrcVLAN, sourceTag);
        props.setProperty(PropertyDstVLAN, destinationTag);
        props.setProperty(PropertyPort, port);
        
        // perform parameter substitution
        commandBuffer = replaceVars(cmd, props);
        
        logger.debug("JUNOS Command buffer content: " + commandBuffer);
        
        genericDeleteCommand("unmapLAN");
	}

}
