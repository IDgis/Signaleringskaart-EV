package nl.prv.veiligheidstoets;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//import org.apache.log4j.Logger;
//import org.apache.log4j.xml.DOMConfigurator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import nl.prv.veiligheidstoets.util.LogStream;
import nl.prv.veiligheidstoets.util.StreamToDocumentParser;
import nl.prv.veiligheidstoets.util.TemplateHandler;
import nl.prv.veiligheidstoets.util.WKT2GMLParser;


/**
 * Servlet that handles spatial requests for veiligheidtoets
 *  
 * @author Linda Vels
 * @version 1.0 16-09-2013
 */


public class VeiligheidtoetsServlet extends HttpServlet {
	//private static Logger logger = Logger.getLogger("nl.prv.veiligheidstoets");
	private String risicokaartWMSUrl;
	private String risicokaartWFSUrl;
	private String risicokaartUserName="";
	private String risicokaartPassword="";
	
	private String veiligheidstoetsWFSUrl;
	private TemplateHandler filterHandler;
	
	
	/**
	 * initializes the servlet
	 * reads configuration
	 * 
	 */
	public void init() {
		System.out.println("init servlet");
		//URL url = Loader.getResource("log4j.xml");
		//DOMConfigurator.configure(url);
		loadConfig();
	}


	/**
	 * Parses the configuration from the Veiligheidstoets.xml 
	 * 
	 */
	public void loadConfig(){
		
		try{
			String configdir = "/etc/veiligheidstoets/veiligheidstoets.xml";
			System.out.println("configdir == "+ configdir); 
			File configfile= new File(configdir + File.separator + "veiligheidstoets.xml");
			if(configfile.exists()){
				FileInputStream fis = new FileInputStream(configfile);
				Document configDoc = StreamToDocumentParser.parse(fis);
				risicokaartWMSUrl = getConfigProperty(configDoc,"risicokaartWMSUrl");
				risicokaartWFSUrl = getConfigProperty(configDoc,"risicokaartWFSUrl");
				risicokaartUserName = getConfigProperty(configDoc,"risicokaartUserName");
				risicokaartPassword = getConfigProperty(configDoc,"risicokaartPassword");
				veiligheidstoetsWFSUrl  = getConfigProperty(configDoc,"veiligheidstoetsWFSUrl");
				filterHandler = new TemplateHandler();
			} else {
				System.out.println("Config file missing " + configdir + File.separator + "veiligheidstoets.xml");
			}
			
			
		}
		catch(Exception e) {	
			System.out.println(e);
			//logger.error(e,e);
		}
	}
	
	/** 
	 * handles the post requests from the veiligheidstoets client
	 * 
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response)  {
		LogStream logStream = new LogStream(new ByteArrayOutputStream());			
		try{
			Map <String,String[]> params = request.getParameterMap();
			Map <String,String> props = new HashMap<String,String>();
			Iterator it  = params.entrySet().iterator();
			 while (it.hasNext()) {
			        Map.Entry <String,String[]> pairs = (Map.Entry <String,String[]>)it.next();
			        props.put(pairs.getKey(), pairs.getValue()[0]);
			}
			String url = "";
			if(props.containsKey("servicename")){
				if(props.get("servicename").equals("risicokaartWFS")){
					url = risicokaartWFSUrl;
				} 
				if (props.get("servicename").equals("veiligheidstoetsWFS")){ 	
					url = veiligheidstoetsWFSUrl;
				}
			} else {
				logStream.write("\r servicename: " + props.get("servicename") + " is not known.");
			}
			if(props.containsKey("plangebied-wkt")){
				String wktGeom = props.get("plangebied-wkt");
				String gml = WKT2GMLParser.parse(wktGeom); 
				props.put("plangebiedgml", gml);
			}	
			String filter = "";
			String fn = props.get("filter");
			filter = filterHandler.getFilter(fn, props);
			SpatialQuery sq = new SpatialQuery(url,filter,logStream,risicokaartUserName,risicokaartPassword);
			response.setContentType("text/xml");
			PrintWriter out = new PrintWriter(response.getOutputStream());
			out.println(sq.getResult());
			out.flush();
			out.close();								
		}			
		catch(Exception e) {
			//logger.info(logStream.getStream().toString());
			//logger.error(e,e);
			System.out.println(logStream.getStream().toString());
			System.out.println(e);
		}
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
		String propValue = root.getAttribute(propName);
		return propValue;	
	}
}
