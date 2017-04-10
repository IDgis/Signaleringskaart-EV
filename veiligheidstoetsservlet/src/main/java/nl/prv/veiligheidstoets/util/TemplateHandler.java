package nl.prv.veiligheidstoets.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;

public class TemplateHandler {
	
	private StringTemplateGroup templateGroup;

	public TemplateHandler() {
		templateGroup = new StringTemplateGroup("template group");
	}
	
	public String getFilter(String templateName, Map<String,String> props){
		 StringTemplate template = templateGroup.getInstanceOf("nl/prv/veiligheidstoets/templates/" + templateName);
		 Iterator it = props.entrySet().iterator();
		 while (it.hasNext()) {  				 
		     Map.Entry<String,String> pairs = (Map.Entry<String,String>)it.next();
		     template.setAttribute(pairs.getKey(), pairs.getValue());  
		     it.remove(); // avoids a ConcurrentModificationException
		 }
		 return template.toString();

		 //TODO check if all placeholders are replaced by properties??
	}
	
	
}
