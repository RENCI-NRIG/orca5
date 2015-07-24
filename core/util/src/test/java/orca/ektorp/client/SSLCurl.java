package orca.ektorp.client;

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

import orca.util.ssl.MultiKeyManager;

import org.apache.commons.codec.binary.Base64;


public class SSLCurl 
{
    public static void main(String[] args)
    {
        SSLCurl curl = new SSLCurl();
        
       // url -k -X PUT https://admin:exoadmin@slookup.exogeni.net:6984/panama/_security  -d 
        //	'{"admins":{"names":["mama"], "roles":[]}, "members":{"names":["mama"],"roles":[]}}'
        
       		 
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
        	        	//	System.out.println("RESULT added to panama: " + theResult3);
        	        	      System.out.println("Added papa to database members list");
        	        		 System.exit(1);
        	        
       url = "https://admin:exoadmin@slookup.exogeni.net:6984/panama/_security";
       method = "PUT";
       
       		 data =  " {\"admins\":{\"names\":[\"mama\"],\"roles\":[]},"
       	              + "\"members\":{\"names\":[\"mama\"],\"roles\":[]}}";
       	       result = curl.doHttpCall(url, method, data);
       		System.out.println("RESULT added to panama: " + result);
       		 System.exit(1);
       		 
       		 
       		 
        url = "http://127.0.0.1:5984/employee";
        method = "PUT";
        String createResult = curl.doHttpCall(url, method, data);
        System.out.println("DB CREATED : "+createResult);

        url = "http://127.0.0.1:5984/_all_dbs";
        method= "GET";
        data = null;
        String getAllDB2 = curl.doHttpCall(url, method, data);
        System.out.println("ALL DB's : "+getAllDB2);
        
        url = "http://127.0.0.1:5984/employee";
        method = "POST";
        data =  "{\"Name\":\"Ashish\","
              + "\"EmployeeID\":\"1\","
              + "\"Experience\":\"Don't required\","
              + "\"Designation\":\"Java/J2EE Developer\","
              + "\"Group\":\"javatute\"}";
        String postResult = curl.doHttpCall(url, method, data);
        System.out.println("DOCUMENT INSERTED : "+postResult);
        
        url = "http://127.0.0.1:5984/employee";
        method = "DELETE";
        data = null;
        String deleteResult = curl.doHttpCall(url, method, data);
        System.out.println("DB DELETED : "+deleteResult);
        
        url = "http://127.0.0.1:5984/_all_dbs";
        method= "GET";
        data = null;
        String getAllDB3 = curl.doHttpCall(url, method, data);
        System.out.println("ALL DB's : "+getAllDB3);
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
						String registryCertFingerprint = "df:5c:1d:99:46:9a:5f:a8:92:8e:15:e4:b9:82:d8:ad";
						// convert to byte array
						String[] fingerPrintBytes = registryCertFingerprint.split(":");

						for (int i = 0; i < 16; i++ )
						{
							registryCertDigest[i] = (byte)(Integer.parseInt(fingerPrintBytes[i], 16) & 0xFF);
						}
						byte[] certDigest = md.digest(certs[0].getEncoded());
						if (!Arrays.equals(certDigest, registryCertDigest)) {
							System.err.println("Certificate presented by registry does not match local copy, communications with registry is not possible");
							
							
							throw new CertificateException();
						}
					} catch (NoSuchAlgorithmException e) {

					} catch (Exception e) {
						System.err.println("Unable to compare server certificate digest to the existing registry digest: " + e.toString());
					
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
   //     HttpURLConnection httpUrlConnection = null;

        try 
        {
            URL url = new URL(urlAsString);
      //      httpUrlConnection = (HttpURLConnection) url.openConnection();
            httpsUrlConnection = (HttpsURLConnection) url.openConnection();
            
            httpsUrlConnection.setSSLSocketFactory(mysslsf);
            httpsUrlConnection.setHostnameVerifier(allHostsValid);
     //       httpUrlConnection.setDoInput(true);
            httpsUrlConnection.setDoInput(true);
   //         httpUrlConnection.setRequestMethod(method);
            httpsUrlConnection.setRequestMethod(method);
          
        System.out.println("HERE!");
        //    print_https_cert(httpsUrlConnection);
          //  print_content(httpsUrlConnection);
         //  System.exit(1); 
            
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
            	System.out.println("doc is not empty "+doc.getBytes(charsetName).length);
            	System.out.println("DOC "+doc);
                httpsUrlConnection.setDoOutput(true);
                httpsUrlConnection.setRequestProperty("Content-Length", "" + doc.getBytes(charsetName));

                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(httpsUrlConnection.getOutputStream(), charsetName);
                outputStreamWriter.write(doc);
                outputStreamWriter.close();
            }

            readInputStream(result, httpsUrlConnection.getInputStream());
            System.out.println("It read from readInputStream");

        }
        catch (RuntimeException e) 
        {
            System.out.println(e.getMessage());
        }
        catch (MalformedURLException e) 
        {
            System.out.println("The url '" + urlAsString + "' is malformed.");
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
                    System.out.println("This is as low as we can get, nothing worked!");
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
