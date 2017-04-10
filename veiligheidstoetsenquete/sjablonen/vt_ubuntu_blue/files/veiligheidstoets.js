
var baseurl = "${SURVEY.BASE.URL}";
var wmsurl = "${WMS.BASE.URL}";
var baseservleturl = baseurl + "/veiligheidstoetsservlet";


var toetsError = false;
var spatialQuestions = new Array();
var vlayer;
var map;
var urlInput;
var curId;

$(document).ready(function() {
	if(document.limesurvey) {
		$('.map').each(function() {
			var mapDiv = $(this);
		    var sgq = mapDiv.attr('data-sgq');
		    var answerInput = $('#answer' + sgq);
		    answerInput.hide();
		    map = initialiseBaseMap(mapDiv, true, answerInput);
	
		});
		$('.imageurl').each(function() {
			var urlDiv = $(this);
		    var sgq = urlDiv.attr('data-sgq');
		    var qid = urlDiv.attr('data-qid');
		    urlInput = $('#answer' + sgq);
		    $('#question' + qid).hide();
		    curId = urlDiv.attr('id');
		});
		$('.inputchoice').each(function() {
			var inputChoiceDiv = $(this);
		    var sgq = inputChoiceDiv.attr('data-sgq');
		    var inputChoiceAns =  $('#answer' + sgq + 'A3');
		    inputChoiceAns.click();   
		});
		$('.submitshp').each(function() {
			var submitDiv = $(this);
			postShape(submitDiv);
		});	
	} 
});	

//Spatialquestion onshow
jQuery(function($) {
	 $('.SpatialQuestion')
	   .show(100, function() {
		   if(document.limesurvey) {
			   var spatialDiv = $(this);     
			   postSpatialQuestion(spatialDiv);
		   }   
	    }) 
	    .show(); 
});
jQuery(function($) {
  var _oldShow = $.fn.show;
  $.fn.show = function(speed, oldCallback) {
    return $(this).each(function() {
      var obj = $(this),
          newCallback = function() {
            if ($.isFunction(oldCallback)) {
              oldCallback.apply(obj);
            }
            obj.trigger('beforeShow');
          };
      _oldShow.apply(obj, [speed, newCallback]);
    });
  }
});



function postSpatialQuestion(spatialDiv) {
	var sgq = "";
		//defaults can be overruled
		var postbody = new Object({servicename:'veiligheidstoetsWFS', requesttype:'GetFeature', geomprop:'the_geom', });
		for(s in spatialDiv.data()) {
			switch(s)
			{
			case 'sgq':
				sgq = spatialDiv.data(s);
			  break;
			case 'plangebied-wkt':
				var wktStr = spatialDiv.data(s);
				postbody[s] = wktStr.substring(wktStr.indexOf('hidestart') + 9, wktStr.indexOf('hideend'));
				//postbody[s] = wktStr.substring(wktStr.indexOf('>') + 1, wktStr.indexOf('</'));
			  break;
			default:
				postbody[s] = spatialDiv.data(s);
			}
		}		
		var sgqArray = sgq.split('X'); 
		var answerInputY = $('#answer' + sgq + 'Y');
		var answerInputN = $('#answer' + sgq + 'N');
		var question =  $('#question' + sgqArray[2]);
		question.hide();
		spatialQuestions.push(question);
		var url = baseservleturl + "/toets";
		document.body.style.cursor = 'wait';
		var posting = $.post( url, postbody,
		function(result){
			var aantal = $(result).find("wfs\\:FeatureCollection, FeatureCollection").attr("numberMatched");
	    	if(typeof aantal == "undefined"){
	    		aantal = $(result).find("wfs\\:FeatureCollection, FeatureCollection").attr("numberOfFeatures");
	    	}
	    	if(typeof aantal == "undefined"){
	    		if(!toetsError){
		    		toetsError=true;	
	    			//alert only first error
	    			alert ("Er is een fout opgetreden, raadpleeg de applicatie beheerder.");
	    			//toon de bijbehorende vraag
	    			question.show();
	    		}	
    			    			
	    	} else {
	    		if (parseInt(aantal) > 0){
	    			answerInputY.click();
		    	} else { 
		    		answerInputN.click();
		    	}	
	    		for (var a in spatialQuestions){
	    			spatialQuestions[a].hide();
	    		}
	    	}
	    	document.body.style.cursor = 'default';
	    	
	    });
};


function postShape(submitDiv){
	var sgq = submitDiv.attr('data-sgq');
	var resultWkt =  $('#answer633225X2X156');
	$("#shpsubmit").click(function () {
		var url = baseservleturl + "/shp2wkt";
		var fileInput = $("#"+sgq);
		var shapeObject = jQuery.parseJSON(fileInput.val());
		if(shapeObject === null){
			alert("selecteer eerst shape bestanden.");
			return;
		} else if (typeof shapeObject[0].ext != ""){
			var shpcheck = false;
			var shxcheck = false;
			var dbfcheck = false;
			var shpbaseurl = baseurl + "/tmp/upload/";
			var shpurl;
			var shxurl;
			var dbfurl;
			var deleteurls = new Array();
    		for (var s in shapeObject) {
    			var file = shapeObject[s];
    			var deleteurl = baseurl + "/index.php/uploader/index/mode/upload/delete/1/fieldname/" + sgq + "/filename/" + file.filename + "/name/" + file.name;
				deleteurls.push (deleteurl);
    			switch(file.ext)
    			{
    			case 'shp':
    				shpcheck = true;
    				shpurl = shpbaseurl +  file.filename;
    				//http://192.168.122.108/index.php/uploader/index/mode/upload/delete/1/fieldname/633225X2X157/filename/futmp_6dg8hbajbnqr8pe_dbf/name/invloedsgebied.dbf
    				
    			  break;
    			case 'shx':
    				shxcheck = true;
    				shxurl = shpbaseurl +  file.filename;
    			  break;
    			case 'dbf':
    				dbfcheck = true;
    				dbfurl = shpbaseurl +  file.filename;
    			  break;  
    			}
    		}	
    		if(!shpcheck||!shxcheck||!dbfcheck){
    			alert("*.shp, *.shx of *.dbf mist");//toon waarschuwing en ga naar vorige pagina
    			return;
    		}
    		var postbody = new Object({shpurl:shpurl, shxurl:shxurl, dbfurl:dbfurl})
    		var posting = $.post( url, postbody,
			function(result){
				var wktStr = $(result).find("wkt").text();
				var leegObj = null;//'[{"title":"","comment":"","size":"","name":"","filename":"","ext":""},{"title":"","comment":"","size":"","name":"","filename":"","ext":""},{"title":"","comment":"","size":"","name":"","filename":"","ext":""}]';
				if(typeof wktStr == "undefined" || wktStr == ""){
	    			alert ("Er is een fout opgetreden bij het laden van de shape file, raadpleeg de applicatie beheerder.")
	    			$("#"+sgq+"_filecount").val(0);
					$("#"+sgq+"_uploadedfiles").html("");
					$("#"+sgq).val(leegObj);
	    			return;
				} else { 
					$("#"+sgq+"_filecount").val(0);
					$("#"+sgq+"_uploadedfiles").html("");
					$("#"+sgq).val(leegObj);
					//resultWkt.text("<div style='display:none;'>" + wktStr + "</div>");
					resultWkt.text("hidestart" + wktStr + "hideend");
					showOnMap(wktStr);
				}
				//opschonen
				for (var s in deleteurls) {
					var deleteurl = deleteurls[s];
					$.get( deleteurl, function( data ) {
							//alert( "Opschonen gelukt " + data )
					});
				}
    		 });
		} else {
			alert ("Er is geen geldige shape file geselecteerd, probeer het nogmaals.")
			return;
		}
	});	
};


function initialiseBaseMap(mapDiv,editable,answerInput) {
	//mapDiv.empty ();
	mapDiv.css({
	       'width': '700px',
	       'height': '700px',
	       'position': 'relative'
	});
	vlayer = new OpenLayers.Layer.Vector("Editable");
	var wkt = new OpenLayers.Format.WKT();
	var answer = answerInput.text();
	var wktPoly = '';
	if (answer.indexOf('hidestart') == -1){
		wktPoly = answer;
	} else {
		wktPoly = answer.substring(answer.indexOf('hidestart') + 9, answer.indexOf('hideend}'));
		//wktPoly = answer.substring(answer.indexOf('>') + 1, answer.indexOf('</'));
	}
	var isValid = true;
	if (editable){
		vlayer.events.on({
	        'beforefeatureadded': function() {
	        	vlayer.removeAllFeatures();
	        },
	        'featureadded': function(event) {
	        	var wktStr = wkt.write(event.feature);
	        	checkWktValiditie(wktStr, answerInput);
	        },
	        'afterfeaturemodified': function(event) {
	        	var wktStr = wkt.write(event.feature);
	        	checkWktValiditie(wktStr, answerInput);
	        }
	      });
	} 

	//map controls 
	 var mapControls;
	 var panzoom = new OpenLayers.Control.PanZoom();
	 var touchnav = new OpenLayers.Control.TouchNavigation();
	 if (editable){
		 var editControls = [new OpenLayers.Control.ModifyFeature(vlayer, {'displayClass': 'olControlModifyFeature',
                				  mode: OpenLayers.Control.ModifyFeature.RESHAPE, title: 'Wijzig plangebied'
		 					 }),
                			 new OpenLayers.Control.DrawFeature(vlayer, OpenLayers.Handler.Polygon, {
                				 'displayClass': 'olControlDrawFeaturePolygon',
                				 title: 'Voeg plangebied toe'
                        	 }),
                        	 new OpenLayers.Control.Navigation({title: 'Verplaats kaart'})];
		 var editToolbar = new OpenLayers.Control.Panel({
		       displayClass: 'olControlEditingToolbar',
		       defaultControl: editControls[2]
		 });
		 editToolbar.addControls(editControls,touchnav);
		 
		 mapControls = [panzoom, editToolbar, touchnav];
		 
	 } else {
		 mapControls = [panzoom, touchnav];
	 }

     var resolutions = [3440.64, 1720.32, 860.16, 430.08, 215.04, 
                        107.52, 53.76, 26.88, 13.44, 6.72, 3.36, 1.68, 0.84, 0.42, 0.21];

  
     
     var pdok = new OpenLayers.Layer.TMS(
   	        "Achtergrond", // name for display in LayerSwitcher
   	     
 	        "http://geodata.nationaalgeoregister.nl/tms/", // service endpoint
     	        {   layername: 'brtachtergrondkaartpastel@EPSG:28992@png', 
     	            isBaseLayer: true, 
     	            type: 'png8',
     	            tileOrigin: new OpenLayers.LonLat(-285401.92,22598.08),
     	            serverResolutions: resolutions,
     	            maxExtent: new OpenLayers.Bounds (-7000, 289000, 300000, 629000),
     	            displayOutsideMaxExtent: false,
     	            tileSize: new OpenLayers.Size (256, 256)
     	       }
     	  );

     var risicocontouren = new OpenLayers.Layer.WMS(
    		 
             "Risicocontouren",

             "http://servicespub.risicokaart.nl/rk_services_pub/services/WMS-risicokaart",
             {	layers: 'risicocontouren',
            	format: 'image/png'
            }
         );
    
     map = new OpenLayers.Map(mapDiv.get(0), {
         controls: mapControls, 
         projection:'EPSG:28992',
         resolutions: resolutions,
         units: 'm',
         maxExtent: new OpenLayers.Bounds (-7000, 289000, 300000, 629000),
         layers: [pdok, risicocontouren, vlayer]
       }); 
    // map.addLayers([ risicocontouren, vlayer, pdok]);
     var zoom = 5;
     var centrex = 225000;
     var centrey = 500000;
     map.setCenter(new OpenLayers.LonLat(centrex , centrey), zoom); 
	 if(wktPoly!=''){ 
		showOnMap(wktPoly);
	 } 
	 return map;
};


function showOnMap(wktStr) {
	if(vlayer!==null && map!==null){
		vlayer.removeAllFeatures();
		var wkt = new OpenLayers.Format.WKT();
	  	vlayer.addFeatures([wkt.read(wktStr)]);
	  	zoomToPlangebied();
	}    
};

function zoomToPlangebied() {
	var bounds = vlayer.features[0].geometry.getBounds();
  	var zoom = map.getZoomForExtent(bounds);
  	var centrex = bounds.left + ((bounds.right - bounds.left) / 2);
  	var centrey = bounds.bottom + ((bounds.top - bounds.bottom) / 2);
    map.setCenter(new OpenLayers.LonLat(centrex , centrey), zoom);
    updateImageUrl();
};

function updateImageUrl(){
	if(urlInput!==undefined){
		var kleur = 'EE9900';
		var bbox = map.getExtent().toBBOX(0,false);
		var url = wmsurl + '/veiligheidstoets/services/plangebied_wms?service=WMS&REQUEST=GetMap&LAYERS=&STYLES=&TRANSPARENT=TRUE&SRS=EPSG:28992&VERSION=1.1.1&EXCEPTIONS=application/vnd.ogc.se_xml&FORMAT=image/png';
		url += '&HEIGHT=500&WIDTH=800';
		url += '&BBOX=' + bbox;
		var sld_body = '%3CStyledLayerDescriptor%20xmlns%3Aogc%3D%22http%3A%2F%2Fwww.opengis.net%2Fogc%22%3E%3CNamedLayer%3E%3CName%3Epg_ond%3C%2FName%3E%3CUserStyle%3E%3CName%3Eplangebied%3C%2FName%3E%3CFeatureTypeStyle%3E%3CName%3Eplangebied%3C%2FName%3E%3CRule%3E%3Cogc%3AFilter%3E%3Cogc%3APropertyIsEqualTo%3E%3Cogc%3APropertyName%3Eid%3C%2Fogc%3APropertyName%3E%3Cogc%3ALiteral%3E'+ curId + '%3C%2Fogc%3ALiteral%3E%3C%2Fogc%3APropertyIsEqualTo%3E%3C%2Fogc%3AFilter%3E%3CPolygonSymbolizer%3E%3CFill%3E%3CSvgParameter%20name%3D%22fill%22%3E%23EE9900%3C%2FSvgParameter%3E%3CSvgParameter%20name%3D%22fill-opacity%22%3E0.2%3C%2FSvgParameter%3E%3C%2FFill%3E%3CStroke%3E%3CSvgParameter%20name%3D%22stroke%22%3E%23EE9900%3C%2FSvgParameter%3E%3CSvgParameter%20name%3D%22stroke-opacity%22%3E1.0%3C%2FSvgParameter%3E%3CSvgParameter%20name%3D%22stroke-width%22%3E1%3C%2FSvgParameter%3E%3C%2FStroke%3E%3C%2FPolygonSymbolizer%3E%3C%2FRule%3E%3C%2FFeatureTypeStyle%3E%3C%2FUserStyle%3E%3C%2FNamedLayer%3E%3C%2FStyledLayerDescriptor%3E';  
		url += '&SLD_BODY=' + sld_body;
		urlInput.text( url );
	}
};

function checkWktValiditie(wktStr, answer){
	var url = baseservleturl + "/wktvalid";
	var postbody = new Object({wkt:wktStr});
	var posting = $.post( url, postbody,
		function(result){
			var valid = $(result).find("wktIsValid").text();
			if(valid=="false"){
				var msg = $(result).find("wktMessage").text();
				alert (msg);
				//vlayer.removeAllFeatures();
				answer.text('');
			} else {
				answer.text("<div style='display: none;'>hidestart" + wktStr + "hideend</div>");
				//answer.text("hidestart" + wktStr + "hideend");
	        	zoomToPlangebied();
			}
	    }
	);
};





