package nl.prv.veiligheidstoets.request;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import nl.prv.veiligheidstoets.SpatialQuery;
import nl.prv.veiligheidstoets.util.GMLParser;
import nl.prv.veiligheidstoets.util.TemplateHandler;

public class EVFeaturesRequest extends VeiligheidtoetsRequest {
	
	private String filter;
	private String url;
	private TemplateHandler templateHandler;
	
	public EVFeaturesRequest(Map<String, String> props) {
		super(props);
		logger = Logger.getLogger(EVFeaturesRequest.class.getName());
		logger.setLevel(Level.ALL);
		templateHandler = new TemplateHandler();
	}
	
	@Override
	public String setupProperties() {
		String result = super.setupProperties();
		if(result != null) {
			return result;
		}
		
		if(!props.containsKey("servicename")) {
			return "Service name is missing!";
		}
		String servicename = props.get("servicename");
		url = getConfigProperty(servicename + "Url");
		
		if(url == null || "".equals(url)) {
			logger.log(Level.WARN, "Service name is invalid: " + servicename);
			return "Service name is invalid: " + servicename;
		}
		
		if(!props.containsKey("filter")) {
			return "Filter is missing!";
		}
		filter = props.get("filter");
		
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
		
		String template = templateHandler.getFilter(filter, props);
		logger.log(Level.DEBUG, "TEMPLATE:\n" + template);
		logger.log(Level.DEBUG, "URL: " + url);
		SpatialQuery sq = new SpatialQuery(url, template);
		logger.log(Level.INFO, "Getting features...");
		RequestProcessor rp = new RequestProcessor();
		return rp.processFeatureResult(sq.getFilterResult());
	}
}
