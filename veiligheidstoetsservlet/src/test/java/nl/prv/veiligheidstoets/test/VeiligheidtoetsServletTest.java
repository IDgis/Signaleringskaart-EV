package nl.prv.veiligheidstoets.test;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.antlr.stringtemplate.StringTemplateGroup;
import org.junit.Ignore;
import org.junit.Test;

import nl.prv.veiligheidstoets.SpatialQuery;
import nl.prv.veiligheidstoets.request.RequestFactory;
import nl.prv.veiligheidstoets.request.VeiligheidtoetsRequest;
import nl.prv.veiligheidstoets.util.TemplateHandler;

@SuppressWarnings("unused")
public class VeiligheidtoetsServletTest {
	
	private final String templateURL = "C:/Users/Kevin/Signaleringskaart/git/docker/deegree/veiligheidstoets/templates";
	private final String configURL = "C:/Users/Kevin/Signaleringskaart/git/docker/deegree/veiligheidstoets";
	private final String invalidwktString = "POLYGON((208157.92 511433.0884375,204287.2 503476.6084375,210093.28 500466.0484375,204287.2 511433.0884375,208157.92 511433.0884375))";
	private final String validwktString = "POLYGON((208157.92 511433.0884375,204287.2 503476.6084375,210093.28 500466.0484375,213533.92 507132.2884375,213748.96 507132.2884375,212673.76 510142.8484375,212673.76 510142.8484375,211168.48 511648.1284375,208157.92 511433.0884375))";
	
	private static final String FILTER_EV = "filterEv";
	private static final String FILTER_KO = "filterKo";
	private static final String SERVICENAME_EV = "servicenameEv";
	private static final String SERVICENAME_KO = "servicenameKo";
	
	@Ignore
	public void testRequestTypeMissing() throws Exception {
		Map<String, String> props = new HashMap<>();
		
		VeiligheidtoetsRequest request = RequestFactory.createVeiligheidtoetsRequest(props);
		
		assertNull(request);
	}
	
	@Ignore
	public void testRequestTypeInvalid() throws Exception {
		Map<String, String> props = new HashMap<>();
		props.put("requesttype", "test");
		
		VeiligheidtoetsRequest request = RequestFactory.createVeiligheidtoetsRequest(props);
		
		assertNull(request);
	}
	
	@Ignore
	public void testWktIsMissing() throws Exception {
		Map<String, String> props = new HashMap<>();
		props.put("requesttype", "polygonIsValid");
		
		VeiligheidtoetsRequest request = RequestFactory.createVeiligheidtoetsRequest(props);
		
		Field configDir = request.getClass().getSuperclass().getDeclaredField("configDir");
		configDir.setAccessible(true);
		configDir.set(request, configURL);
		
		String result = request.initProperties();
		
		String expectedErrorMessage = "Plangebied-wkt is missing!";
		
		assertEquals(expectedErrorMessage, result);
	}
	
	@Ignore
	public void testWktInvalid() throws Exception {
		Map<String, String> props = new HashMap<>();
		props.put("requesttype", "polygonIsValid");
		props.put("wkt", invalidwktString);
		
		VeiligheidtoetsRequest request = RequestFactory.createVeiligheidtoetsRequest(props);
		
		Field configDir = request.getClass().getSuperclass().getDeclaredField("configDir");
		configDir.setAccessible(true);
		configDir.set(request, configURL);
		
		request.initProperties();
		String response = request.getResponse().get("isValid");
		
		assertEquals("false", response);
	}
	
	@Ignore
	public void testWktValid() throws Exception {
		Map<String, String> props = new HashMap<>();
		props.put("requesttype", "polygonIsValid");
		props.put("wkt", validwktString);
		
		VeiligheidtoetsRequest request = RequestFactory.createVeiligheidtoetsRequest(props);
		
		Field configDir = request.getClass().getSuperclass().getDeclaredField("configDir");
		configDir.setAccessible(true);
		configDir.set(request, configURL);
		
		request.initProperties();
		String response = request.getResponse().get("isValid");
		
		assertEquals("true", response);
	}
	
	@Ignore
	public void testServiceNameMissing() throws Exception {
		Map<String, String> props = new HashMap<>();
		props.put("requesttype", "getEVFeatures");
		props.put("plangebiedWkt", validwktString);
		
		VeiligheidtoetsRequest request = RequestFactory.createVeiligheidtoetsRequest(props);
		
		Field configDir = request.getClass().getSuperclass().getDeclaredField("configDir");
		configDir.setAccessible(true);
		configDir.set(request, configURL);
		
		String response = request.initProperties();
		String expected = "Service name is missing!";
		
		assertEquals(expected, response);
	}
	
	@Ignore
	public void testServiceNameEVMissing() throws Exception {
		Map<String, String> props = new HashMap<>();
		props.put("requesttype", "getKOFeatures");
		props.put("plangebiedWkt", validwktString);
		
		VeiligheidtoetsRequest request = RequestFactory.createVeiligheidtoetsRequest(props);
		
		Field configDir = request.getClass().getSuperclass().getDeclaredField("configDir");
		configDir.setAccessible(true);
		configDir.set(request, configURL);
		
		String response = request.initProperties();
		String expected = "Service name EV is missing!";
		
		assertEquals(expected, response);
	}
	
	@Ignore
	public void testServiceNameKOMissing() throws Exception {
		Map<String, String> props = new HashMap<>();
		props.put("requesttype", "getKOFeatures");
		props.put("plangebiedWkt", validwktString);
		props.put(SERVICENAME_EV, "risicokaartWFS");
		
		VeiligheidtoetsRequest request = RequestFactory.createVeiligheidtoetsRequest(props);
		
		Field configDir = request.getClass().getSuperclass().getDeclaredField("configDir");
		configDir.setAccessible(true);
		configDir.set(request, configURL);
		
		String response = request.initProperties();
		String expected = "Service name KO is missing!";
		
		assertEquals(expected, response);
	}
	
	@Ignore
	public void testServiceNameInvalid() throws Exception {
		Map<String, String> props = new HashMap<>();
		props.put("requesttype", "getEVFeatures");
		props.put("plangebiedWkt", validwktString);
		props.put("servicename", "risicokaartWKS");
		
		VeiligheidtoetsRequest request = RequestFactory.createVeiligheidtoetsRequest(props);
		
		Field configDir = request.getClass().getSuperclass().getDeclaredField("configDir");
		configDir.setAccessible(true);
		configDir.set(request, configURL);
		
		String result = request.initProperties();
		String expected = "Service name is invalid: " + props.get("servicename");
		
		assertEquals(expected, result);
	}
	
	@Ignore
	public void testServiceNameEvInvalid() throws Exception {
		Map<String, String> props = new HashMap<>();
		props.put("requesttype", "getKOFeatures");
		props.put("plangebiedWkt", validwktString);
		props.put(SERVICENAME_EV, "risicokaartWKS");
		props.put(SERVICENAME_KO, "risicokaartWFS");
		
		VeiligheidtoetsRequest request = RequestFactory.createVeiligheidtoetsRequest(props);
		
		Field configDir = request.getClass().getSuperclass().getDeclaredField("configDir");
		configDir.setAccessible(true);
		configDir.set(request, configURL);
		
		String result = request.initProperties();
		String expected = "Service name EV is invalid!";
		
		assertEquals(expected, result);
	}
	
	@Ignore
	public void testServiceNameKoInvalid() throws Exception {
		Map<String, String> props = new HashMap<>();
		props.put("requesttype", "getKOFeatures");
		props.put("plangebiedWkt", validwktString);
		props.put(SERVICENAME_EV, "risicokaartWFS");
		props.put(SERVICENAME_KO, "risicokaartWKS");
		
		VeiligheidtoetsRequest request = RequestFactory.createVeiligheidtoetsRequest(props);
		
		Field configDir = request.getClass().getSuperclass().getDeclaredField("configDir");
		configDir.setAccessible(true);
		configDir.set(request, configURL);
		
		String result = request.initProperties();
		String expected = "Service name KO is invalid!";
		
		assertEquals(expected, result);
	}
	
	@Ignore
	public void testTemplateMissing() throws Exception {
		Map<String, String> props = new HashMap<>();
		props.put("requesttype", "getEVFeatures");
		props.put("plangebiedWkt", validwktString);
		props.put("servicename", "risicokaartWFS");
		
		VeiligheidtoetsRequest request = RequestFactory.createVeiligheidtoetsRequest(props);
		
		Field configDir = request.getClass().getSuperclass().getDeclaredField("configDir");
		configDir.setAccessible(true);
		configDir.set(request, configURL);
		
		String response = request.initProperties();
		String expected = "Filter is missing!";
		
		assertEquals(expected, response);
	}
	
	@Ignore
	public void testTemplateEVMissing() throws Exception {
		Map<String, String> props = new HashMap<>();
		props.put("requesttype", "getKOFeatures");
		props.put("plangebiedWkt", validwktString);
		props.put(SERVICENAME_EV, "risicokaartWFS");
		props.put(SERVICENAME_KO, "risicokaartWFS");
		
		VeiligheidtoetsRequest request = RequestFactory.createVeiligheidtoetsRequest(props);
		
		Field configDir = request.getClass().getSuperclass().getDeclaredField("configDir");
		configDir.setAccessible(true);
		configDir.set(request, configURL);
		
		String response = request.initProperties();
		String expected = "Filter EV is missing!";
		
		assertEquals(expected, response);
	}
	
	@Ignore
	public void testTemplateKOMissing() throws Exception {
		Map<String, String> props = new HashMap<>();
		props.put("requesttype", "getKOFeatures");
		props.put("plangebiedWkt", validwktString);
		props.put(SERVICENAME_EV, "risicokaartWFS");
		props.put(SERVICENAME_KO, "risicokaartWFS");
		props.put(FILTER_EV, "SQ3");
		
		VeiligheidtoetsRequest request = RequestFactory.createVeiligheidtoetsRequest(props);
		
		Field configDir = request.getClass().getSuperclass().getDeclaredField("configDir");
		configDir.setAccessible(true);
		configDir.set(request, configURL);
		
		String response = request.initProperties();
		String expected = "Filter KO is missing!";
		
		assertEquals(expected, response);
	}
	
	@Ignore
	public void testNumberOfFeaturesFound() throws Exception {
		Map<String, String> props = new HashMap<>();
		props.put("requesttype", "getEVFeatures");
		props.put("plangebiedWkt", validwktString);
		props.put("servicename", "risicokaartWFS");
		props.put("filter", "BQ3");
		
		VeiligheidtoetsRequest request = RequestFactory.createVeiligheidtoetsRequest(props);
		TemplateHandler handler = new TemplateHandler();
		
		Field configDir = request.getClass().getSuperclass().getDeclaredField("configDir");
		configDir.setAccessible(true);
		configDir.set(request, configURL);
		
		Field templateGroup = handler.getClass().getDeclaredField("templateGroup");
		templateGroup.setAccessible(true);
		templateGroup.set(handler, new StringTemplateGroup("template group", templateURL));
		
		Field templateHandler = request.getClass().getDeclaredField("templateHandler");
		templateHandler.setAccessible(true);
		templateHandler.set(request, handler);
		
		request.initProperties();
		
		Map<String, String> expected = new HashMap<>();
		expected.put("numberOfFeaturesFound", Integer.toString(8));

		assertEquals(expected, request.getResponse());
	}
	
	@Ignore
	public void testFeaturesFound() throws Exception {
		Map<String, String> props = new HashMap<>();
		props.put("requesttype", "getEVFeatures");
		props.put("plangebiedWkt", validwktString);
		props.put("servicename", "basisnetWFS");
		props.put("filter", "SQ5");
		
		VeiligheidtoetsRequest request = RequestFactory.createVeiligheidtoetsRequest(props);
		TemplateHandler handler = new TemplateHandler();
		
		Field configDir = request.getClass().getSuperclass().getDeclaredField("configDir");
		configDir.setAccessible(true);
		configDir.set(request, configURL);
		
		Field templateGroup = handler.getClass().getDeclaredField("templateGroup");
		templateGroup.setAccessible(true);
		templateGroup.set(handler, new StringTemplateGroup("template group", templateURL));
		
		Field templateHandler = request.getClass().getDeclaredField("templateHandler");
		templateHandler.setAccessible(true);
		templateHandler.set(request, handler);
		
		request.initProperties();
		
		String result = request.getResponse().keySet().iterator().next();
		String expected = "features";
		
		assertEquals(expected, result);
	}
	
	@Ignore
	public void testKOFeaturesNoFeaturesFound() throws Exception {
		Map<String, String> props = new HashMap<>();
		props.put("requesttype", "getKOFeatures");
		props.put("plangebiedWkt", "POLYGON((20.0 51.0,21.0 51.0,21.0 52.0,20.0 51.0))");
		props.put(SERVICENAME_EV, "risicokaartWFS");
		props.put(SERVICENAME_KO, "veiligheidstoetsWFS");
		props.put(FILTER_EV, "BQ2");
		props.put(FILTER_KO, "AQ1");
		
		VeiligheidtoetsRequest request = RequestFactory.createVeiligheidtoetsRequest(props);
		TemplateHandler handler = new TemplateHandler();
		
		Field configDir = request.getClass().getSuperclass().getDeclaredField("configDir");
		configDir.setAccessible(true);
		configDir.set(request, configURL);
		
		Field templateGroup = handler.getClass().getDeclaredField("templateGroup");
		templateGroup.setAccessible(true);
		templateGroup.set(handler, new StringTemplateGroup("template group", templateURL));
		
		Field templateHandler = request.getClass().getDeclaredField("templateHandler");
		templateHandler.setAccessible(true);
		templateHandler.set(request, handler);
		
		request.initProperties();
		
		Field urlKo = request.getClass().getDeclaredField("urlKo");
		urlKo.setAccessible(true);
		urlKo.set(request, "http://evs.local.nl/services/veiligheidstoets_wfs");
		
		Map<String, String> expected = new HashMap<>();
		expected.put("message", "\"NO_FEATURES_FOUND\"");

		assertEquals(expected, request.getResponse());
	}
	
	@Ignore
	public void testKwetsbareObjecten() throws Exception {
		Map<String, String> props = new HashMap<>();
		props.put("requesttype", "getKOFeatures");
		props.put("plangebiedWkt", validwktString);
		props.put(SERVICENAME_EV, "risicokaartWFS");
		props.put(SERVICENAME_KO, "veiligheidstoetsWFS");
		props.put(FILTER_EV, "BQ1");
		props.put(FILTER_KO, "AQ1");
		
		VeiligheidtoetsRequest request = RequestFactory.createVeiligheidtoetsRequest(props);
		TemplateHandler handler = new TemplateHandler();
		
		Field configDir = request.getClass().getSuperclass().getDeclaredField("configDir");
		configDir.setAccessible(true);
		configDir.set(request, configURL);
		
		Field templateGroup = handler.getClass().getDeclaredField("templateGroup");
		templateGroup.setAccessible(true);
		templateGroup.set(handler, new StringTemplateGroup("template group", templateURL));
		
		Field templateHandler = request.getClass().getDeclaredField("templateHandler");
		templateHandler.setAccessible(true);
		templateHandler.set(request, handler);
		
		request.initProperties();
		
		Field urlKo = request.getClass().getDeclaredField("urlKo");
		urlKo.setAccessible(true);
		urlKo.set(request, "http://evs.local.nl/services/veiligheidstoets_wfs");
		
		String result = request.getResponse().keySet().iterator().next();
		String expected = "message";
		
		assertEquals(expected, result);
	}
}
