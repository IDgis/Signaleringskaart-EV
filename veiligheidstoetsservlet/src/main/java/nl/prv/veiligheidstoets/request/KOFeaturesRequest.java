package nl.prv.veiligheidstoets.request;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.deegree.geometry.Geometry;

import nl.prv.veiligheidstoets.SpatialQuery;
import nl.prv.veiligheidstoets.util.GMLParser;
import nl.prv.veiligheidstoets.util.TemplateHandler;

public class KOFeaturesRequest extends VeiligheidtoetsRequest {
	
	private String filterEv;
	private String filterKo;
	private String urlEv;
	private String urlKo;
	private TemplateHandler templateHandler;
	
	protected KOFeaturesRequest(Map<String, String> props) {
		super(props);
		logger = Logger.getLogger(EVFeaturesRequest.class.getName());
		logger.setLevel(Level.ALL);
		templateHandler = new TemplateHandler();
	}
	
	@Override
	public String initProperties() {
		String result = super.initProperties();
		if(result != null) {
			return result;
		}
		
		if(!props.containsKey("servicenameEv")) {
			return "Service name EV is missing!";
		}
		if(!props.containsKey("servicenameKo")) {
			return "Service name KO is missing!";
		}
		String servicenameEv = props.get("servicenameEv");
		String servicenameKo = props.get("servicenameKo");
		urlEv = getConfigProperty(servicenameEv + "Url");
		urlKo = getConfigProperty(servicenameKo + "Url");
		if(urlEv == null || "".equals(urlEv)) {
			logger.log(Level.WARN, "Service name EV is invalid: " + servicenameEv);
			return "Service name EV is invalid!";
		}
		if(urlKo == null || "".equals(urlKo)) {
			logger.log(Level.WARN, "Service name KO is invalid: " + servicenameKo);
			return "Service name KO is invalid!";
		}
		
		if(!props.containsKey("filterEv")) {
			return "Filter EV is missing!";
		}
		if(!props.containsKey("filterKo")) {
			return "Filter KO is missing!";
		}
		filterEv = props.get("filterEv");
		filterKo = props.get("filterKo");
		
		return null;
	}
	
	public Map<String, String> getResponse() {
		Map<String, String> features = new HashMap<>();
		
		try {
			String gml = GMLParser.parseFromWKT(plangebiedWkt);
			props.put("plangebiedgml", gml);
		} catch (IOException e) {
			logger.log(Level.FATAL, e.getMessage(), e);
			features.put(ERROR, "\"" + e.getMessage() + "\"");
			return features;
		}
		
		String templateEv = templateHandler.getFilter(filterEv, props);
		logger.log(Level.DEBUG, "TEMPLATE EV:\n" + templateEv);
		logger.log(Level.DEBUG, "URL EV: " + urlEv);
		
		// Get MultiPolygon Geometry from first template
		SpatialQuery sq = new SpatialQuery(urlEv, templateEv);
		RequestProcessor rp = new RequestProcessor();
		Geometry geometry = rp.getRisicogebiedGeom(sq, plangebiedWkt);
		if(geometry == null) {
			features.put("message", "\"NO_FEATURES_FOUND\"");
			return features;
		}
		
		try {
			String gml = GMLParser.parseFromGeometry(geometry);
			props.put("plangebiedgml", gml);
		} catch (IOException e) {
			logger.log(Level.FATAL, e.getMessage(), e);
			features.put(ERROR, "\"" + e.getMessage() + "\"");
			return features;
		}
		
		// Get Kwetsbare Objecten within the MultiPoint object
		String templateKo = templateHandler.getFilter(filterKo, props);
		logger.log(Level.DEBUG, "TEMPLATE KO:\n" + templateKo);
		logger.log(Level.DEBUG, "URL KO:\n" + urlKo);
		sq = new SpatialQuery(urlKo, templateKo);
		logger.log(Level.INFO, "Getting features...");
		return rp.getFeatureResult(sq.getFeatureResult());
	}
}
