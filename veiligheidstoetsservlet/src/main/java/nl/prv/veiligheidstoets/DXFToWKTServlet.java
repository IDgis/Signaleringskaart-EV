package nl.prv.veiligheidstoets;

import java.io.File;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DXFToWKTServlet extends HttpServlet {
	
	public void init() {
		System.out.println("init servlet DXFToWKT");
		//URL url = Loader.getResource("log4j.xml");
		//DOMConfigurator.configure(url);
		//loadConfig();
	}
	
	
	/** 
	 * handles the post requests from the veiligheidstoets client
	 * 
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response) {
		File dxf = null;
	}
	

}
