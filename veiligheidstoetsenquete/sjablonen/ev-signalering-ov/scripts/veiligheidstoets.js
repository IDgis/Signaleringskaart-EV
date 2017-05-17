var baseurl = "${SURVEY.BASE.URL}";
var wmsurl = "http://evs.local.nl"//"${WMS.BASE.URL}";
var baseservleturl = "http://evs.local.nl/veiligheidstoetsservlet/toets";


var toetsError = false;
var spatialQuestions = [];
var nextSpatialQuestion = 0
var vlayer;
var map;
var urlInput;
var curId;



$(document).ready(function() {
    if(document.limesurvey) {
		$('.ev-map').each(function() {
            if (map===undefined) {
     			var mapDiv = $(this);
    		    var sgq = mapDiv.attr('data-sgq');
    		    var answerInput = $('#answer' + sgq);
    		    //answerInput.hide();
                initialiseBaseMap(mapDiv, true, answerInput);
            }    

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
        $('.spatialquestion').each(function() {
            var sq = $(this);
            var spatialquestion = {};
        	for(var s in sq.data()) {
        		spatialquestion[s] = sq.data(s);
        	}
            spatialQuestions.push(spatialquestion);
        }); 
        
	} 
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
  };
});


jQuery(function($) {
  $('.spatialquestion')
    .bind('beforeShow', function() {
        if($(this)[0]=== $('.spatialquestion')[0]) {
            postSpatialQuestion(spatialQuestions[0]);
        }
    }) 
    .show(1000, function() {
      //in show callback;
    })
    .show();
});
    


function postSpatialQuestion(spatialQuestion) {
	var sgq = spatialQuestion['sgq'];              
	var sgqArray = sgq.split('X');
	var question =  $('#question' + sgqArray[2]);
	//question.hide();
	document.body.style.cursor = 'wait';
	$.post( baseservleturl, spatialQuestion, function(result){

		if (result.error) {
			alert (result.error);
		}
		if (result.numberOfFeaturesFound !== undefined) {
			if (result.numberOfFeaturesFound > 0) {
				$('#answer'+ sgq + 'Y').click();
			} else {
                $('#answer'+ sgq + 'N').click(); 
			}
		} else {
			if (result.features !== undefined) {
				showProperties (result.features);
                $('#answer'+ sgq + 'Y').click();
                
			} else {
    		    $('#answer'+ sgq + 'N').click();  
			}
		}	
		nextSpatialQuestion++;
		if (nextSpatialQuestion < spatialQuestions.length){
		postSpatialQuestion(spatialQuestions[nextSpatialQuestion]);
		}
		if (nextSpatialQuestion === spatialQuestions.length){
			document.body.style.cursor = 'default';
		}	 
    });

}


function showProperties (features) {
    $('.properties').empty();
    var table = $('.properties')[0];
    
    var thead = table.appendChild(document.createElement("thead"));
    var hrow = thead.appendChild(document.createElement("tr"));
    var tbody = table.appendChild(document.createElement("tbody"));

    for (var feature of features) {
		var row = tbody.appendChild(document.createElement("tr"));
        var properties = feature.properties;
        
		for (var property of properties) {
            if (feature==="0") {
                var hcol = hrow.appendChild(document.createElement("th"));
                hcol.appendChild(document.createTextNode(Object.keys(property)));
            }  
            var col = row.appendChild(document.createElement("td"));
            col.appendChild(document.createTextNode(Object.values(property)));
		}
	}
	//resultDiv.show();
}


function showOnMap(wktStr) {
	if(vlayer!==undefined && map!==undefined){
		vlayer.removeAllFeatures();
		var wkt = new OpenLayers.Format.WKT();
	  	vlayer.addFeatures([wkt.read(wktStr)]);
	  	zoomToPlangebied();
	}    
}

function zoomToPlangebied() {
	var bounds = vlayer.features[0].geometry.getBounds();
  	var zoom = map.getZoomForExtent(bounds);
  	var centrex = bounds.left + ((bounds.right - bounds.left) / 2);
  	var centrey = bounds.bottom + ((bounds.top - bounds.bottom) / 2);
    map.setCenter(new OpenLayers.LonLat(centrex , centrey), zoom);
    updateImageUrl();
}

function updateImageUrl(){
	if(urlInput!==undefined){
		var kleur = 'EE9900';
		var bbox = map.getExtent().toBBOX(0,false);
		var url = wmsurl + '/services/plangebied_wms_ov?service=WMS&REQUEST=GetMap&LAYERS=&STYLES=&TRANSPARENT=TRUE&SRS=EPSG:28992&VERSION=1.1.1&EXCEPTIONS=application/vnd.ogc.se_xml&FORMAT=image/png';
		url += '&HEIGHT=500&WIDTH=800';
		url += '&BBOX=' + bbox;
		var sld_body = '%3CStyledLayerDescriptor%20xmlns%3Aogc%3D%22http%3A%2F%2Fwww.opengis.net%2Fogc%22%3E%3CNamedLayer%3E%3CName%3Epg_ond_ov%3C%2FName%3E%3CUserStyle%3E%3CName%3Eplangebied%3C%2FName%3E%3CFeatureTypeStyle%3E%3CName%3Eplangebied%3C%2FName%3E%3CRule%3E%3Cogc%3AFilter%3E%3Cogc%3APropertyIsEqualTo%3E%3Cogc%3APropertyName%3Eid%3C%2Fogc%3APropertyName%3E%3Cogc%3ALiteral%3E'+ curId + '%3C%2Fogc%3ALiteral%3E%3C%2Fogc%3APropertyIsEqualTo%3E%3C%2Fogc%3AFilter%3E%3CPolygonSymbolizer%3E%3CFill%3E%3CSvgParameter%20name%3D%22fill%22%3E%23EE9900%3C%2FSvgParameter%3E%3CSvgParameter%20name%3D%22fill-opacity%22%3E0.2%3C%2FSvgParameter%3E%3C%2FFill%3E%3CStroke%3E%3CSvgParameter%20name%3D%22stroke%22%3E%23EE9900%3C%2FSvgParameter%3E%3CSvgParameter%20name%3D%22stroke-opacity%22%3E1.0%3C%2FSvgParameter%3E%3CSvgParameter%20name%3D%22stroke-width%22%3E1%3C%2FSvgParameter%3E%3C%2FStroke%3E%3C%2FPolygonSymbolizer%3E%3C%2FRule%3E%3C%2FFeatureTypeStyle%3E%3C%2FUserStyle%3E%3C%2FNamedLayer%3E%3C%2FStyledLayerDescriptor%3E';  
		url += '&SLD_BODY=' + sld_body;
		urlInput.val( url );
	}
}

function checkWktValiditie(wktStr, answer) {
    var postbody = {wkt:wktStr, requesttype:'polygonIsValid'};
	$.post( baseservleturl, postbody,
		function(result){
        	if(result.isValid===false){
				alert (result.error);
				answer.val('');
			} else {
				answer.val(wktStr);
	        	zoomToPlangebied();
			}
	    }
	);
}


function initialiseBaseMap(mapDiv,editable,answerInput) {
	vlayer = new OpenLayers.Layer.Vector("Editable");
	var wkt = new OpenLayers.Format.WKT();
	var answer = answerInput.val();
	var wktPoly = answer;
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

     map = new OpenLayers.Map(mapDiv.get(0), {
         controls: mapControls, 
         projection:'EPSG:28992',
         resolutions: resolutions,
         units: 'm',
         maxExtent: new OpenLayers.Bounds (-7000, 289000, 300000, 629000)
         
       }); 
     
    var pdok = new OpenLayers.Layer.TMS(
   	        "Achtergrond", // name for display in LayerSwitcher
 	        "http://geodata.nationaalgeoregister.nl/tms/", // service endpoint
     	        {   layername: 'brtachtergrondkaartpastel@EPSG:28992@png', 
     	            isBaseLayer: true, 
     	            type: 'png8',
     	            tileOrigin: new OpenLayers.LonLat(-285401.92,22598.08),
     	            serverResolutions: resolutions,
     	            maxExtent: new OpenLayers.Bounds (-7000, 289000, 300000, 629000),
     	            //displayOutsideMaxExtent: false,
     	            tileSize: new OpenLayers.Size (256, 256)
     	       }
     	  );
  
       var risicocontouren = new OpenLayers.Layer.WMS(     
             "Risicocontouren",
             "http://servicespub.risicokaart.nl/rk_services_pub/services/WMS-risicokaart",
             {	layers: "risicocontouren",
            	format: "image/png",
                transparent: true
             }, {
               singleTile: true,
               isBaseLayer:false
            });
     
     map.addLayers([pdok,risicocontouren,vlayer]);
     var zoom = 4;
     var centrex = 225000;
     var centrey = 500000;
     map.setCenter(new OpenLayers.LonLat(centrex , centrey), zoom); 
	 if(wktPoly!=='' && wktPoly!==undefined){ 
		showOnMap(wktPoly);
	 } 
}






