/**
 * Copyright (c) 2009 Renaissance Computing Institute and Duke University
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and/or hardware specification (the �Work�) to deal in the Work without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Work, and to permit persons to whom the Work is furnished to do so, subject to
 * the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Work.
 * THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS
 * IN THE WORK.
 */

package orca.handlers.nlr;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.*; 
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.cookie.CookieSpec;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpParams;
import org.apache.log4j.Logger;

/**
 * This is a class that maintains login session with cosign in support
 * of Sherpa web services. See SherpaAPI for the actual useful things
 * you can do with Sherpa.
 * 
 * For details see http://noc.nlr.net/nlr/maps_documentation/nlr-framenet-documentation.html
 * 
 * @author ibaldin@renci.org
 *
 */
public class SherpaSession {
	private static final String defaultSherpaHostURL = "https://sherpa.nlr.net";
	private static final String planURLStub = "/services/planning.cgi";
	private static final String provisionURLStub = "/services/provisioning.cgi";
	private static final String postURL = "https://weblogin.grnoc.iu.edu/cosign-bin/cosign.cgi";
	private static final String sherpaService = "cosign-sherpa-GRNOC";
	private static final String sherpaRealm = "cosign-sherpa-GRNOC";
	
	private String planURL;
	private String provisionURL;
	private String sherpaLogin;
	private String sherpaPassword;
	private HttpClient dhc;
	private Logger logger;
	
	private boolean debug = false;
	
	private void ctor_init() {

		dhc = new HttpClient();
		dhc.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
		
	}
	
	/**
	 * Replace the default planning URL (https://sherpa.nlr.net/services/planning.cgi)
	 * @param url
	 */
	void replacePlanURL(String url) {
		planURL = url;
	}
	
	/**
	 * Replace the default provisioning URL (https://sherpa.nlr.net/services/provisioning.cgi)
	 * @param url
	 */
	void replaceProvisionURL(String url) {
		provisionURL = url;
	}
	
	/**
	 * This constructor requires you explicitly pass the login and password
	 * 
	 * @param l
	 * @param p
	 */
	public SherpaSession(String l, String p, Logger log) {
		sherpaLogin = l;
		sherpaPassword = p;
		planURL = defaultSherpaHostURL + planURLStub;
		provisionURL = defaultSherpaHostURL + provisionURLStub;
		logger = log;
		
		ctor_init();
	}
	
	/**
	 * This constructor take login and password from System properties 'login' and 'password'
	 */
	public SherpaSession() {
		sherpaLogin = System.getProperty("login");
		sherpaPassword = System.getProperty("password");
		planURL = defaultSherpaHostURL + planURLStub;
		provisionURL = defaultSherpaHostURL + provisionURLStub;

		ctor_init();
	}

	void handleLogin(GetMethod cmdHttpGet) throws IOException {
		// need to request again to get to the login form
		// get whatever it is (not really interested)
		
		// this time follow the redirect
		//enableRedirects();
		
		cmdHttpGet.setFollowRedirects(true);
		
		// get the login form
		dhc.executeMethod(cmdHttpGet);
				
		
		// New location (login form post)
        // ideally the form port URL should come from the form
        // right now we make it a constant
		PostMethod httpform = new PostMethod(postURL);
		
		// add username/password and other form values
		NameValuePair login = new NameValuePair("login", sherpaLogin);
		NameValuePair password = new NameValuePair("password", sherpaPassword);
		NameValuePair reqd = new NameValuePair("required","");
		NameValuePair ref = new NameValuePair("ref", cmdHttpGet.getURI().toString());
		NameValuePair service = new NameValuePair("service", sherpaService);
		NameValuePair realm = new NameValuePair("realm", sherpaRealm);		
		
		httpform.setRequestBody(new NameValuePair[] { login, password, reqd, ref, service, realm});
		
		//disableRedirects();
		httpform.setFollowRedirects(false);
		
		// post the form
		dhc.executeMethod(httpform);
		
	}
	
	public Reader executePlanningCmd(String urlizedCmd) {
		return executeCommand(planURL + urlizedCmd);
	}
	
	public Reader executeProvisioningCmd(String urlizedCmd) {
		return executeCommand(provisionURL + urlizedCmd);
	}
	
	// this presumes that appropriate planning or provisioning URL is prepended
	Reader executeCommand(String wellFormedCmdURL) {
		
		if (debug)
			System.out.println("Executing " + wellFormedCmdURL);
		try {
			
			GetMethod httpget = new GetMethod(wellFormedCmdURL);
	
			//disableRedirects();
			httpget.setFollowRedirects(false);
			
			dhc.executeMethod(httpget);
			
			if (httpget.getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
				// consume content and handle the login
				
				// get the redirected location
				Header location = httpget.getResponseHeader("location");
				
				handleLogin(httpget);
				
				// hit the redirected location again
				//enableRedirects();
				
				GetMethod httpget1 = new GetMethod(location.getValue());
				httpget1.setFollowRedirects(true);
				
				dhc.executeMethod(httpget1);
				InputStreamReader isr = new InputStreamReader(httpget1.getResponseBodyAsStream());
				return isr;
			}		

	        InputStreamReader isr = new InputStreamReader(httpget.getResponseBodyAsStream());
			
	        return isr;
			
		} catch(IOException e2) {
			e2.printStackTrace( System.err );
		}
		return null;
	}
	
//	String getCookiesAsString() {
//		CookieSpec cookiespec = CookiePolicy.getDefaultSpec();
//		Cookie[] cookies = cookiespec.match(arg0, arg1, arg2, arg3, arg4)
//        if (cookies.isEmpty()) {
//            return "";
//        } else {
//        	String ret = "";
//            for (int i = 0; i < cookies.size(); i++) {
//                ret += "-" + cookies.get(i).toString();
//            }
//            return ret;
//        }
//	}
	
//	public static void main(String [] argv) {
//		SherpaSession ss = new SherpaSession();
//		
//		//System.out.println("Cookies: " + ss.getCookiesAsString());
//		
//		//Reader ret = ss.executeCommand(planURL + "?method=get_available_vlan_id&net=1&wg=18");
//		Reader ret = ss.executeCommand(planURL + "?method=get_vlans&net=1&wg=18");
//		//Reader ret = ss.executeCommand(planURL );
//
////		try {
////		BufferedReader br = new BufferedReader(ret);
////		System.out.println(br.readLine());
////		} catch (IOException e1) {
////			
////		}
////		System.exit(0);
////		Gson gson = new Gson();
////		ErrorSuccess o = gson.fromJson(ret, ErrorSuccess.class);
////		
////		System.out.println("OK");
////		
////		System.out.println(o.error_text);
//
//		Gson gson = new Gson();
//		
//		GetVlansResponse er = gson.fromJson(ret, GetVlansResponse.class);
//		
//		VlanDefinition vd = er.results.get(3);
//		
//		System.out.println(vd.ckt_id);
//	}
}

