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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;


public class SpatialQuery {
	private static final Logger LOGGER = Logger.getLogger(SpatialQuery.class.getName());
	static {
		LOGGER.setLevel(Level.ALL);
	}
	
	private String urlstr;
	private String template;

	public SpatialQuery(String urlstr, String template) {
		this.urlstr = urlstr;
		this.template = template;
	}

	/**
	 * 
	 * @return Gets an xml with the data given in the postbody
	 */
	private String getResult() {
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
		
		return response.toString();
	}
	
	/**
	 * Returns the number of properties found or a json of the features with the properties specified in the filter.
	 * @return A Map with the result of the found features
	 */
	public Map<String, String> getFeatureResult() {
		Map<String, String> features = new HashMap<>();
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new ByteArrayInputStream(getResult().getBytes())));
			
			Document templateDoc = builder.parse(new InputSource(new ByteArrayInputStream(template.getBytes())));
			String resultType = templateDoc.getDocumentElement().getAttribute("resultType");
			if("hits".equals(resultType)) {
				return getNumFeatures(doc);
			}
			
			// Getting properties to filter
			List<String> properties = new ArrayList<>();
			NodeList queryList = templateDoc.getElementsByTagName("wfs:Query");
			Element queryElement = (Element)queryList.item(0);
			Node childNode = queryElement.getFirstChild().getNextSibling();
			while("ogc:PropertyName".equals(childNode.getNodeName())) {
				properties.add(childNode.getTextContent());
				childNode = childNode.getNextSibling().getNextSibling();
			}
			
			NodeList featureList = doc.getElementsByTagName("*");
			features = getFeatures(properties, featureList);
		} 
		catch (IOException | ParserConfigurationException | SAXException e) {
			LOGGER.log(Level.FATAL, e.toString(), e);
			features.put("error", "\"" + e.getMessage() + "\"");
			return features;
		}
		return features;
	}
	
	/**
	 * Returns the number of features found in the given document
	 * @param doc - the xml document created by the template
	 * @return The number of features found
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
	 * @param properties - The name of properties to filter e.g. PR10-6, PR10-7, PAG
	 * @param elementList - All xml tags in the given filter
	 * @return A Map with all features found as a json string
	 */
	private Map<String, String> getFeatures(List<String> properties, NodeList elementList) {
		Map<String, String> features = new HashMap<>();
		List<String> featureList = new ArrayList<>();
		
		// Loop through all elements in the document
		for(int i = 0; i < elementList.getLength(); i++) {
			Node featureMemberNode = elementList.item(i);
			//Loop through the filters and add matched elements
			fillPropertyList(featureList, properties, featureMemberNode);
		}
		if(featureList.isEmpty()) {
			features.put("message", "\"NO_FEATURES_FOUND\"");
			return features;
		}
		
		// DEBUGGING
		LOGGER.log(Level.DEBUG, String.format("properties: %d, featureList: %d", properties.size(), featureList.size()));
		String valueString = mergeAsJsonString(properties, featureList);
		LOGGER.log(Level.DEBUG, "features to return: " + valueString);
		features.put("features", valueString);
		
		return features;
	}
	
	/**
	 * Fills the propertyList with matching elements
	 * @param propertyList - the List to fill with properties
	 * @param properties - the property names to check to match
	 * @param featureMemberNode - the current element
	 */
	private void fillPropertyList(List<String> propertyList, List<String> properties, Node featureMemberNode) {
		for(int i = 0; i < properties.size(); i++) {
			String propertyName = properties.get(i);
			String nodeName = featureMemberNode.getNodeName();
			int nameStart = nodeName.indexOf(':');
			nodeName = nodeName.substring(nameStart, nodeName.length());
			if(nodeName.equalsIgnoreCase(":" + propertyName)) {
				String textContent = featureMemberNode.getTextContent();
				if(textContent == null || "".equals(textContent)) {
					properties.remove(i);
					continue;
				}
				propertyList.add(textContent);
			}
		}
	}
	
	/**
	 * Gets 2 Lists with properties and combines them into a single json string.
	 * @param propertyList the property names found in the template
	 * @param featureList all features found for the names by the given propertyList
	 * @return A json string from the combined lists
	 */
	private String mergeAsJsonString(List<String> propertyList, List<String> featureList) {
		int index = 0;
		int resultId = 1;
		int numIters = featureList.size() / propertyList.size();
		StringBuilder sb = new StringBuilder();
		
		sb.append("[");
		for(int i = 0; i < numIters; i++) {
			sb.append("{");
			sb.append("\"id\":\"" + resultId++ + "\",");
			sb.append("\"properties\":" + "[");
			for(int j = 0; j < propertyList.size() - 1; j++) {
				sb.append("{\"" + propertyList.get(j) + "\":\"" + featureList.get(index++) + "\"},");
			}
			sb.append("{\"" + propertyList.get(propertyList.size() - 1) + "\":\"" + featureList.get(index++) + "\"}]},");
		}
		sb.replace(sb.length() - 1, sb.length(), "]");
		LOGGER.log(Level.DEBUG, "JSON value String: " + sb.toString());
		return sb.toString();
	}
	
	/**
	 * Creates a MultiPoint Geometry of the area where the plangebiedWkt and the risicovolle gebieden are overlapping
	 * @param plangebiedWkt - The plangebiedWkt entered by the client
	 * @return
	 * @throws ParseException 
	 */
	public Geometry getRisicogebiedGeom(String plangebiedWkt) {
		Geometry finalGeometry = null;
		try {
			LOGGER.log(Level.INFO, "Creating MultiPolygon Geometry for Kwetsbare Objecten...");
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new ByteArrayInputStream(getResult().getBytes())));
		
		
			// Get the Geometry for the plangebiedWkt	
			WKTReader wktReader = new WKTReader();
			Geometry plangebiedWktGeom = wktReader.read(plangebiedWkt);
			WKTWriter writer = new WKTWriter();
			LOGGER.log(Level.DEBUG, "plangebiedWKT: " + writer.write(plangebiedWktGeom));
			// Get all risicogebieden found and parse the geometry in it
			NodeList memberList = doc.getElementsByTagName("wfs:member");
			LOGGER.log(Level.DEBUG, "Members found: " + memberList.getLength());
			if(memberList.getLength() == 0) {
				return null;
			}
			Geometry risicogebiedGeometry = null;
			for(int i = 0; i < memberList.getLength(); i++) {
				Element memberElement = (Element)memberList.item(i);
				NodeList posList = memberElement.getElementsByTagName("gml:posList");
				// Create MultiPolygon from all Polygons found in the document
				Geometry combinedGeom = getCombinedGeometry(posList);
				if(risicogebiedGeometry == null) {
					risicogebiedGeometry = combinedGeom;
				}
				else {
					risicogebiedGeometry = risicogebiedGeometry.union(combinedGeom);
				}
			}
			
			LOGGER.log(Level.DEBUG, "risicogebiedWKT: " + writer.write(risicogebiedGeometry));
			// Check if part of the risicogebied intersects with the wktGeom and add the overlapping part to geometry as MultiPolygon
			
			if(plangebiedWktGeom != null && risicogebiedGeometry != null && risicogebiedGeometry.intersects(plangebiedWktGeom)) {
				finalGeometry = risicogebiedGeometry.intersection(plangebiedWktGeom);
			}
			
			LOGGER.log(Level.DEBUG, "finalWKT: " + writer.write(finalGeometry));
		}
		catch(ParseException | SAXException | IOException | ParserConfigurationException e) {
			LOGGER.log(Level.FATAL, e.getMessage(), e);
		}
		return finalGeometry;
	}
	
	/**
	 * Creates a MultiPolygon Geometry of all coordinates within the member node of the NodeList
	 * @param posList
	 * @return
	 */
	private Geometry getCombinedGeometry(NodeList posList) {
		String[] firstCoords = posList.item(0).getTextContent().split(" ");
		StringBuilder builder = new StringBuilder("POLYGON((" + firstCoords[0] + " " + firstCoords[1]);
		for(int i = 0; i < posList.getLength(); i++) {
			String[] coords = posList.item(i).getTextContent().split(" ");
			for(int j = 2; j < coords.length; j+=2) {
				builder.append("," + coords[j] + " " + coords[j+1]);
			}
		}
		builder.append("))");
		WKTReader reader = new WKTReader();
		try {
			return reader.read(builder.toString());
		} catch (ParseException e) {
			LOGGER.log(Level.FATAL, e.getMessage(), e);
		}
		return null;
	}
	
	/**
	 * Returns the features for the Kwetsbare Objecten found or a message with no features
	 * @return
	 */
	public Map<String, String> getKOFeatures() {
		Map<String, String> features = new HashMap<>();
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new ByteArrayInputStream(getResult().getBytes())));
			
			// Get property names to filter from the template
			Document templateDoc = builder.parse(new InputSource(new ByteArrayInputStream(template.getBytes())));
			List<String> propertyNames = new ArrayList<>();
			NodeList queryList = templateDoc.getElementsByTagName("wfs:Query");
			Element queryElement = (Element)queryList.item(0);
			Node childNode = queryElement.getFirstChild().getNextSibling();
			while("ogc:PropertyName".equals(childNode.getNodeName())) {
				propertyNames.add(childNode.getTextContent());
				childNode = childNode.getNextSibling().getNextSibling();
			}
			
			// Get all Kwetsbare Objecten found by the second template
			NodeList memberList = doc.getElementsByTagName("wfs:member");
			if(memberList.getLength() == 0) {
				features.put("message", "\"NO_FEATURES_FOUND\"");
				return features;
			}
			
			// Get the features for the given properties
			List<String> featureList = new ArrayList<>();
			for(int i = 0; i < memberList.getLength(); i++) {
				Element tagElement = (Element)memberList.item(i);
				NodeList tagList = tagElement.getElementsByTagName("*");
				setKOProperties(tagList, propertyNames, featureList);
			}
			
			String resultString = mergeAsJsonString(propertyNames, featureList);
			features.put("features", resultString);
		} catch (ParserConfigurationException | SAXException | IOException e) {
			LOGGER.log(Level.FATAL, e.getMessage(), e);
		}
		
		return features;
	}
	
	/**
	 * Puts all found features in the featureList
	 * @param nodeList - the MemberList with all features found
	 * @param propertyNames - The propertyNames to filter
	 * @param featureList - the List with all filtered features
	 */
	private void setKOProperties(NodeList nodeList, List<String> propertyNames, List<String> featureList) {
		// Go through all tags in the nodeList
		for(int i = 0; i < nodeList.getLength(); i++) {
			Element node = (Element)nodeList.item(i);
			// Go through the propertyNames if they exist and add them to the map
			for(int j = 0; j < propertyNames.size(); j++) {
				String propertyName = propertyNames.get(j);
				String nodeName = node.getNodeName();
				int nameStart = nodeName.indexOf(':');
				nodeName = nodeName.substring(nameStart, nodeName.length());
				if(nodeName.equalsIgnoreCase(":" + propertyName)) {
					featureList.add(node.getTextContent());
				}
			}
		}
	}
}
