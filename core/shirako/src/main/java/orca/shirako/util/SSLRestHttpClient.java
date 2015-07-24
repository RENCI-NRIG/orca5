package orca.shirako.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.cert.CertificateException;

import orca.shirako.container.Globals;
import orca.shirako.container.OrcaContainer;
import orca.util.ssl.MultiKeyManager;

import org.apache.commons.codec.binary.Base64;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * 
 * @author claris
 * 
 *
 */
public class SSLRestHttpClient {

	
	  public static void main(String[] args)
	    {
	        SSLRestHttpClient curl = new SSLRestHttpClient();
	        
	        String url = "https://admin:exoadmin@slookup.exogeni.net:6984/_all_dbs";
	        String method= "GET";
	      String   data = null;
	      String result = null;
	       // String result = curl.doHttpCall(url, method, data);
	        //System.out.println("ALL DB's : "+result);
	        
	        
	        url = "https://admin:exoadmin@slookup.exogeni.net:6984/_users/org.couchdb.user:papa";
	        method = "PUT";
	        
	        		 data =  " {\"_id\":\"org.couchdb.user:papa\","
	        	              + "\"name\":\"papa\","
	        	              + "\"roles\":[],"
	        	              + "\"type\":\"user\","
	        	              + "\"password\":\"papa\"}";
	        		
	        //result = curl.doHttpCall(url, method, data);
	        //System.out.println("Papa user added");
	      //  System.out.println("user added : "+theResult);
	       
	        		  url = "https://admin:exoadmin@slookup.exogeni.net:6984/panama/_security";
	        	         method = "PUT";
	        	        
	        	        		 data =  "{\"admins\":{\"names\":[\"papa\"],\"roles\":[]},"
	        	        	              + "\"members\":{\"names\":[\"papa\"],\"roles\":[]}}";
	        	        	      result = curl.doHttpCall(url, method, data);
	        	        		System.out.println("RESULT added to panama: " + result);
	        	        	      System.out.println("Added papa to database members list");
	        	        	
	        	        	      url = "https://admin:exoadmin@slookup.exogeni.net:6984/panama/_security";
	        		        	    method = "GET";
	        		        	    data = null;
	        		        	    result = curl.doHttpCall(url, method, data);
	        		        	    System.out.println("MY RESULT is "+result);
	        		        	  
	        		        	    try {
	        		        	    	ObjectMapper mapper = new ObjectMapper();
										JsonNode actualObj = mapper.readTree(result);
										JsonNode members = actualObj.get("members");
										JsonNode names = members.get("names");
										
										if(names.isArray()) 
										{
											ArrayNode arraynode = (ArrayNode)names;
											arraynode.add("mama");
										}
										
										System.out.println("New json + " +actualObj.toString());
									} catch (JsonProcessingException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
	        		        	    System.exit(1);
	        	        
	       url = "https://admin:exoadmin@slookup.exogeni.net:6984/panama/_security";
	       method = "PUT";
	       
	       		 data =  " {\"admins\":{\"names\":[\"papa\"],\"roles\":[]},"
	       	              + "\"members\":{\"names\":[\"papa\"],\"roles\":[]}}";
	       	       result = curl.doHttpCall(url, method, data);
	       		System.out.println("RESULT added to panama: " + result);
	     
	    }
	    public String doHttpCall(String urlAsString, String method, String doc) 
	    {
	        StringBuffer result = new StringBuffer();
	        Map<String, String> requestProperties = null;
	        requestProperties = new HashMap<String, String>();
	        requestProperties.put("Content-Type", "application/json");
	        String charsetName = "UTF8";
	        SSLSocketFactory sslSocketFactory =null;
	        SSLContext context=null;
	        try {
				 context = SSLContext.getInstance("TLS");
				MultiKeyManager km = new MultiKeyManager();
				TrustManager tm = 
						 new X509TrustManager() {
					public X509Certificate[] getAcceptedIssuers() {
						
						// return 0 size array, not null, per spec
						return new X509Certificate[0];
					}

					public void checkClientTrusted(X509Certificate[] certs, String authType) {
						// Trust always
					}

					public void checkServerTrusted(X509Certificate[] certs, String authType) {
						// Trust always
						MessageDigest md = null;
						try {
							md = MessageDigest.getInstance("MD5");

							if (certs.length == 0) 
								{
								throw new CertificateException();
								}
							
							byte[] registryCertDigest = new byte[16];
							String registryCertFingerprint = Globals.getContainer().getConfiguration().getProperty(OrcaContainer.PropertyRegistryCertFingerprint_1).trim();

							// convert to byte array
							String[] fingerPrintBytes = registryCertFingerprint.split(":");

							for (int i = 0; i < 16; i++ )
							{
								registryCertDigest[i] = (byte)(Integer.parseInt(fingerPrintBytes[i], 16) & 0xFF);
							}
							byte[] certDigest = md.digest(certs[0].getEncoded());
							if (!Arrays.equals(certDigest, registryCertDigest)) {
								Globals.Log.error("Certificate presented by registry does not match local copy, communications with registry is not possible");
								
								
								throw new CertificateException();
							}
						} catch (NoSuchAlgorithmException e) {

						} catch (Exception e) {
							Globals.Log.error("Unable to compare server certificate digest to the existing registry digest: " + e.toString());
						
						}
					}

				};
				context.init(new MultiKeyManager[]{km}, new TrustManager[] {tm}, null);
				
				 
	        } catch (NoSuchAlgorithmException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			} catch (KeyManagementException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        
	        HostnameVerifier allHostsValid = new HostnameVerifier() {

				@Override
				public boolean verify(String hostname, SSLSession session) {
					// TODO Auto-generated method stub
					return true;
				}
	        	
				
	        };
	        SSLSocketFactory mysslsf = context.getSocketFactory();
	        HttpsURLConnection httpsUrlConnection = null;

	        try 
	        {
	            URL url = new URL(urlAsString);
	            httpsUrlConnection = (HttpsURLConnection) url.openConnection();
	            
	            httpsUrlConnection.setSSLSocketFactory(mysslsf);
	            httpsUrlConnection.setHostnameVerifier(allHostsValid);
	            httpsUrlConnection.setDoInput(true);
	            httpsUrlConnection.setRequestMethod(method);
	          
	            if(url.getUserInfo() != null) 
	            {
	                String basicAuth = "Basic " + new String(new Base64().encode(url.getUserInfo().getBytes()));
	                httpsUrlConnection.setRequestProperty("Authorization", basicAuth);
	            }
	            
	            httpsUrlConnection.setRequestProperty("Content-Length", "0");
	            for (String key : requestProperties.keySet()) 
	            {
	                httpsUrlConnection.setRequestProperty(key, requestProperties.get(key));
	            }

	            if(doc != null && !doc.isEmpty()) 
	            {
	            	
	                httpsUrlConnection.setDoOutput(true);
	                httpsUrlConnection.setRequestProperty("Content-Length", "" + doc.getBytes(charsetName));

	                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(httpsUrlConnection.getOutputStream(), charsetName);
	                outputStreamWriter.write(doc);
	                outputStreamWriter.close();
	            }

	            readInputStream(result, httpsUrlConnection.getInputStream());
	     
	        }
	        catch (RuntimeException e) 
	        {
	            System.out.println(e.getMessage());
	        }
	        catch (MalformedURLException e) 
	        {
	            Globals.Log.error("The url '" + urlAsString + "' is malformed.");
	        }
	        catch (IOException e) 
	        {
	            try 
	            {
	                result.append(e.getMessage());
	                readInputStream(result, httpsUrlConnection.getErrorStream());
	                if ("".equals(result.toString())) 
	                {
	                    result.append("Error ");
	                    result.append(httpsUrlConnection.getResponseCode());
	                    result.append(" : ");
	                    result.append(httpsUrlConnection.getResponseMessage());
	                    result.append(".  Exception message is: [");
	                    result.append(e.getMessage());
	                    result.append("]");
	                }
	            } 
	            catch (IOException e1) {}
	        }
	        finally 
	        {
	            if ("HEAD".equalsIgnoreCase(method)) 
	            {
	                try 
	                {
	                    result.append(httpsUrlConnection.getResponseMessage());
	                }
	                catch (IOException e) 
	                {
	                    Globals.Log.debug("This is as low as we can get, nothing worked!");
	                    e.printStackTrace();
	                }
	            }
	            if (httpsUrlConnection != null)
	                httpsUrlConnection.disconnect();
	        }
	       
	        return result.toString();
	    }

	    private void readInputStream(StringBuffer result, InputStream inputStream) throws UnsupportedEncodingException, IOException 
	    {
	        String charsetName = "UTF8";
	        if (inputStream == null)
	            {
	        	throw new IOException("No working inputStream.");
	            }
	        InputStreamReader streamReader = new InputStreamReader(inputStream, charsetName);
	        BufferedReader bufferedReader = new BufferedReader(streamReader);

	        String row;
	        while ((row = bufferedReader.readLine()) != null) 
	        {
	            result.append(row);
	            result.append("\n");
	        }

	        bufferedReader.close();
	        streamReader.close();
	    }
	    
	    private void print_https_cert(HttpsURLConnection con){
	    	 
	        if(con!=null){
	     
	          try {
	     
	    	System.out.println("Response Code : " + con.getResponseCode());
	    	System.out.println("Cipher Suite : " + con.getCipherSuite());
	    	System.out.println("\n");
	     
	    	Certificate[] certs = con.getServerCertificates();
	    	for(Certificate cert : certs){
	    	   System.out.println("Cert Type : " + cert.getType());
	    	   System.out.println("Cert Hash Code : " + cert.hashCode());
	    	   System.out.println("Cert Public Key Algorithm : " 
	                                        + cert.getPublicKey().getAlgorithm());
	    	   System.out.println("Cert Public Key Format : " 
	                                        + cert.getPublicKey().getFormat());
	    	   System.out.println("\n");
	    	}
	     
	    	} catch (SSLPeerUnverifiedException e) {
	    		e.printStackTrace();
	    	} catch (IOException e){
	    		e.printStackTrace();
	    	}
	     
	         }
	     
	       }
	     
	       private void print_content(HttpsURLConnection con){
	    	if(con!=null){
	     
	    	try {
	     
	    	   System.out.println("****** Content of the URL ********");			
	    	   BufferedReader br = 
	    		new BufferedReader(
	    			new InputStreamReader(con.getInputStream()));
	     
	    	   String input;
	     
	    	   while ((input = br.readLine()) != null){
	    	      System.out.println(input);
	    	   }
	    	   br.close();
	     
	    	} catch (IOException e) {
	    	   e.printStackTrace();
	    	}
	     
	           }
	     
	       }
	     
}
