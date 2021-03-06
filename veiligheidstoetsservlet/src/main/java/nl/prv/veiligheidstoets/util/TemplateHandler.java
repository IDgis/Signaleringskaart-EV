package nl.prv.veiligheidstoets.util;

import java.util.Map;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;

public class TemplateHandler {
	
	private StringTemplateGroup templateGroup;
	private String locationURL = "/etc/veiligheidstoets/templates";

	public TemplateHandler() {
		templateGroup = new StringTemplateGroup("template group", locationURL);
	}
	
	public String getFilter(String templateName, Map<String,String> props){
		if(templateGroup.isDefined(templateName)) {
			StringTemplate template = templateGroup.getInstanceOf(templateName, props);
			return template.toString();
		}
		return null;
	}
}