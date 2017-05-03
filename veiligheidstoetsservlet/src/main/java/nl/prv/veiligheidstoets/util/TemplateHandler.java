package nl.prv.veiligheidstoets.util;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;

public class TemplateHandler {
	
	private StringTemplateGroup templateGroup;

	public TemplateHandler() {
		templateGroup = new StringTemplateGroup("template group");
	}
	
	public String getFilter(String templateName, Map<String,String> props){
		 StringTemplate template = templateGroup.getInstanceOf("nl/prv/veiligheidstoets/templates/" + templateName);
		 Iterator<Entry<String, String>> it = props.entrySet().iterator();
		 while (it.hasNext()) {  				 
		     Map.Entry<String,String> pairs = it.next();
		     template.setAttribute(pairs.getKey(), pairs.getValue());  
		     //it.remove(); // avoids a ConcurrentModificationException
		 }
		 return template.toString();
	}
}