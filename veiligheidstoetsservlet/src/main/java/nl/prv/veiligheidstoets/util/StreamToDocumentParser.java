package nl.prv.veiligheidstoets.util;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;

public class StreamToDocumentParser{
	/**
	 * converts an InputStream into a Document object
	 * 
	 * @param is
	 * @return a Document object
	 * @throws Exception
	 */
	
	public static Document parse(InputStream is)throws Exception {
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
			Document dom = builder.parse(is);
			return dom;
		}
	
}
