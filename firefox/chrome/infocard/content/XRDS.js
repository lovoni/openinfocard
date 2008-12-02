
//var xrdsHelper = new IdentitySelectorUtils("IdentitySelectorXrds");


var listObserver = {
   onDragOver : function (event, flavour, session) {
      IdentitySelectorDiag.logMessage("onDragOver: " + flavour.contentType);
      }
   , onDrop : function (evt, transferData, session) {
      IdentitySelectorDiag.logMessage("onDrop: " + transferData.data);
      // event.target.setAttribute("value",transferData.data);
      }
   , getSupportedFlavours : function () {
      var flavours = new FlavourSet();
      // flavours.appendFlavour("text/unicode");
      flavours.appendFlavour("application/x-informationcard+id");
      return flavours;
      }
};

/***************************************************************************
 * Desc:
 **************************************************************************/
function nsResolver(prefix) {
   var ns = {
      'xrds' : 'xri://$xrds', 'xrd': 'xri://$XRD*($v*2.0)'};
   return ns[prefix] || null;
}



var InformationCardXrds = {

	/***************************************************************************
	 * Desc:
	 **************************************************************************/
	xrdsListener: function(doc, hrefStr) {
	   this.doc = doc;
	   this.hrefStr = hrefStr;
	   this.onError = function(error) {
	      IdentitySelectorDiag.logMessage("xrdsListener:onError", "error=" + error);
	      };
	   this.onReady = function(xrds) {
	      try {
	         var elts = xrds.getElementsByTagName("Service");
	         for (var i = 0; i < elts.length; i++) {
	            var type = "" + elts[i].getElementsByTagName("Type")[0].firstChild.nodeValue + "";
	            var uri;
	            if (type.indexOf("http://infocardfoundation.org/policy/1.0/login") === 0) {
	               uri = "" + elts[i].getElementsByTagName("URI")[0].firstChild.nodeValue ;
	               doc.__identityselector__.icLoginPolicyUri = uri;
	               IdentitySelectorDiag.logMessage("xrdsListener:onReady", "IC Login Service Policy: " + doc.__identityselector__.icLoginPolicy);
	               InformationCardXrds.retrieveIcLoginServicePolicy(doc, doc.__identityselector__.icLoginPolicyUri);
	               }
	            else {
	               if (type.indexOf("http://infocardfoundation.org/service/1.0/login") === 0) {
	                  uri = "" + elts[i].getElementsByTagName("URI")[0].firstChild.nodeValue ;
	                  doc.__identityselector__.icLoginService = uri;
	                  IdentitySelectorDiag.logMessage("xrdsListener:onReady", "IC Login Service: " + doc.__identityselector__.icLoginService);
	                  }
	               else {
	                  IdentitySelectorDiag.logMessage("xrdsListener:onReady", "Service: type=" + type + ":" + typeof(type) + " URI=" + elts[i].getElementsByTagName("URI")[0].firstChild.nodeValue);
	                  }
	               }
	            }
	         // for (var i in xrds) {
	         // IdentitySelectorDiag.logMessage("xrdsListener:onReady", "i=" + i
				// + " type=" +
	         // typeof(i));
	         // }
	         var response = new XML (Components.classes['@mozilla.org/xmlextras/xmlserializer;1'].createInstance (Components.interfaces.nsIDOMSerializer).serializeToString(xrds.documentElement));
	         doc.__identityselector__.xrds = response;
	         IdentitySelectorDiag.logMessage("xrdsListener:onReady", "response=" + response);
	         // var elts = xrds.evalutate('Service', xrds, nsResolver,
	         // XPathResult.UNORDERED_NODE_ITERATOR_TYPE, null);
	         }
	      catch(e) {
	         IdentitySelectorDiag.logMessage("xrdsListener:onReady", "Error: " + e);
	         }
	      };
	}

	/***************************************************************************
	 * Desc:
		 **************************************************************************/
	, icLoginServiceListener : function(doc, hrefStr) {
	   this.doc = doc;
	   this.hrefStr = hrefStr;
	   this.onError = function(error) {
	      IdentitySelectorDiag.logMessage("icLoginServiceListener:onError", "error=" + error);
	      };
	   this.onReady = function(xrds) {
	      try {
	         var response = new XML (Components.classes['@mozilla.org/xmlextras/xmlserializer;1'].createInstance (Components.interfaces.nsIDOMSerializer).serializeToString(xrds.documentElement));
	         doc.__identityselector__.icLoginPolicy = response;
	         IdentitySelectorDiag.logMessage("icLoginServiceListener:onReady", "response=" + response);
	         // var elts = xrds.evalutate('Service', xrds, nsResolver,
	         // XPathResult.UNORDERED_NODE_ITERATOR_TYPE, null);
	         if ((doc.defaultView !== undefined) && (doc.defaultView)) {
	             var docWindow = doc.defaultView;
	             docWindow.addEventListener("dragdrop", InformationCardDragAndDrop.onWindowDragDrop, false);
	             if (InformationCardStatusbar !== undefined) {
	            	 InformationCardStatusbar.showStatusbarIcon(document, true);
	             } else {
	            	 IdentitySelectorDiag.logMessage("icLoginServiceListener:onReady", "InformationCardStatusbar===undefined");
	             }
	            }
	         }
	      catch(e) {
	         IdentitySelectorDiag.logMessage("icLoginServiceListener:onReady", "Error: " + e);
	         }
	      };
	}
	, retrieveXrds : function(doc, hrefStr) {
      IdentitySelectorDiag.logMessage("retrieveXrds: doc=" + doc.location.href + " href=", hrefStr);
      InformationCardXrds.retrieveX(doc, hrefStr, InformationCardXrds.xrdsListener);
      }
	, retrieveIcLoginServicePolicy : function(doc, hrefStr) {
		InformationCardXrds.retrieveX(doc, hrefStr, InformationCardXrds.icLoginServiceListener);
      }
	, retrieveX : function(doc, hrefStr, listenerO) {
      try {
         if (typeof(hrefStr) == 'string') {
            var i = hrefStr.indexOf("://");
            if (i == - 1) {
               // it is not an URL. Try to build an URL from the baseURI of the
               // document.
               var baseUri = doc.baseURI;
               if ((baseUri !== null) && (baseUri.length > 0)) {
                  if ((baseUri.length - 1) == baseUri.lastIndexOf('/')) {
                     // ends with /
                     hrefStr = baseUri + hrefStr;
                     }
                  else {
                     hrefStr = baseUri + '/' + hrefStr;
                     }
                  IdentitySelectorDiag.logMessage("retrieveX: href=", hrefStr);
                  }
               // else no baseUri
               }
            // else its an URL. Go ahead.
            }
         // else not string but document
         var sameSchemeAndDomain = InformationCardHelper.sameSchemeAndDomain(doc, hrefStr);
         if (sameSchemeAndDomain === true) {
            var req = Components.classes["@mozilla.org/xmlextras/xmlhttprequest;1"].createInstance();
            req.open('GET', hrefStr, true);
            req.setRequestHeader ('Content-Type', 'text/xml');
            req.overrideMimeType ('text/xml');
            var listener = new listenerO(doc, hrefStr);
            
            if (doc.__identityselector__.timer === undefined) {
	            var event = { notify: function(timer) { InformationCardStatusbar.onProgress(doc, null); } };
	            doc.__identityselector__.timerUrlArray = [];
	            doc.__identityselector__.timerUrlArray[hrefStr] = true;
	            doc.__identityselector__.timer = Components.classes["@mozilla.org/timer;1"].createInstance(Components.interfaces.nsITimer);
	            doc.__identityselector__.timer.initWithCallback( event, 1000, Components.interfaces.nsITimer.TYPE_REPEATING_SLACK);
	            IdentitySelectorDiag.logMessage("retrieveX", "started timer: " + doc.location.href);
            } else {
            	IdentitySelectorDiag.logMessage("retrieveX", "timer already started: " + doc.location.href);
            	// a statusbar time is already running. Just add the hrefStr to the list of outstanding requests
	            doc.__identityselector__.timerUrlArray[doc.__identityselector__.timerUrlArray.length] = hrefStr;
            }
            
//            var timeoutId = doc.defaultView.setInterval(function(){InformationCardStatusbar.onProgress(doc, null);}, 500, true);
//            var timeoutId = window.setInterval(function(){InformationCardStatusbar.onProgress(doc, null);}, 1000, true);
//            if ((InformationCardStatusbar !== undefined) &&(typeof(InformationCardStatusbar.onProgress) === "function")) {
//            	req.onprogress = function(aEvent) {
//            		IdentitySelectorDiag.logMessage("retrieveX", " onprogress");
//            		InformationCardStatusbar.onProgress(doc, aEvent);
//            	};
//            } else {
//            	IdentitySelectorDiag.logMessage("retrieveX", " InformationCardStatusbar is undefined: " + doc.location.href);
//            }
            req.onreadystatechange = function (aEvent) {
               if (req.readyState == 4) {
            	  IdentitySelectorDiag.logMessage("retrieveX", " deleting timer: " + hrefStr);
            	  delete doc.__identityselector__.timerUrlArray[hrefStr];
            	  if (doc.__identityselector__.timerUrlArray.length === 0) {
            		  InformationCardStatusbar.cancelTimer(doc);
            	  }
                  if (!req.responseXML) {
                     listener.onError (req.responseText);
                     return;
                     }
                  if (req.status != 200) {
                     listener.onError (req.statusText);
                     return;
                     }
                  listener.onReady(req.responseXML);
                  }
               };
            req.send(null);
            }
         }
      catch(e) {
         IdentitySelectorDiag.logMessage("retrieveXrds: ", e);
         }
      }

	  // ***********************************************************************
	  // Method: processHtmlLinkElements
	  // ***********************************************************************
	  , processHtmlLinkElements : function(event) {
//		  var doc = event.originalTarget;
		var browser = gBrowser.selectedTab.linkedBrowser;
			// browser is the XUL element of the browser that's just been selected
			var doc = gBrowser.selectedBrowser.contentDocument;

		  var dispatchEvents = true;
		  
		  IdentitySelectorDiag.logMessage("processHtmlLinkElements", " start: " + doc.location.href);
	      if( doc.wrappedJSObject !== undefined) {
	         doc = doc.wrappedJSObject;
	         }
	      if (IdentitySelector.disabled === true) {
	         IdentitySelectorDiag.logMessage("processHtmlLinkElements", " ID selector is disabled. Exiting");
	         return;
	         }
	      
	      if (!(doc instanceof HTMLDocument)) {
		      IdentitySelectorDiag.logMessage("processHtmlLinkElements", " no html document. Exiting");
	    	  return;
	      }
	      
	      if( doc.__identityselector__ === undefined) {
	         IdentitySelector.runInterceptScript(doc);
	         }
	      var linkElems = doc.getElementsByTagName( "LINK");
	      for( var i = 0; i < linkElems.length; i++) {
	         var linkElem = linkElems[ i];
	         var relStr = linkElem.getAttribute( "REL");
	         if( (relStr !== null) && (relStr === "xrds.metadata")) {
	            var hrefStr = linkElem.getAttribute( "HREF");
	            if (hrefStr === null) {
	               continue;
	               }
	            else {
	               IdentitySelectorDiag.logMessage("processHtmlLinkElements: href=", hrefStr);
	               if (doc.__identityselector__.xrds_metadata_href === undefined) {
	            	   doc.__identityselector__.xrds_metadata_href = hrefStr;
	            	   InformationCardXrds.retrieveXrds(doc, hrefStr);
	               }
//	               if( doc.__identityselector__.xrds === undefined) {
//		                  var data = doc.__identityselector__.data;
//		                  InformationCardXrds.retrieveXrds(doc, hrefStr);
////		                  if (doc.__identityselector__.data.xrds_metadata_href === undefined) {
////		                	  doc.__identityselector__.data.xrds_metadata_href = hrefStr;
////			                  InformationCardXrds.retrieveXrds(doc, hrefStr);
////		                  }	               
//		               }
	               else {
	                  IdentitySelectorDiag.logMessage("processHtmlLinkElements: already loaded: href=", hrefStr);
	                  }
	               return;
	               }
	            }
	         else {
	            continue;
	            }
	         }
	      }
	  
		, onLoad : function(event) {
			IdentitySelectorDiag.logMessage( "InformationCardXrds", "onLoad");
			window.removeEventListener( "load", function(event){gBrowser.addEventListener("load", InformationCardXrds.onLoad(event), true);}, false);
			
			window.addEventListener("DOMContentLoaded", 
					function(evnt) {InformationCardXrds.processHtmlLinkElements(evnt);}, false );
		}
		, onUnload : function(event) {
			IdentitySelectorDiag.logMessage( "InformationCardXrds", "onUnload");
			window.removeEventListener("DOMContentLoaded", 
					function(evnt) {InformationCardXrds.processHtmlLinkElements(evnt);}, false );
		}
};

try {
	IdentitySelectorDiag.logMessage( "InformationCardXrds", "start");
	
    window.addEventListener( "load", function(event){gBrowser.addEventListener("load", InformationCardXrds.onLoad, true);}, false);
           
    window.addEventListener( "unload", InformationCardXrds.onUnload, false);
} catch( e) {
	IdentitySelectorDiag.reportError( "InformationCardXrds window.addEventListener failed: ", e);
}
