package orca.util.password.hash;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import org.junit.Test;

public class TestPasswordHash {
	
	@Test
	public void run() throws InvalidKeySpecException, NoSuchAlgorithmException {
		String pass = "mypassword";
		String hash = OrcaPasswordHash.generatePasswordHash(pass);
		assert(OrcaPasswordHash.validatePassword("mypassword", hash));
		assert(!OrcaPasswordHash.validatePassword("someotherpassword", hash));
	}
	
	public static void main(String[] argv) {
		TestPasswordHash tph = new TestPasswordHash();
		
		try {
			tph.run();
		} catch (Exception e) {
			System.err.println("Exception: " + e);
		}
		System.out.println("OK");
	}
}
