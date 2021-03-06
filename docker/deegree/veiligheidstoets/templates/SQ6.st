<?xml version="1.0" encoding="UTF-8"?>
<wfs:GetFeature  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd"
    xmlns:ogc="http://www.opengis.net/ogc" xmlns:gml="http://www.opengis.net/gml"
    xmlns:wfs="http://www.opengis.net/wfs" service="WFS" version="1.1.0"
    outputFormat="text/xml; subtype=gml/3.2">
    <wfs:Query typeName="basisnet_spoor_trajecten">
        <ogc:PropertyName>PR10_6</ogc:PropertyName>
        <ogc:PropertyName>PR10_7</ogc:PropertyName>
        <ogc:PropertyName>PR10_8</ogc:PropertyName>
        <ogc:PropertyName>PAG</ogc:PropertyName>
        <ogc:PropertyName>A</ogc:PropertyName>
        <ogc:PropertyName>B2</ogc:PropertyName>
        <ogc:PropertyName>B3</ogc:PropertyName>
        <ogc:PropertyName>C3</ogc:PropertyName>
        <ogc:PropertyName>D3</ogc:PropertyName>
        <ogc:PropertyName>D4</ogc:PropertyName>
        <ogc:PropertyName>AWK</ogc:PropertyName>
        <ogc:PropertyName>B2WK</ogc:PropertyName>
        <ogc:PropertyName>Breedte</ogc:PropertyName>
        <ogc:Filter>
            <ogc:Or>
	            <ogc:And>
	                <ogc:PropertyIsEqualTo>
	                    <ogc:PropertyName>PAG</ogc:PropertyName>
	                    <ogc:Literal>Ja</ogc:Literal>
	                </ogc:PropertyIsEqualTo>
	                <ogc:PropertyIsEqualTo>
	                    <ogc:PropertyName>Breedte</ogc:PropertyName>
	                    <ogc:Literal>0-24</ogc:Literal>
	                </ogc:PropertyIsEqualTo>
	                <ogc:DWithin>
	                    <ogc:PropertyName>the_geom</ogc:PropertyName>
	                    $plangebiedgml$        
	                    <ogc:Distance units="meter">42</ogc:Distance>
	                </ogc:DWithin>
	            </ogc:And>
	            <ogc:And>
	                <ogc:PropertyIsEqualTo>
	                    <ogc:PropertyName>PAG</ogc:PropertyName>
	                    <ogc:Literal>Ja</ogc:Literal>
	                </ogc:PropertyIsEqualTo>
	                <ogc:PropertyIsEqualTo>
	                    <ogc:PropertyName>Breedte</ogc:PropertyName>
	                    <ogc:Literal>25-49</ogc:Literal>
	                </ogc:PropertyIsEqualTo>
	                <ogc:DWithin>
	                    <ogc:PropertyName>the_geom</ogc:PropertyName>
	                    $plangebiedgml$        
	                    <ogc:Distance units="meter">54.5</ogc:Distance>
	                </ogc:DWithin>
	            </ogc:And>
	            <ogc:And>
	                <ogc:PropertyIsEqualTo>
	                    <ogc:PropertyName>PAG</ogc:PropertyName>
	                    <ogc:Literal>Ja</ogc:Literal>
	                </ogc:PropertyIsEqualTo>
	                <ogc:PropertyIsEqualTo>
	                    <ogc:PropertyName>Breedte</ogc:PropertyName>
	                    <ogc:Literal>50-74</ogc:Literal>
	                </ogc:PropertyIsEqualTo>
	                <ogc:DWithin>
	                    <ogc:PropertyName>the_geom</ogc:PropertyName>
	                    $plangebiedgml$        
	                    <ogc:Distance units="meter">67</ogc:Distance>
	                </ogc:DWithin>
	            </ogc:And>
	        </ogc:Or>      
        </ogc:Filter>
    </wfs:Query>
</wfs:GetFeature>