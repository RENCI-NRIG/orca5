package orca.network;

import junit.framework.TestCase;

public class SliceRequestValidTest extends TestCase {
	String requestFileName, substrateFileName;
	SliceRequest request;
	
	public SliceRequestValidTest(String arg0) {
		super(arg0);
	}

	protected void setUp() throws Exception {
		super.setUp();
		
		requestFileName="orca/network/request-6509.rdf";
		substrateFileName="orca/network/ben-dell.rdf";
		request= new SliceRequest(requestFileName,substrateFileName);
	}

	public void testIsRequestValid() {
		assertTrue(request.isRequestValid());
	}

}
