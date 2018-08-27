package orca.handlers.network.router;

import orca.handlers.network.core.CommandException;

public class G8264RouterDevice extends IBMRouterDevice {

    public G8264RouterDevice(String address, String user, String pass) throws CommandException {
        super(address, user, pass, "/orca/handlers/network/router/ibm/g8264");
    }

    private static Long[] burstSizes = { 32L, 64L, 128L, 256L, 512L, 1024L, 2048L, 4096L };

    /**
     * Create a vlan. Note that the IBM switches take QoS rate as 64-40000000 kilobits per second, a multiple of 64
     * kbps. Burst size as 32-4096 kilobits, one of 8 values: 32 64 128 256 512 1024 2048 4096
     */
    @Override
    public void createVLAN(String vlanTag, String qosRate, String qosBurstSize) throws CommandException {

        // validate inputs
        Long qInt = 0L;
        if ((qosRate != null) && ((qInt = Long.parseLong(qosRate.trim())) > 0)) {
            if (qInt % 64 != 0)
                throw new CommandException("QoS rate for device must be in multiples of 64kbps, not " + qosRate);
        }

        if ((qosBurstSize != null) && ((qInt = Long.parseLong(qosBurstSize.trim())) > 0)) {
            boolean accepted = false;
            for (Long bs : burstSizes) {
                if (qInt.compareTo(bs) == 0) {
                    accepted = true;
                    break;
                }
            }
            if (!accepted)
                throw new CommandException(
                        "QoS burst size must be in kbps, one of 32 64 128 256 512 1024 2048 4096, not " + qosBurstSize);
        }
        super.createVLAN(vlanTag, qosRate, qosBurstSize);
    }

    public static void main(String[] argv) {

        try {
            G8264RouterDevice ird = new G8264RouterDevice("hostname", "username", "password");
            // ird.enableEmulation();
            // ird.connect();

            // Integer vmap = ird.findFreeVmap();
            // System.out.println("First available vmap is " + vmap);

            // System.out.println(ird.findVlanVmap("1234"));

            // System.out.println("Creating vlan");
            // ird.createVLAN("1238", "64000", "256");

            ird.removeTrunkPortsFromVLAN("1238", "30-31");

            // ird.deleteVLAN("1238", true);

            ird.connect();
            System.out.println(ird.getDeviceConfiguration());
            ird.disconnect();

            // ird.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Exception with device: " + e);
        }

    }
}
