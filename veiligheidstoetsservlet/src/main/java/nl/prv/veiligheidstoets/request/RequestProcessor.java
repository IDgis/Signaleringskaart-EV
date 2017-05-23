package nl.prv.veiligheidstoets.request;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.deegree.commons.xml.XMLParsingException;
import org.deegree.cs.persistence.CRSManager;
import org.deegree.cs.refs.coordinatesystem.CRSRef;
import org.deegree.geometry.Geometry;
import org.deegree.geometry.io.WKTReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.vividsolutions.jts.io.ParseException;

import nl.prv.veiligheidstoets.SpatialQuery;
import nl.prv.veiligheidstoets.util.GMLParser;

public class RequestProcessor {
	
	private static final Logger LOGGER = Logger.getLogger(RequestProcessor.class.getName());
	
	public RequestProcessor() {
		LOGGER.setLevel(Level.ALL);
	}
	
	/**
	 * Gets the xml string with the filtered results and returns the properties given in the template with its values.
	 * @param featureResult - The xml string
	 * @return
	 */
	public Map<String, String> getFeatureResult(String featureResult) {
		Map<String, String> features = new HashMap<>();
		
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new ByteArrayInputStream(featureResult.getBytes())));
			
			// Look for number of features first
			Element root = doc.getDocumentElement();
			if(root.hasAttribute("numberOfFeatures")) {
				int result = Integer.parseInt(root.getAttribute("numberOfFeatures"));
				features.put("numberOfFeaturesFound", Integer.toString(result));
				return features;
			}
			
			// Get all properties returned
			NodeList memberList = doc.getElementsByTagName("wfs:member");
			if(memberList.getLength() == 0) {
				features.put("message", "\"NO_FEATURES_FOUND\"");
				return features;
			}
			
			// Features found
			StringBuilder valueString = new StringBuilder();
			valueString.append("[");
			for(int i = 0; i < memberList.getLength(); i++) {
				valueString.append("{\"id\":\"" + (i + 1) + "\",");
				valueString.append("\"properties\":[");
				Element memberElement = (Element)memberList.item(i);
				NodeList memberFeatures = memberElement.getElementsByTagName("*");
				valueString.append(processMemberFeatures(memberFeatures));
				valueString.append("]}");
				
				if(i < memberList.getLength() - 1) {
					valueString.append(",");
				}
			}
			valueString.append("]");
			
			features.put("features", valueString.toString());
		} catch (ParserConfigurationException | SAXException | IOException e) {
			LOGGER.log(Level.FATAL, e.getMessage(), e);
			features.put("error", "\"" + e.getMessage() + "\"");
		}
		return features;
	}
	
	/**
	 * Cycles through the member nodes in the parsed xml string and returns the result as a single String.
	 * @param memberFeatures - The member Node List
	 * @return
	 */
	private String processMemberFeatures(NodeList memberFeatures) {
		List<String> properties = new ArrayList<>();
		List<String> features = new ArrayList<>();
		String namespace = memberFeatures.item(0).getNodeName().substring(0, memberFeatures.item(0).getNodeName().indexOf(':'));
		
		boolean firstIteration = true;
		for(int i = 0; i < memberFeatures.getLength(); i++) {
			Node node = memberFeatures.item(i);
			String nodeName = node.getNodeName().substring(node.getNodeName().indexOf(':') + 1, node.getNodeName().length());
			if(firstIteration || node.getNodeName().equals(namespace + ":geom") || !node.getNodeName().startsWith(namespace)) {
				firstIteration = false;
				continue;
			}
			properties.add(nodeName);
			features.add(node.getTextContent());
			
		}
		return createValueString(properties, features);
	}
	
	/**
	 * Gets all filtered property names and features and creates one value String from it in Json notation.
	 * @param properties
	 * @param features
	 * @return
	 */
	private String createValueString(List<String> properties, List<String> features) {
		StringBuilder valueString = new StringBuilder();
		
		for(int i = 0; i < properties.size(); i++) {
			valueString.append("{");
			valueString.append("\"" + properties.get(i) + "\":\"" + features.get(i) + "\"");
			valueString.append("}");
			if(i < properties.size() - 1) {
				valueString.append(",");
			}
		}
		
		return valueString.toString();
	}
	
	/**
	 * Creates a MultiPoint Geometry of the area where the plangebiedWkt and the risicovolle gebieden are overlapping
	 * @param sq - The result of the GetFeature request
	 * @param plangebiedWkt - The plangebiedWkt entered by the client
	 * @return
	 * @throws ParseException 
	 */
	public Geometry getRisicogebiedGeom(SpatialQuery sq, String plangebiedWkt) {
		Geometry finalGeometry = null;
		try {
			LOGGER.log(Level.INFO, "Creating MultiPolygon Geometry for Kwetsbare Objecten...");
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new ByteArrayInputStream(sq.getFeatureResult().getBytes())));
		
		
			// Get the Geometry for the plangebiedWkt
			CRSRef crs = CRSManager.getCRSRef("EPSG:28992");
			WKTReader wktReader = new WKTReader(crs);
			Geometry plangebiedWktGeom = wktReader.read(plangebiedWkt);
			// Get all risicogebieden found and parse the geometry in it
			NodeList memberList = doc.getElementsByTagName("wfs:member");
			LOGGER.log(Level.DEBUG, "Members found: " + memberList.getLength());
			if(memberList.getLength() == 0) {
				return null;
			}
			Geometry risicogebiedGeometry = null;
			for(int i = 0; i < memberList.getLength(); i++) {
				Element memberElement = (Element)memberList.item(i);
				NodeList polygonList = memberElement.getElementsByTagName("gml:Polygon");
				// Create MultiPolygon from all Polygons found in the document
				Geometry combinedGeom = getGeometry(polygonList);
				if(risicogebiedGeometry == null) {
					risicogebiedGeometry = combinedGeom;
				}
				else {
					risicogebiedGeometry = risicogebiedGeometry.getUnion(combinedGeom);
				}
			}
			
			// Check if part of the risicogebied intersects with the wktGeom and add the overlapping part to geometry as MultiPolygon
			
			if(plangebiedWktGeom != null && risicogebiedGeometry != null && risicogebiedGeometry.intersects(plangebiedWktGeom)) {
				finalGeometry = risicogebiedGeometry.getIntersection(plangebiedWktGeom);
			}
		}
		catch(SAXException | IOException | ParserConfigurationException | ParseException e) {
			LOGGER.log(Level.FATAL, e.getMessage(), e);
		}
		return finalGeometry;
	}
	
	/**
	 * Creates a MultiPolygon Geometry of all coordinates within the member node of the NodeList
	 * @param polygonList
	 * @return
	 */
	private Geometry getGeometry(NodeList polygonList) {
		try {
			Node polygonNode = polygonList.item(0);
			((Element)polygonNode).setAttribute("xmlns:gml", "http://www.opengis.net/gml");
			
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer = tFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			
			StringWriter sw = new StringWriter();
			transformer.transform(new DOMSource(polygonNode), new StreamResult(sw));
			
			return GMLParser.parseToGeometry(sw.toString());
		}
		catch(TransformerException | XMLParsingException | IOException e) {
			LOGGER.log(Level.FATAL, e.getMessage(), e);
		}
		
		return null;
	}
}
