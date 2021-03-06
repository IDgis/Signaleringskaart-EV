package nl.prv.veiligheidstoets;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;


public class SpatialQuery {
	private static final Logger LOGGER = Logger.getLogger(SpatialQuery.class.getName());
	
	private static final int TIMEOUT_VALUE = 20000; // Time in milliseconds to wait for a url to connect
	
	private String urlstr;
	private String template;
	private String featureResult;

	public SpatialQuery(String urlstr, String template) {
		LOGGER.setLevel(Level.INFO);
		this.urlstr = urlstr;
		this.template = template;
	}
	
	/**
	 * Gets the feature response of the request with the filled template.
	 */
	public String processFilter() {
		URL url = null;
		HttpURLConnection hpcon = null;
		StringBuilder response = new StringBuilder(256);
		try {
			url = new URL(urlstr);
			hpcon = (HttpURLConnection)url.openConnection();
			hpcon.setConnectTimeout(TIMEOUT_VALUE);
			hpcon.setReadTimeout(TIMEOUT_VALUE);
			hpcon.setRequestMethod("POST");
			hpcon.setRequestProperty("Content-Length", "" + Integer.toString(template.getBytes().length));
			hpcon.setRequestProperty("Content-Type", "xml/text");
			hpcon.setUseCaches(false);
			hpcon.setDoInput(true);
			hpcon.setDoOutput(true);
		} catch (SocketTimeoutException e) {
			LOGGER.log(Level.FATAL, e.toString(), e);
			return "Timeout in connection to " + urlstr + ". Probeer het later nogmaals of neem contact op met de beheerder als het probleem zich voor blijft doen.";
		} catch (IOException e) {
			LOGGER.log(Level.FATAL, e.toString(), e);
			return e.getMessage();
		}
		if(hpcon != null) {
			try(DataOutputStream printout = new DataOutputStream(hpcon.getOutputStream())) {
				printout.writeBytes(template);
			} catch (IOException e) {
				LOGGER.log(Level.FATAL, e.toString(), e);
				return e.getMessage();
			}
			try(BufferedReader in = new BufferedReader(new InputStreamReader(hpcon.getInputStream()))) {
				String input;
				while((input = in.readLine()) != null) {
					response.append(input + "\r");
				}
			} catch(IOException e) {
				LOGGER.log(Level.FATAL, String.format("fout in request naar %s met filter %s", urlstr, template));
				LOGGER.log(Level.FATAL, e.toString(), e);
				return String.format("Er heeft zich een fout voorgedaan met het verzoek naar %s. Probeer het later nogmaals of neem contact op met de beheerder.", urlstr);
			} finally {
				hpcon.disconnect();
			}
		}
		if(response.toString().indexOf("ExceptionReport") > -1) {
			LOGGER.log(Level.FATAL, String.format("fout in request naar %s met filter %s response: %s", urlstr, template, response.toString()));
			return String.format("Er heeft zich een fout voorgedaan in het verzoek naar %s. Probeer het later nogmaals of neem contact op met de beheerder als het probleem zich voor blijft doen", urlstr);
		}
		
		featureResult = response.toString();
		return null;
	}
	
	/**
	 * Gets the filtered properties in a xml string
	 * @return
	 */
	public String getFeatureResult() {
		return featureResult;
	}
}
