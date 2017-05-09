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
import java.util.logging.Level;
import java.util.logging.Logger;

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
	private static final Logger LOGGER = Logger.getLogger(SpatialQuery.class.getName());
	
	private String urlstr;
	private String filter;

	public SpatialQuery(String urlstr, String filter) {
		this.urlstr = urlstr;
		this.filter = filter;
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
			if (response.toString().indexOf("ExceptionReport") > -1){
				LOGGER.log(Level.SEVERE, "fout in request naar {0} met filter {1} response: {2}", new Object[]{ urlstr, filter, response.toString() });
				throw new IOException("fout in request naar " + urlstr + " met filter " + filter + " response: " + response.toString());
			}
			return response.toString();
		} catch(Exception e){
			LOGGER.log(Level.INFO, "fout in request naar {0} met filter {1}", new Object[]{ urlstr, filter });
			LOGGER.log(Level.SEVERE, e.toString(), e);
			throw new IOException("fout in request naar " + urlstr + " met filter " + filter);	
		} finally {
			if(hpcon != null){
				hpcon.disconnect();	
			}
		} 
	}
	
	/**
	 * Returns the number of properties found or a json of the features with the properties specified in the filter.
	 * @return
	 * @throws Exception
	 */
	public Map<String, String> getPropertyResult() {
		Map<String, String> properties = new HashMap<>();
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new ByteArrayInputStream(getResult().getBytes())));
			
			Document filterDoc = builder.parse(new InputSource(new ByteArrayInputStream(filter.getBytes())));
			String resultType = filterDoc.getDocumentElement().getAttribute("resultType");
			if("hits".equals(resultType)) {
				return getNumFeatures(doc);
			}
			
			// Getting filter names to apply
			List<String> filteredFeatures = new ArrayList<>();
			NodeList queryList = filterDoc.getElementsByTagName("wfs:Query");
			Element queryElement = (Element)queryList.item(0);
			Node childNode = queryElement.getFirstChild().getNextSibling();
			while("ogc:PropertyName".equals(childNode.getNodeName())) {
				filteredFeatures.add(childNode.getTextContent());
				childNode = childNode.getNextSibling().getNextSibling();
			}
			
			NodeList featureList = doc.getElementsByTagName("*");
			properties = getProperties(filteredFeatures, featureList);
		} 
		catch (IOException | ParserConfigurationException | SAXException e) {
			LOGGER.log(Level.SEVERE, e.toString(), e);
			properties.put("error", "\"" + e.getMessage() + "\"");
			return properties;
		}
		return properties;
	}
	
	/**
	 * Return the json of the kwetsbare objecten found.
	 * @param kwObjectsInBuffer
	 * @return
	 */
	public Map<String, String> getPropertyResult(List<KwetsbaarObject> kwObjectsInBuffer) {
		Map<String, String> features = new HashMap<>();
		if(kwObjectsInBuffer == null || kwObjectsInBuffer.isEmpty()) {
			features.put("message", "\"NO_FEATURES_FOUND\"");
			return features;
		}
		
		List<String> propertyList = new ArrayList<>();
		List<String> filteredFeatures = new ArrayList<>();
		filteredFeatures.add("id");
		filteredFeatures.add("gebruiksdoel");
		filteredFeatures.add("oppervlakte");
		filteredFeatures.add("position");
		
		for(int i = 0; i < kwObjectsInBuffer.size(); i++) {
			KwetsbaarObject obj = kwObjectsInBuffer.get(i);
			propertyList.add(obj.getId());
			propertyList.add(obj.getGebruiksDoel());
			propertyList.add(obj.getOppervlakte());
			propertyList.add(obj.getCoordX() + " " + obj.getCoordY());
		}
		
		LOGGER.log(Level.INFO, "Building result to display...");
		String resultString = parseToJsonString(filteredFeatures, propertyList);
		LOGGER.log(Level.INFO, resultString);
		features.put("features", resultString);
		
		return features;
	}
	
	/**
	 * returns the KwetsbaarObject if the number of features found is not zero.
	 * @param kwObject
	 * @param index
	 * @return
	 */
	public KwetsbaarObject getKwetsbaarObjectInBuffer(KwetsbaarObject kwObject) {
		Map<String, String> numFeatures = new HashMap<>();
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new ByteArrayInputStream(getResult().getBytes())));
			numFeatures = getNumFeatures(doc);
			if(!"0".equals(numFeatures.get("numberOfFeaturesFound"))) {
				return kwObject;
			}
		}
		catch(IOException | ParserConfigurationException | SAXException e) {
			LOGGER.log(Level.SEVERE, e.toString(), e);
		}
		return null;
	}
	
	/**
	 * Returns the number of features found in the given document
	 * @param doc
	 * @return
	 */
	private Map<String, String> getNumFeatures(Document doc) {
		Map<String, String> numFeatures = new HashMap<>();
		
		Element element = doc.getDocumentElement();
		int result = Integer.parseInt(element.getAttribute("numberOfFeatures"));
		numFeatures.put("numberOfFeaturesFound", Integer.toString(result));
		
		return numFeatures;
	}
	
	/**
	 * Returns a Map with the name and json array string of the features found
	 * @param filteredFeatures - The name of features to filter e.g. PR10-6, PR10-7, PAG
	 * @param elementList - All xml tags in the given filter
	 * @return
	 */
	private Map<String, String> getProperties(List<String> filteredFeatures, NodeList elementList) {
		Map<String, String> features = new HashMap<>();
		
		List<String> propertyList = new ArrayList<>();
		// Loop through all elements in the document
		for(int i = 0; i < elementList.getLength(); i++) {
			Node featureMemberNode = elementList.item(i);
			//Loop through the filters
			for(int j = 0; j < filteredFeatures.size(); j++) {
				// If filter matched the element, add it
				if(featureMemberNode.getNodeName().endsWith(":" + filteredFeatures.get(j))) {
					String textContent = featureMemberNode.getTextContent();
					if(textContent == null || "".equals(textContent)) {
						filteredFeatures.remove(j);
						continue;
					}
					propertyList.add(textContent);
					LOGGER.log(Level.INFO, "{0}, {1}", new Object[]{ featureMemberNode.getNodeName(), featureMemberNode.getTextContent() });
				}
			}
		}
		
		if(propertyList.isEmpty()) {
			features.put("message", "\"NO_FEATURES_FOUND\"");
			return features;
		}
		
		// DEBUGGING
		LOGGER.log(Level.INFO, "filteredFeatures: {0}, propertyList: {1}", new Object[]{ filteredFeatures.size(), propertyList.size() });
		String valueString = parseToJsonString(filteredFeatures, propertyList);
		LOGGER.log(Level.INFO, "features to return: {0}", valueString);
		features.put("features", valueString);
		
		return features;
	}
	
	/**
	 * Gets 2 Lists with properties and turns them into a single json string.
	 * @param filteredFeatures the smallest array
	 * @param propertyList the longest array
	 * @return
	 */
	private String parseToJsonString(List<String> filteredFeatures, List<String> propertyList) {
		int index = 0;
		int resultId = 1;
		int numIters = propertyList.size() / filteredFeatures.size();
		StringBuilder sb = new StringBuilder();
		
		sb.append("[");
		for(int i = 0; i < numIters; i++) {
			sb.append("{");
			sb.append("\"id\":\"" + resultId++ + "\",");
			sb.append("\"properties\":" + "[");
			for(int j = 0; j < filteredFeatures.size() - 1; j++) {
				sb.append("{\"" + filteredFeatures.get(j) + "\":\"" + propertyList.get(index++) + "\"},");
			}
			sb.append("{\"" + filteredFeatures.get(filteredFeatures.size() - 1) + "\":\"" + propertyList.get(index++) + "\"}]},");
		}
		sb.replace(sb.length() - 1, sb.length(), "]");
		LOGGER.log(Level.INFO, "JSON value String: {0}", sb.toString());
		return sb.toString();
	}
	
	/**
	 * Returns a KwetsbaarObject[] for the given filter, an empty list if no KwetsbaarObject is found.
	 * @throws Exception
	 */
	public List<KwetsbaarObject> getKwetsbareObjecten() {
		LOGGER.log(Level.INFO, "Getting kwetsbare objecten...");
		List<KwetsbaarObject> kwetsbaarObjList = new ArrayList<>();
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new ByteArrayInputStream(getResult().getBytes())));
			LOGGER.log(Level.INFO, "Document: \n {0}", getResult());
			Element docElement = doc.getDocumentElement();
			int numResults = Integer.parseInt(docElement.getAttribute("numberOfFeatures"));
			NodeList elementList = doc.getElementsByTagName("wfs:member");
			LOGGER.log(Level.INFO, "Aantal objecten found in numResults: {0}", numResults);
			LOGGER.log(Level.INFO, "Aantal objecten found in elementList: {0}" + elementList.getLength());
			if(elementList.getLength() == 0) {
				return new ArrayList<>();
			}
			
			for(int i = 0; i < elementList.getLength(); i++) {
				Element element = (Element)elementList.item(i);
				NodeList childList = element.getElementsByTagName("*");
				KwetsbaarObject obj = new KwetsbaarObject();
				obj.setAttributes(childList);
				kwetsbaarObjList.add(obj);
			}
		} catch (ParserConfigurationException | SAXException | IOException e) {
			LOGGER.log(Level.SEVERE, e.toString(), e);
		}
		
		LOGGER.log(Level.INFO, "Kwetsbare objecten in list: {0}", kwetsbaarObjList.size());
		return kwetsbaarObjList;
	}
}
