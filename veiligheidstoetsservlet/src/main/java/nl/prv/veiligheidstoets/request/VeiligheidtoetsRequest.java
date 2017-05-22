package nl.prv.veiligheidstoets.request;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public abstract class VeiligheidtoetsRequest {

	protected Document configDoc;
	protected String plangebiedWkt;
	protected Map<String, String> props;
	protected Logger logger;
	
	private static final String WKT = "plangebiedWkt";
	protected static final String ERROR = "error";
	
	public VeiligheidtoetsRequest(Map<String, String> props) {
		this.props = props;
	}
	
	/**
	 * Sets up all properties needed by the request and checks if all information is present
	 * @return An error message if something went wrong, null otherwise
	 */
	public String setupProperties() {
		String configdir = "/etc/veiligheidstoets";
		File configFile= new File(configdir + File.separator + "veiligheidstoets.xml");
		if(configFile.exists()) {
			try {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				FileInputStream fis = new FileInputStream(configFile);
				configDoc = builder.parse(fis);
			} catch (ParserConfigurationException | SAXException | IOException e) {
				logger.log(Level.FATAL, e.getMessage(), e);
				return e.getMessage();
			}
			
		}
		else {
			logger.log(Level.FATAL, String.format("Config file missing %s%sveiligheidstoets.xml", configdir, File.separator));
			return String.format("Config file missing %s%sveiligheidstoets.xml", configdir, File.separator);
		}
		
		if(!(props.containsKey(WKT) || props.containsKey("wkt"))) {
			return "Plangebied-wkt is missing!";
		}
		if(props.containsKey(WKT)) {
			plangebiedWkt = props.get(WKT);
		}
		else {
			plangebiedWkt = props.get("wkt");
		}
		
		return null;
	}
	
	/**
	 * Returns an response for a given request type. Each type generates its own response.
	 * @return
	 */
	public abstract Map<String, String> getResponse();
	
	/**
	 * Gets the property by the given property name from a config xml file
	 * @param propName
	 * @return
	 */
	protected String getConfigProperty(String propName){
		Element root = configDoc.getDocumentElement();
		return root.getAttribute(propName);
	}
}
