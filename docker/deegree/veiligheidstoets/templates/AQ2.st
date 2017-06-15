<?xml version="1.0" encoding="UTF-8"?>
<wfs:GetFeature xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 xsi:schemaLocation="http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd"
 xmlns:ogc="http://www.opengis.net/ogc"
 xmlns:gml="http://www.opengis.net/gml"
 xmlns:wfs="http://www.opengis.net/wfs"
 service="WFS"
 version="1.1.0"
 outputFormat="text/xml; subtype=gml/3.2.1">
    <wfs:Query typeName="verblijfsobject_ov">
        <ogc:PropertyName>gebruiksdoel</ogc:PropertyName>
        <ogc:PropertyName>oppervlakte</ogc:PropertyName>
        <ogc:Filter>
	        <ogc:And>
       	        <ogc:Within>
       	            <ogc:PropertyName>geom</ogc:PropertyName>
       	            	$plangebiedgml$                            
       	        </ogc:Within>
	            <ogc:Or>
    	            <ogc:PropertyIsEqualTo>
    	                <ogc:PropertyName>gebruiksdoel</ogc:PropertyName>
    	                <ogc:Literal>sportfunctie</ogc:Literal>
    	            </ogc:PropertyIsEqualTo> 
	                <ogc:PropertyIsEqualTo>
	                    <ogc:PropertyName>gebruiksdoel</ogc:PropertyName>
	                    <ogc:Literal>overige gebruiksfunctie</ogc:Literal>
	                </ogc:PropertyIsEqualTo> 
	                <ogc:PropertyIsEqualTo>
	                    <ogc:PropertyName>gebruiksdoel</ogc:PropertyName>
	                    <ogc:Literal>industriefunctie</ogc:Literal>
	                </ogc:PropertyIsEqualTo>
	              
	                <ogc:And>
	                    <ogc:PropertyIsEqualTo>
	                        <ogc:PropertyName>gebruiksdoel</ogc:PropertyName>
	                        <ogc:Literal>winkelfunctie</ogc:Literal>
	                    </ogc:PropertyIsEqualTo>
	                    <ogc:PropertyIsLessThanOrEqualTo>
	                        <ogc:PropertyName>oppervlakte</ogc:PropertyName>
	                        <ogc:Literal>1000</ogc:Literal>
	                    </ogc:PropertyIsLessThanOrEqualTo>
	                </ogc:And>
	                <ogc:And>
	                    <ogc:PropertyIsEqualTo>
	                        <ogc:PropertyName>gebruiksdoel</ogc:PropertyName>
	                        <ogc:Literal>logiesfunctie</ogc:Literal>
	                    </ogc:PropertyIsEqualTo>
	                    <ogc:PropertyIsLessThanOrEqualTo>
	                        <ogc:PropertyName>oppervlakte</ogc:PropertyName>
	                        <ogc:Literal>1500</ogc:Literal>
	                    </ogc:PropertyIsLessThanOrEqualTo>   
	                </ogc:And>
	                <ogc:And>
	                    <ogc:PropertyIsEqualTo>
	                        <ogc:PropertyName>gebruiksdoel</ogc:PropertyName>
	                        <ogc:Literal>kantoorfunctie</ogc:Literal>
	                    </ogc:PropertyIsEqualTo>
	                    <ogc:PropertyIsLessThanOrEqualTo>
	                        <ogc:PropertyName>oppervlakte</ogc:PropertyName>
	                        <ogc:Literal>1500</ogc:Literal>
	                    </ogc:PropertyIsLessThanOrEqualTo>    
	                </ogc:And>
	            </ogc:Or>
	        </ogc:And>
        </ogc:Filter>     
    </wfs:Query>
</wfs:GetFeature>