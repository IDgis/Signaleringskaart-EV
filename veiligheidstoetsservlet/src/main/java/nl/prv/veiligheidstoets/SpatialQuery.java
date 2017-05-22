package nl.prv.veiligheidstoets;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;

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

import nl.prv.veiligheidstoets.util.GMLParser;


public class SpatialQuery {
	private static final Logger LOGGER = Logger.getLogger(SpatialQuery.class.getName());
	static {
		LOGGER.setLevel(Level.ALL);
	}
	
	private String urlstr;
	private String template;
	private String filterResult;

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
		
		filterResult = response.toString();
	}
	
	/**
	 * Gets the filtered properties in a xml string
	 * @return
	 */
	public String getFilterResult() {
		return filterResult;
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
			Document doc = builder.parse(new InputSource(new ByteArrayInputStream(filterResult.getBytes())));
		
		
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
