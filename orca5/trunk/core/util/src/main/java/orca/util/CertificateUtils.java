package orca.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

public class CertificateUtils {
	public static Certificate decode(byte[] bytes) throws CertificateException {
		InputStream is = null;
		try {
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			is = new ByteArrayInputStream(bytes);
			return cf.generateCertificate(is);
		} catch (CertificateException e) {
			throw e;
		} catch (Exception e){
			throw new CertificateException("Could not decode certificate", e);
		} finally {
			if (is != null) {
				Closer.close(is);
			}
		}
	}
}