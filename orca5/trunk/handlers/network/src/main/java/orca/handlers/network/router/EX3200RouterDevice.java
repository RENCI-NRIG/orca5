package orca.handlers.network.router;


public class EX3200RouterDevice extends JuniperRouterDevice {
	public EX3200RouterDevice(String device, String uid, String pass) {
		super(device, uid, pass, "/orca/handlers/network/router/junos/ex3200");
	}
	
//	public static void main(String[] arstring) {
//		try {
//			EX3200RouterDevice jrd = new EX3200RouterDevice("ex3200.renci.ben", "user", "pass");
//			
//			//jrd.enableEmulation();
//			//jrd.deleteVLAN("345");
//			jrd.createVLAN("345", "Some QOS");
//		} catch(CommandException e) {
//			System.out.println("Command exception: " + e);
//		} catch(Exception e) {
//			System.out.println("Other exception: " + e);
//		}
//	}
//	
}
