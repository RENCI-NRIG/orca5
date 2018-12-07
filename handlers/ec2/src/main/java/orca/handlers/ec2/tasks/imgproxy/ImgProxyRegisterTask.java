package orca.handlers.ec2.tasks.imgproxy;

import java.io.ByteArrayInputStream;
import java.util.Properties;

import orca.shirako.plugins.config.OrcaAntTask;

import org.apache.axis2.AxisFault;
import org.apache.tools.ant.BuildException;

public class ImgProxyRegisterTask extends OrcaAntTask {
    String url, signature, emiPropertyName, qcow2PropertyName, ekiPropertyName, eriPropertyName, statusPropertyName, imgProxyServiceUrl;
    int axisTimeout = 3600;

    public static final String FILE_SYSTEM_IMAGE_KEY = "FILESYSTEM";
    public static final String QCOW2_SYSTEM_IMAGE_KEY = "QCOW2";
    public static final String KERNEL_IMAGE_KEY = "KERNEL";
    public static final String RAMDISK_IMAGE_KEY = "RAMDISK";
    public static final String ERROR_CODE = "ERROR";

    @Override
    public void execute() throws BuildException {
        super.execute();
        String status = ERROR_CODE;

        if ((signature == null) || (url == null))
            throw new BuildException("Missing image signature or URL parameter");

        try {
            IMAGEPROXYStub stub = new IMAGEPROXYStub(imgProxyServiceUrl);
            stub._getServiceClient().getOptions().setTimeOutInMilliSeconds(1000 * axisTimeout);
            RegisterImage ri = new RegisterImage();
            ri.setUrl(url);
            ri.setSignature(signature);
            RegisterImageResponse rir = null;
            rir = stub.RegisterImage(ri);
            String returnVal = rir.get_return();
            if (!returnVal.equals(ERROR_CODE)) {
                Properties imageIds = new Properties();
                ByteArrayInputStream stream = new ByteArrayInputStream(returnVal.getBytes());
                imageIds.load(stream);

                // set the indicated property to actual value or empty string
                if (imageIds.getProperty(FILE_SYSTEM_IMAGE_KEY) != null) {
                    getProject().setNewProperty(emiPropertyName, imageIds.getProperty(FILE_SYSTEM_IMAGE_KEY));
                }
                if (imageIds.getProperty(QCOW2_SYSTEM_IMAGE_KEY) != null) {
                    getProject().setNewProperty(qcow2PropertyName, imageIds.getProperty(QCOW2_SYSTEM_IMAGE_KEY));
                }
                if (imageIds.getProperty(KERNEL_IMAGE_KEY) != null) {
                    getProject().setNewProperty(ekiPropertyName, imageIds.getProperty(KERNEL_IMAGE_KEY));
                } else
                	getProject().setNewProperty(ekiPropertyName, "");
                if (imageIds.getProperty(RAMDISK_IMAGE_KEY) != null) {
                    getProject().setNewProperty(eriPropertyName, imageIds.getProperty(RAMDISK_IMAGE_KEY));
                } else
                	getProject().setNewProperty(eriPropertyName, "");

                status = "SUCCESS";
            }
        } catch (AxisFault e) {
            throw new BuildException("ImageProxy unable to retrieve image: " + e.toString());
        } catch (java.lang.Exception e) {
            throw new BuildException("ImageProxy unable to retrieve image: " + e.toString());
        }

        getProject().setNewProperty(statusPropertyName, status);
    }

    // attribute setters
    public void setUrl(String url) {
        this.url = url;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public void setEmiPropertyName(String emiPropertyName) {
        this.emiPropertyName = emiPropertyName;
    }

    public void setQcow2PropertyName(String qcow2PropertyName) {
        this.qcow2PropertyName = qcow2PropertyName;
    }

    public void setEkiPropertyName(String ekiPropertyName) {
        this.ekiPropertyName = ekiPropertyName;
    }

    public void setEriPropertyName(String eriPropertyName) {
        this.eriPropertyName = eriPropertyName;
    }

    public void setStatusPropertyName(String statusPropertyName) {
        this.statusPropertyName = statusPropertyName;
    }

    public void setImgproxyServiceUrl(String url) {
        imgProxyServiceUrl = url;
    }

    public void setAxisTimeout(String to) {
        axisTimeout = Integer.parseInt(to);
    }

}
