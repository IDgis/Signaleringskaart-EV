package nl.prv.veiligheidstoets;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamException;

import org.deegree.cs.exceptions.TransformationException;
import org.deegree.cs.exceptions.UnknownCRSException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import nl.prv.veiligheidstoets.util.LogStream;
import nl.prv.veiligheidstoets.util.TemplateHandler;
import nl.prv.veiligheidstoets.util.WKT2GMLParser;


/**
 * Servlet that handles spatial requests for veiligheidtoets
 *  
 * @author Linda Vels
 * @version 1.0 16-09-2013
 */

@WebServlet(urlPatterns = {"/toets"})
public class VeiligheidtoetsServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private String basisnetWFSUrl;
	private String risicokaartWFSUrl;
	private String wktError;
	
	private String veiligheidstoetsWFSUrl;
	private TemplateHandler filterHandler;
	
	/**
	 * initializes the servlet
	 * reads configuration
	 * 
	 */
	@Override
	public void init() {
		System.out.println("init servlet...");
		loadConfig();
	}


	/**
	 * Parses the configuration from the Veiligheidstoets.xml 
	 * 
	 */
	private void loadConfig(){
		
		try{
			String configdir = "/etc/veiligheidstoets";
			File configfile= new File(configdir + File.separator + "veiligheidstoets.xml");
			if(configfile.exists()){
				DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
				FileInputStream fis = new FileInputStream(configfile);
				Document configDoc = builder.parse(fis);
				basisnetWFSUrl = getConfigProperty(configDoc, "basisnetWFSUrl");
				risicokaartWFSUrl = getConfigProperty(configDoc,"risicokaartWFSUrl");
				veiligheidstoetsWFSUrl  = getConfigProperty(configDoc,"veiligheidstoetsWFSUrl");
				wktError = getConfigProperty(configDoc, "wktError");
				filterHandler = new TemplateHandler();
				fis.close();
			} 
			else {
				System.out.println("Config file missing " + configdir + File.separator + "veiligheidstoets.xml");
			}
		}
		catch(Exception e) {	
			System.out.println(e);
		}
	}
	
	/** 
	 * handles the post requests from the veiligheidstoets client
	 * 
	 */
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		LogStream logStream = new LogStream(new ByteArrayOutputStream());
		Map<String, String> returnMessage = new HashMap<>();
		
		try(PrintWriter out = response.getWriter()) {
			
			response.setContentType("application/json");
			
			Map <String, String[]> params = request.getParameterMap();
			Map <String,String> props = new HashMap<>();
			Iterator<Entry<String, String[]>> it  = params.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry <String,String[]> pairs = it.next();
				props.put(pairs.getKey(), pairs.getValue()[0]);
			}
			
			// Check request type
			if(props.containsKey("requesttype")) {
				if(props.get("requesttype").equals("polygonIsValid")) {
					// Check wktIsValid
					returnMessage = checkWktValid(props, "\"" + wktError + "\"");
				}
				else if(props.get("requesttype").equals("getEVFeatures")) {
					// getEVFeatures
					returnMessage = getEVFeatures(props);
				}
				else {
					returnMessage.put("error", "\"Request type is invalid: " + props.get("requesttype") + "\"");
				}
			}
			else {
				returnMessage.put("error", "\"Request type is missing!\"");
			}
			
			JsonObject json = new JsonObject();
			JsonParser parser = new JsonParser();
			
			Iterator<String> iter = returnMessage.keySet().iterator();
			while(iter.hasNext()) {
				String key = iter.next();
				String value = returnMessage.get(key);
				json.add(key, parser.parse(value));
			}
			
			System.out.println(json);
			out.println(json);
			out.flush();
		}			
		catch(IOException e) {
			logStream.write(e.toString());
			throw new ServletException(e);
		}
	}
	
	/**
	 * Checks if the client entered a valid shape. Returns a Map with the key isValid with
	 * the value true/false.
	 * @param props
	 * @param wktError
	 * @return
	 */
	private Map<String, String> checkWktValid(Map<String, String> props, String wktError) {
		Map<String, String> checkResult = new HashMap<>();
		if(props.containsKey("wkt")) {
			String wktGeom = props.get("wkt");
			WKTReader reader = new WKTReader();
			try {
				Polygon poly = (Polygon) reader.read(wktGeom);
				if(poly != null && !(poly.isValid())) {
					checkResult.put("isValid", "false");
					checkResult.put("error", wktError);
					return checkResult;
				}
				else {
					checkResult.put("isValid", "true");
					return checkResult;
				}
			} catch (ParseException e) {
				checkResult.put("isValid", "false");
				checkResult.put("error", "\"an error occurred: " + e.getMessage() + "\"");
			}
		}
		checkResult.put("error", "\"Wkt is missing!\"");
		return checkResult;
	}
	
	/**
	 * Gets the number of features or the values for the given properties in
	 * named in the postbody.
	 * @param props
	 * @throws Exception 
	 */
	private Map<String, String> getEVFeatures(Map<String, String> props) {
		Map<String, String> features = new HashMap<>();
		try {
			// Check servicename
			String url = getServiceName(props, 0);
			if(url == null) {
				features.put("error", "\"Servicename is missing!\"");
				return features;
			}
			else if(url.equals("INVALID")) {
				features.put("error", "\"Servicename is invalid: " + props.get("servicename") + "\"");
				return features;
			}
			
			// Check plangebied-wkt
			boolean plangebiedPresent = getPlangebiedWkt(props);
			if(!plangebiedPresent) {
				features.put("error", "\"Plangebied Wkt is missing!\"");
				return features;
			}
			
			// Check filter
			String[] filters = props.get("filter").split("x");
			if(filters == null) {
				features.put("error", "\"Filter is missing!\"");
				return features;
			}
			String filter = filterHandler.getFilter(filters[0], props);
			System.out.println("FILTER: \n" + filter);
			SpatialQuery sq = new SpatialQuery(url, filter);
			
			// Check for second filter
			if(filters.length > 1) {
				System.out.println("Second filter found...");
				List<KwetsbaarObject> kwObjects = sq.getKwetsbareObjecten();
				System.out.println("Number of kwetsbare objecten returned: " + kwObjects.size());
				url = getServiceName(props, 1);
				List<KwetsbaarObject> kwObjectsInBuffer = createSecondFilter(kwObjects, url, props);
				return sq.getPropertyResult(kwObjectsInBuffer);
			}
			
			// Check properties
			System.out.println("Getting features...");
			features = sq.getPropertyResult();
		}
		catch(Exception e) {
			features.put("error", "\"" + e.getMessage() + "\"");
			return features;
		}
		return features;
	}
	
	/**
	 * 
	 * @param props
	 * @param index the index of the servicename if more are given. 0 for the default servicename,
	 * 1 for the second filter if present.
	 * @return The url for the given servicename, INVALID if the servicename is invalid.
	 */
	private String getServiceName(Map<String, String> props, int index) {
		if(props.containsKey("servicename")){
			String[] urls = props.get("servicename").split("x");
			if(urls[index].equals("risicokaartWFS")) {
				return risicokaartWFSUrl;
			} 
			else if(urls[index].equals("veiligheidstoetsWFS")) {
				return veiligheidstoetsWFSUrl;
			}
			else if(urls[index].equals("basisnetWFS")) {
				return basisnetWFSUrl;
			}
			return "INVALID";
		}
		return null;
	}
	
	/**
	 * Adds the gml to the props if plangebiedWkt is present
	 * @param props
	 * @return false if an error occurred. True otherwise.
	 * @throws Exception 
	 */
	private boolean getPlangebiedWkt(Map<String, String> props) throws Exception {
		if(props.containsKey("plangebiedWkt")){
			String wktGeom = props.get("plangebiedWkt");
			
			String gml = null;
			try {
				gml = WKT2GMLParser.parse(wktGeom);
			} 
			catch (ParseException | XMLStreamException | UnknownCRSException | TransformationException e) {
				throw new Exception(e);
			} 
			props.put("plangebiedgml", gml);
			return true;
		}
		return false;
	}
	
	/**
	 * Returns a List of Kwetsbare Objecten that are inside the buffer.
	 * @param kwObjects
	 * @param url
	 * @param props
	 */
	private List<KwetsbaarObject> createSecondFilter(List<KwetsbaarObject> kwObjects, String url, Map<String, String> props) throws IOException {
		if(kwObjects.isEmpty()) {
			return new ArrayList<>();
		}
		System.out.println("Kwetsbare objecten found for secondary filter: " + kwObjects.size());
		List<KwetsbaarObject> kwObjectsInBuffer = new ArrayList<>();
		for(int i = 0; i < kwObjects.size(); i++) {
			String pointGeom = kwObjects.get(i).getPoint().toString();
			String gml = null;
			try {
				gml = WKT2GMLParser.parse(pointGeom);
			} 
			catch (ParseException | XMLStreamException | UnknownCRSException | TransformationException e) {
				throw new IOException(e);
			} 
			props.put("plangebiedgml", gml);
			
			String[] filters = props.get("filter").split("x");
			String filter = filterHandler.getFilter(filters[1], props);
			System.out.println("FILTER_2: \n" + filter);
			
			try {
				SpatialQuery sq2 = new SpatialQuery(url, filter);
				KwetsbaarObject bufferResult = sq2.getKwetsbaarObjectInBuffer(kwObjects.get(i));
				if(bufferResult != null) {
					kwObjectsInBuffer.add(bufferResult);
				}
			}
			catch(Exception e) {
				throw new IOException(e);
			}
		}
		return kwObjectsInBuffer;
	}

	/**
	 * parses the value of a property with a given name from the configuration Document
	 * 
	 * @param doc 
	 * @param propName
	 * @return the value of a given property
	 */
	private String getConfigProperty(Document doc, String propName){
		Element root = doc.getDocumentElement();
		return root.getAttribute(propName);
	}
}
