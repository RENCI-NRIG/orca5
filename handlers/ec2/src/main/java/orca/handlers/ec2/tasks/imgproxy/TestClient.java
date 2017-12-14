package orca.handlers.ec2.tasks.imgproxy;

import java.io.ByteArrayInputStream;
import java.util.Properties;

import org.apache.tools.ant.BuildException;

public class TestClient {

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

        String FILE_SYSTEM_IMAGE_KEY = "FILESYSTEM";
        String KERNEL_IMAGE_KEY = "KERNEL";
        String RAMDISK_IMAGE_KEY = "RAMDISK";
        String ERROR_CODE = "ERROR";

        String url = "http://some.url.of.image.metadata";
        String signature = "testSignature";

        try {
            System.out.println("Starting");
            IMAGEPROXYStub stub = new IMAGEPROXYStub("http://geni-test.renci.org:11080/axis2/services/IMAGEPROXY");
            stub._getServiceClient().getOptions().setTimeOutInMilliSeconds(1000 * 3600);
            RegisterImage ri = new RegisterImage();
            ri.setUrl(url);
            ri.setSignature(signature);
            RegisterImageResponse rir = null;
            System.out.println("Calling");
            rir = stub.RegisterImage(ri);
            String returnVal = rir.get_return();
            if (!returnVal.equals(ERROR_CODE)) {
                Properties imageIds = new Properties();
                ByteArrayInputStream stream = new ByteArrayInputStream(returnVal.getBytes());
                imageIds.load(stream);

                System.out.println("EMI is " + imageIds.getProperty(FILE_SYSTEM_IMAGE_KEY));
                if (imageIds.getProperty(KERNEL_IMAGE_KEY) != null) {
                    System.out.println("EKI is " + imageIds.getProperty(KERNEL_IMAGE_KEY));
                }
                if (imageIds.getProperty(RAMDISK_IMAGE_KEY) != null) {
                    System.out.println("ERI is " + imageIds.getProperty(RAMDISK_IMAGE_KEY));
                }
            } else {
                System.out.println("Error");
            }
        } catch (java.lang.Exception e) {
            throw new BuildException("ImageProxy unable to retrieve image: " + e.toString());
        }

    }

}
