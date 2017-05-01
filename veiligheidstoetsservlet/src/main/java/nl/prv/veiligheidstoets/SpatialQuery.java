package nl.prv.veiligheidstoets;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


public class SpatialQuery {
	private String urlstr;
	private String filter;
	private String un;
	private String pw;

	public SpatialQuery(String urlstr, String filter, String un, String pw) {
		this.urlstr = urlstr;
		this.filter = filter;
		this.un = un;
		this.pw = pw;
	}

	private String getResult() throws IOException {
		return postDataToService();
	}

	/**
	 * 
	 * @return Gets an xml with the data given in the postbody
	 * @throws IOException
	 */
	private String postDataToService() throws IOException {
		 URL url = new URL(urlstr);
		 HttpURLConnection hpcon = null;
		 try{
			 hpcon = (HttpURLConnection) url.openConnection();   
			 hpcon.setRequestMethod("POST");
			 if (hpcon instanceof HttpsURLConnection) {
				    String userPassword = un + ":" + pw;
				    String encoding = java.util.Base64.getEncoder().encodeToString(userPassword.getBytes());
					//String encoding = new sun.misc.BASE64Encoder().encode(userPassword.getBytes());
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
			StringBuilder response = new StringBuilder(256);
			while((input = in.readLine()) != null) {
				response.append(input + "\r");
			}
			if (response.toString().indexOf("ExceptionReport") > 0){
				System.out.println("fout in request naar " + urlstr + " met filter " + filter + " response: " + response.toString());
			}
			return response.toString();	
			
			
		} catch(Exception e){
			System.out.println("fout in request naar " + urlstr + " met filter " + filter);
			throw new IOException(e);	
		} finally {
			if(hpcon != null){
				hpcon.disconnect();	
			}
		} 
	}

	/**
	 * Returns the number of features in this query and returns them as a value in a Map
	 * @return
	 * @throws Exception
	 */
	public Map<String, String> getNumFeatures() throws Exception {
		Map<String, String> numFeatures = new HashMap<>();
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new ByteArrayInputStream(getResult().getBytes())));
			Element element = doc.getDocumentElement();
			int result = Integer.parseInt(element.getAttribute("numberOfFeatures"));
			numFeatures.put("numberOfFeaturesFound", Integer.toString(result));
		} catch (IOException | ParserConfigurationException | SAXException e) {
			numFeatures.put("error", e.getMessage());
			throw new Exception(e);
		}
		return numFeatures;
	}
	
	/**
	 * Gets the features for the given properties in a Map<FeatureName, values>
	 * @param properties
	 * @return
	 * @throws Exception
	 */
	public Map<String, String> getFeatureProperties(String[] properties) throws Exception {
		Map<String, String> featureProps = new HashMap<>();
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new ByteArrayInputStream(getResult().getBytes())));
			NodeList elementList = doc.getElementsByTagName("*");
			
			for(int i = 0; i < properties.length; i++) {
				List<String> valueList = getPropertyValues(elementList, properties[i]);
				String[] values = valueList.toArray(new String[0]);
				String valueString = parseToJsonArrayString(values);
				
				featureProps.put(properties[i], valueString);
			}
			
		} catch (ParserConfigurationException | SAXException | IOException e) {
			featureProps.put("error", e.getMessage());
			throw new Exception(e);
		}
		return featureProps;
	}
	
	/**
	 * 
	 * @param elementList
	 * @param property
	 * @return a list of nodes for the given property
	 */
	private List<String> getPropertyValues(NodeList elementList, String property) {
		List<String> valueList = new ArrayList<>();
		for(int i = 0; i < elementList.getLength(); i++) {
			Node value = elementList.item(i);
			if(value.getNodeName().endsWith(":" + property)) {
				String text = value.getTextContent().replaceAll("\"", "\'");
				valueList.add(text.trim());
			}
		}
		return valueList;
	}
	
	/**
	 * Creates a String starting with '[' and ending with ']' from an array
	 * @param values
	 * @return A single String containing the array
	 */
	private String parseToJsonArrayString(String[] values) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		
		for(int i = 0; i < values.length - 1; i++) {
			sb.append(values[i] + ",");
		}
		sb.append(values[values.length - 1] + "]");
		return sb.toString();
	}
}
