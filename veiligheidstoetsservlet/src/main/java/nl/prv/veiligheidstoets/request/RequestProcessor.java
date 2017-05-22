package nl.prv.veiligheidstoets.request;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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

public class RequestProcessor {
	
	private Logger logger = Logger.getLogger(RequestProcessor.class.getName());
	
	public RequestProcessor() {
		logger.setLevel(Level.ALL);
	}
	
	/**
	 * Gets the xml string with the filtered results and returns the properties given in the template with its values.
	 * @param featureResult - The xml string
	 * @return
	 */
	public Map<String, String> processFeatureResult(String featureResult) {
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
			}
			
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
			logger.log(Level.FATAL, e.getMessage(), e);
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
	 * Gets all filtered property names and features and creates one value String from it.
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
}
