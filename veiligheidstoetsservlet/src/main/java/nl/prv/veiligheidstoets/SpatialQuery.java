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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


public class SpatialQuery {
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
	 * Returns the number of properties found or a json of the features with the properties specified in the filter.
	 * @return
	 * @throws Exception
	 */
	public Map<String, String> getPropertyResult() throws Exception {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new ByteArrayInputStream(getResult().getBytes())));
			
			Document filterDoc = builder.parse(new InputSource(new ByteArrayInputStream(filter.getBytes())));
			String resultType = filterDoc.getDocumentElement().getAttribute("resultType");
			if(resultType.equals("hits")) {
				return getNumFeatures(doc);
			}
			
			// Getting properties to filter
			List<String> filteredFeatures = new ArrayList<>();
			NodeList elementList = filterDoc.getElementsByTagName("ogc:PropertyName");
			for(int i = 0; i < elementList.getLength(); i++) {
				if(elementList.item(i).getTextContent().equals("the_geom") || elementList.item(i).getTextContent().equals("geometrie")) {
					continue;
				}
				System.out.println("FilteredFeatures: " + elementList.item(i).getTextContent());
				filteredFeatures.add(elementList.item(i).getTextContent());
			}
			
			NodeList featureList = doc.getElementsByTagName("*");
			return getProperties(filteredFeatures, featureList);
		} 
		catch (IOException | ParserConfigurationException | SAXException e) {
			throw new Exception(e);
		}
	}
	
	public Map<String, String> getNumFeatures(KwetsbaarObject kwObject, int index) throws Exception {
		Map<String, String> numFeatures = new HashMap<>();
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new ByteArrayInputStream(getResult().getBytes())));
			numFeatures = getNumFeatures(doc);
			if(numFeatures.size() == 1) {
				StringBuilder sb = new StringBuilder();
				sb.append("{\"name\":\"" + kwObject.getName() + "\",");
				sb.append("\"id\":\"" + kwObject.getId() + "\",");
				sb.append("\"location\":\"" + kwObject.getCoordX() + "," + kwObject.getCoordY() + "\"}");
				String result = sb.toString();
				numFeatures.put(Integer.toString(index), result);
			}
		}
		catch(IOException | ParserConfigurationException | SAXException e) {
			throw new Exception(e);
		}
		return numFeatures;
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
	 * @param filteredFeatures
	 * @param resultNames
	 * @param elementList
	 * @return
	 */
	private Map<String, String> getProperties(List<String> filteredFeatures, NodeList elementList) {
		Map<String, String> features = new HashMap<>();
		
		List<String> propertyList = new ArrayList<>();
		for(int i = 0; i < elementList.getLength(); i++) {
			Node featureMemberNode = elementList.item(i);
			Element featureMemberElement = (Element)featureMemberNode;
			NodeList featureMemberList = featureMemberElement.getElementsByTagName("*");
			for(int j = 0; j < featureMemberList.getLength(); j++) {
				Node featureMemberValue = featureMemberList.item(j);
				
				for(int k = 0; k < filteredFeatures.size(); k++) {
					if(filteredFeatures.get(k).equals("the_geom") || filteredFeatures.get(k).equals("geometrie")) {
						filteredFeatures.remove(k);
						continue;
					}
					if(featureMemberValue.getNodeName().endsWith(":" + filteredFeatures.get(k))) {
						propertyList.add(featureMemberValue.getTextContent());
					}
				}
			}
			
		}
		String valueString = parseToJsonString(filteredFeatures, propertyList);
		System.out.println("features to return: " + valueString);
		features.put("features", valueString);
		
		return features;
	}
	
	/**
	 * Gets 2 Lists with properties and turns them into a single json string.
	 * @param filterArray the smallest array
	 * @param propertyArray the longest array
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
		System.out.println("JSON value String: " + sb.toString());
		return sb.toString();
	}
	
	/**
	 * Returns a KwetsbaarObject[] for the given filter.
	 * @throws Exception
	 */
	public KwetsbaarObject[] getKwetsbareObjecten() throws Exception {
		List<KwetsbaarObject> kwetsbaarObjList = new ArrayList<>();
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new ByteArrayInputStream(getResult().getBytes())));
			NodeList elementList = doc.getElementsByTagName("gml:featureMember");
			if(elementList.getLength() == 0) {
				return new KwetsbaarObject[0];
			}
			
			for(int i = 0; i < elementList.getLength(); i++) {
				Node node = elementList.item(i);
				Element element = (Element)node;
				NodeList childList = element.getElementsByTagName("*");
				KwetsbaarObject obj = new KwetsbaarObject();
				obj.setAttributes(childList);
				kwetsbaarObjList.add(obj);
			}
		} catch (ParserConfigurationException | SAXException | IOException e) {
			throw new Exception(e);
		}
		
		return kwetsbaarObjList.toArray(new KwetsbaarObject[kwetsbaarObjList.size()]);
	}
}
