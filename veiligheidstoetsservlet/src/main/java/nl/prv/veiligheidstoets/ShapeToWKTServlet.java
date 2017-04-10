package nl.prv.veiligheidstoets;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.prv.veiligheidstoets.util.StreamToDocumentParser;
import nl.prv.veiligheidstoets.util.URLToFile;

import org.geotools.data.FeatureSource;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;


public class ShapeToWKTServlet extends HttpServlet {
	private String shapeLoadError;
	/**
	 * initializes the servlet
	 * reads configuration
	 * 
	 */
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
			String configdir = System.getProperty("VEILIGHEIDSTOETS_CONFIGDIR");
			System.out.println("configdir == "+ configdir); 
			File configfile= new File(configdir + File.separator + "veiligheidstoets.xml");
			if(configfile.exists()){
				FileInputStream fis = new FileInputStream(configfile);
				Document configDoc = StreamToDocumentParser.parse(fis);
				shapeLoadError = getConfigProperty(configDoc,"shapeLoadError");
				
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
		File shp = null;
		File shx = null;
		File dbf = null;
		try{	
			Boolean shpok = false;
			Boolean shxok = false;
			Boolean dbfok = false;
			shp = File.createTempFile ("shape", ".shp");
			String coveragename = shp.getAbsolutePath().substring(0, shp.getAbsolutePath().length()-4);
			shx = new File(coveragename + ".shx");
			dbf = new File(coveragename + ".dbf");
			Map <String,String[]> params = request.getParameterMap();
			Map <String,String> props = new HashMap<String,String>();
			Iterator it  = params.entrySet().iterator();
			 while (it.hasNext()) {
			        Map.Entry <String,String[]> pairs = (Map.Entry <String,String[]>)it.next();
			        props.put(pairs.getKey(), pairs.getValue()[0]);
			}
				
			if(props.containsKey("shpurl")){
				shpok = true;
				shp = URLToFile.fill(props.get("shpurl"), shp);		
			} 
			if(props.containsKey("shxurl")){
				shxok = true;
				shx = URLToFile.fill(props.get("shxurl"), shx);		
			} 
			if(props.containsKey("shxurl")){
				dbfok = true;
				dbf = URLToFile.fill(props.get("dbfurl"),dbf);		
			} 
			if(!shpok||!shxok||!dbfok){
				//fout melding, cleanup en stoppen;
				shp.delete();
				shx.delete();
				dbf.delete();
				response.setContentType("text/html");
                PrintWriter out = new PrintWriter(response.getOutputStream());
                out.println("<html><body>");
                out.println(shapeLoadError);
                out.println("</body></html>");
                out.flush();
    			out.close();	
    			return;
			}

            shp.toURI().toURL();
            ShapefileDataStore store = (ShapefileDataStore) FileDataStoreFinder.getDataStore(shp);
        
	        FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = store.getFeatureSource();
	        //get first feature, check the geometry and convert to WKT   
            SimpleFeatureCollection fCol = (SimpleFeatureCollection) featureSource.getFeatures();
			FeatureIterator<SimpleFeature> iterator=fCol.features();
			 
			int numFeatures = 0;
			String wktStr = "";
			try {
				WKTWriter writer = new WKTWriter(); 
            	while( iterator.hasNext()  ){
                	numFeatures++;
                	SimpleFeature feature = (SimpleFeature) iterator.next();
                     Geometry geom = (Geometry) feature.getDefaultGeometry();
                     //check geometry
                     if (geom != null && !(geom.isValid())) {
                         System.out.println("A geometry in the shape file is Invalid: " + feature.getID());
                     } else {
                    	 wktStr += writer.write(geom);
                     }
                     //System.out.println( "Geometry toegevoegd voor " + (feature).getID() + " resultaat WKT = " + wktStr);
                }
            }
            finally {
                iterator.close();
                if (numFeatures > 1){
                	System.out.println("The shape file contains more than 1 feature");
                }
                response.setContentType("text/xml");
                PrintWriter out = new PrintWriter(response.getOutputStream());
                out.println("<?xml version='1.0' encoding='UTF-8'?><wkt>" + wktStr + "</wkt>");
                //out.println("<html><body>");
                //out.println("<script>window.onload = function() {parent.shpFrameLoaded('" +  + "');}</script>");
                //out.println("<div id='wktresult' data-wkt='" + wktStr + "'/>");
                //out.println("</body></html>");
    			out.flush();
    			out.close();	
    			shp.delete();
    			shx.delete();
    			dbf.delete();
            }
	
		}
		catch(Exception e) {	
			System.out.println(e);
			System.out.println(e.getStackTrace().toString());
			//fout melding, cleanup;
			shp.delete();
			shx.delete();
			dbf.delete();
			//logger.error(e,e);
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
