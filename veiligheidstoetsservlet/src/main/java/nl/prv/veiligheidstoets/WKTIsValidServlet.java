package nl.prv.veiligheidstoets;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.w3c.dom.Document;
import org.w3c.dom.Element;



import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.geom.Geometry;

import nl.prv.veiligheidstoets.util.StreamToDocumentParser;

public class WKTIsValidServlet  extends HttpServlet {
	private String wktError;
	public void init() {
		System.out.println("init servlet ShapeToWKT");
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
				wktError = getConfigProperty(configDoc,"wktError");			
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
	public void doPost(HttpServletRequest request, HttpServletResponse response) {
		String gml;
		try{
			Map <String,String[]> params = request.getParameterMap();
			if (params.containsKey("wkt")){
				String wktGeom = params.get("wkt")[0];
				WKTReader reader = new WKTReader();
				Geometry geom = reader.read(wktGeom);
				if (geom != null && !(geom.isValid())) {
					//fout melding,stoppen;
					response.setContentType("text/xml");
	                PrintWriter out = new PrintWriter(response.getOutputStream());
	                out.println("<?xml version='1.0' encoding='UTF-8'?><wktValid><wktIsValid>false</wktIsValid><wktMessage>");
	                out.println(wktError);
	                out.println("</wktMessage></wktValid>");
	                out.flush();
	    			out.close();	
	    			return;
				}    
				else {
					response.setContentType("text/xml");
	                PrintWriter out = new PrintWriter(response.getOutputStream());
	                out.println("<?xml version='1.0' encoding='UTF-8'?><wktValid><wktIsValid>true</wktIsValid></wktValid>");
	    			out.flush();
	    			out.close();						
				}	
			}	
			else {
				System.out.println("The parameter plangebied-wkt is missing in the request");
			}
			
		}
		catch(Exception e) {
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


