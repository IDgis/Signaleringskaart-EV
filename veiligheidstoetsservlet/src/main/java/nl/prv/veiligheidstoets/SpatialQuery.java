package nl.prv.veiligheidstoets;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import nl.prv.veiligheidstoets.util.LogStream;

public class SpatialQuery {
	private String urlstr;
	private Map params;
	private LogStream logStream; 
	private String filter; 
	private String un; 
	private String pw; 
	
	public SpatialQuery(String urlstr, String filter, LogStream logStream, String un, String pw) {
		 this.urlstr = urlstr;
		 this.filter = filter;
		 this.logStream = logStream; 
		 this.un = un;
		 this.pw = pw;
	 }
	 
	public String getResult() throws IOException {
		return postDataToService();
	}
		
	 private String postDataToService() throws IOException {
		 URL url = new URL(urlstr);
		 HttpURLConnection hpcon = null;
		 try{
			 hpcon = (HttpURLConnection) url.openConnection();   
			 hpcon.setRequestMethod("POST");
			 if (hpcon instanceof HttpsURLConnection) {
				    hpcon = (HttpsURLConnection) hpcon;
				    String userPassword = un + ":" + pw;
					String encoding = new sun.misc.BASE64Encoder().encode(userPassword.getBytes());
				    hpcon.setRequestProperty("Authorization", "Basic " + encoding);	
			}
			hpcon.setRequestProperty("Content-Length", "" + Integer.toString(filter.getBytes().length));      
			hpcon.setRequestProperty("Content-Type", "xml/text");
			hpcon.setUseCaches (false);
			hpcon.setDoInput(true);
			hpcon.setDoOutput(true);
			DataOutputStream printout = new DataOutputStream (hpcon.getOutputStream ());
			printout.writeBytes (filter);
			printout.flush ();
			printout.close ();		
			BufferedReader in = new BufferedReader(new InputStreamReader(hpcon.getInputStream()));
			String input;
			StringBuffer response = new StringBuffer(256);
			while((input = in.readLine()) != null) {
				response.append(input + "\r");
			}
			if (response.toString().indexOf("ExceptionReport") > 0){
				System.out.println("fout in request naar " + urlstr + " met filter " + filter + " response: " + response.toString());
			}
			return response.toString();	
			
			
		} catch(Exception e){
			System.out.println("fout in request naar " + urlstr + " met filter " + filter);
			throw new IOException(e.getMessage());	
		} finally {
			if(hpcon != null){
				hpcon.disconnect();	
			}
		} 
	 }
	 
	
	
	 
	 

}
