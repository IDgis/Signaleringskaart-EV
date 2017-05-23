package nl.prv.veiligheidstoets;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;


public class SpatialQuery {
	private static final Logger LOGGER = Logger.getLogger(SpatialQuery.class.getName());
	static {
		LOGGER.setLevel(Level.ALL);
	}
	
	private String urlstr;
	private String template;
	private String featureResult;

	public SpatialQuery(String urlstr, String template) {
		this.urlstr = urlstr;
		this.template = template;
		processFilter();
	}
	
	/**
	 * Gets the feature response of the request with the filled template.
	 */
	private void processFilter() {
		URL url = null;
		HttpURLConnection hpcon = null;
		StringBuilder response = new StringBuilder(256);
		try {
			url = new URL(urlstr);
			hpcon = (HttpURLConnection)url.openConnection();
			hpcon.setRequestMethod("POST");
			hpcon.setRequestProperty("Content-Length", "" + Integer.toString(template.getBytes().length));
			hpcon.setRequestProperty("Content-Type", "xml/text");
			hpcon.setUseCaches(false);
			hpcon.setDoInput(true);
			hpcon.setDoOutput(true);
		} catch (IOException e) {
			LOGGER.log(Level.FATAL, e.toString(), e);
		}
		if(hpcon != null) {
			try(DataOutputStream printout = new DataOutputStream(hpcon.getOutputStream())) {
				printout.writeBytes(template);
			} catch (IOException e) {
				LOGGER.log(Level.FATAL, e.toString(), e);
			}
			try(BufferedReader in = new BufferedReader(new InputStreamReader(hpcon.getInputStream()))) {
				String input;
				while((input = in.readLine()) != null) {
					response.append(input + "\r");
				}
			} catch(IOException e) {
				LOGGER.log(Level.WARN, String.format("fout in request naar %s met filter %s", urlstr, template));
				LOGGER.log(Level.FATAL, e.toString(), e);
			} finally {
				hpcon.disconnect();
			}
		}
		if(response.toString().indexOf("ExceptionReport") > -1) {
			LOGGER.log(Level.FATAL, String.format("fout in request naar %s met filter %s response: %s", urlstr, template, response.toString()));
		}
		
		featureResult = response.toString();
	}
	
	/**
	 * Gets the filtered properties in a xml string
	 * @return
	 */
	public String getFeatureResult() {
		return featureResult;
	}
}
