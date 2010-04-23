function sbDebugObject(prefix, object, indent) {
  var msg = "";
  var count = 0;
  //if (indent > 3) return;
  var pre = "";
  for (var j=0; j<indent; j++) { pre += '\t'; }
  for (var i in object) {
    if (object.hasOwnProperty(i)) {
  
      var value = object[i];
      if (typeof(value) == 'object') {
        //sbDebugObject(value, indent+1);
        msg += pre + i + ' type=' + typeof(value) + ':' + value + '\n';
      } else if ((typeof(value) == 'string') || ((typeof(value) == 'boolean')) || ((typeof(value) == 'number'))) {
        msg += pre + ':' + i + '=' + value + '\n';
      } else {
        msg += pre + i + ' type=' + typeof(value) + '\n';
      }
    }
  }
  IdentitySelectorDiag.logMessage(prefix, msg);
}

function getDoc(){
  var mainWindow = window.QueryInterface(Components.interfaces.nsIInterfaceRequestor)
  .getInterface(Components.interfaces.nsIWebNavigation)
  .QueryInterface(Components.interfaces.nsIDocShellTreeItem)
  .rootTreeItem
  .QueryInterface(Components.interfaces.nsIInterfaceRequestor)
  .getInterface(Components.interfaces.nsIDOMWindow);
  var browser = mainWindow.gBrowser;
  var doc = browser.contentDocument;
  IdentitySelectorDiag.logMessage("sidebar", "sidebarLoad mainwindow href=" + doc.location.href );
  if( doc.wrappedJSObject)
  {
    doc = doc.wrappedJSObject;
  }
  return doc;
}

function getData(doc){
  if(!doc.__identityselector__) {
    IdentitySelectorDiag.logMessage("sidebar", "sidebarLoad __identityselector__ is undefined");
  }
  if(!doc.__identityselector__.data) {
    IdentitySelectorDiag.logMessage("sidebar", "sidebarLoad __identityselector__.data is undefined");
  }
  var data = doc.__identityselector__.data;
  sbDebugObject("getData doc.__identityselector__.data: ", doc.__identityselector__.data,0);
  return data;
}

function createPolicy(doc, data){
  var policy = {};
  //policy.protocol = ;
  if (data.issuer) {
    policy.issuer = data.issuer;
  }
  if (data.recipient) {
    policy.recipient = data.recipient;
  }
  if (data.requiredClaims) {
    policy.requiredClaims = data.requiredClaims;
  }
  if (data.optionalClaims) {
    policy.optionalClaims = data.optionalClaims;
  }
  if (data.tokenType) {
    policy.tokenType = data.tokenType;
  }
  if (data.privacyUrl) {
    policy.privacyUrl = data.privacyUrl;
  }
  if (data.privacyVersion) {
    policy.privacyVersion = data.privacyVersion;
  }
  if (data.issuerPolicy) {
    policy.issuerPolicy = data.issuerPolicy;
  }
  return policy;
}

function getDer(cert,win){

  var length = {};
  var derArray = cert.getRawDER(length);
  var certBytes = '';
  for (var i = 0; i < derArray.length; i++) {
      certBytes = certBytes + String.fromCharCode(derArray[i]);
  }
  return win.btoa(certBytes);

}

function sidebarDblClick(event) {
  IdentitySelectorDiag.logMessage("sidebarDblClick", "start");
  var choice = event.originalTarget;
  if (!choice) {
    IdentitySelectorDiag.reportError("sidebarDblClick", "internal error: sidebarDblClick: choice == undefined");
  }
  var selectedCardId = choice.getAttribute("cardid");
  if (!selectedCardId) {
    IdentitySelectorDiag.reportError("sidebarDblClick", "internal error: sidebarDblClick: selectedCardId == undefined");
  }
  IdentitySelectorDiag.logMessage("sidebarDblClick", "selectedCardId="+selectedCardId);
  var choosenCard = getCard(selectedCardId);
  if (choosenCard === null) {
    IdentitySelectorDiag.reportError("sidebarDblClick", "internal error: card not found: " + selectedCardId);
    return;
  }
  
  selectedCard = choosenCard;

  var doc = getDoc();
  if (!doc) { return; }
  IdentitySelectorDiag.logMessage("sidebarDblClick", "doc.location.href=" + doc.location.href);
  
  var objElems = doc.getElementsByTagName("OBJECT");
  for(var iLoop = 0; iLoop < objElems.length; iLoop++) {
    var objElem = objElems[iLoop];
    var objTypeStr = objElem.getAttribute("TYPE");
   
    if( (objTypeStr !== null) &&
         (objTypeStr == "application/x-informationcard"))
    {
      var selectorEvent = doc.createEvent( "Event");
      selectorEvent.initEvent("CallIdentitySelector", true, true);
      doc.__identityselector__.targetElem = objElem;
      doc.__identityselector__.cardid = selectedCardId;
      doc.dispatchEvent( selectorEvent);
    }
  }
  
//  var data = getData(doc);
//  if (!data) { return; }
//  sbDebugObject("sidebarDblClick data: ", data,0);
//  var policy = createPolicy(doc, data);
//  sbDebugObject("sidebarDblClick policy: ", policy,0)
//  ;
//  handleOK(policy, selectedCard);
}

function sidebarLoad(){
  // infocard: sidebarLoad start. href=chrome://infocard/content/cardSidebar.xul
  IdentitySelectorDiag.logMessage("sidebar", "sidebarLoad start");

  var stringsBundle = document.getElementById("string-bundle");

  var doc = getDoc();
  if (!doc) { return; }
  IdentitySelectorDiag.logMessage("sidebar", "sidebarLoad doc.location.href=" + doc.location.href);
  var data = getData(doc);
  if (!data) { return; }
  sbDebugObject("sidebarLoad data: ", data,0);
  var policy = createPolicy(doc, data);
  sbDebugObject("sidebarLoad policy: ", policy,0)

  var extraParamsCardId;
  if (doc.__identityselector__.cardId) {
    extraParamsCardId = doc.__identityselector__.cardId;
  }

  var rpIdentifier = null;
  var sslCert = InformationCardHelper.getSSLCertFromDocument(doc);
  if (sslCert) {
    policy.cert = getDer(sslCert,window);
    policy.cn = sslCert.commonName;
    rpIdentifier = computeRpIdentifier(policy.cert);
  } else {
    IdentitySelectorDiag.logMessage("sidebar", "sslCert is not set.");
  }
  IdentitySelectorDiag.logMessage("sidebar", "rpIdentifier=" + rpIdentifier);

  var cardFile = CardstoreToolkit.readCardStore();
  var cardArea = document.getElementById("cardselection");
  var latestCard;
  var selectMe;
  var beenThere = false;
  var count = 0;
  var scrolledIntoView = false;
  for each (c in cardFile.infocard) {
    var cardClass = "contact";
    if (policy != null) {
      cardClass = computeCardClass(c, policy);
    }
    latestCard = createItem(c, cardClass, null, sidebarDblClick);
    selectMe = c;
    cardArea.appendChild(latestCard);
    count++;
  
    if (!beenThere) {
      if (rpIdentifier != null) {
        for each (rpId in c.rpIds) {
          IdentitySelectorDiag.logMessage("sidebar", "RpId:" + rpId + " RpIdentifier:" + rpIdentifier);
          if (rpId == rpIdentifier) {
            //debug("been there at: " + policy["cn"]);
            beenThere = true;
            if (scrolledIntoView == false) {
              if (cardArea.scrollBoxObject) {
                  var xpcomInterface = cardArea.scrollBoxObject.QueryInterface(Components.interfaces.nsIScrollBoxObject);
                  xpcomInterface.ensureElementIsVisible(latestCard);
              }
              scrolledIntoView = true;
            }
            break;
          }
        }
      }
    }
  }
    
  if ( count != 0) {
    if (policy != null) {
        var label = document.getElementById("notify");
        if (label != null) {
      var site = "Unknown";
      if (policy.hasOwnProperty("cn")) {
        site = policy["cn"];
      } else if (policy.hasOwnProperty("url")) {
        site = policy["url"];
      }
          var please = stringsBundle.getFormattedString('pleaseselectacard', [site]);
          label.setAttribute("value", please);
        }
    } else {
        var label = document.getElementById("notify");
        if (label != null) {
          label.setAttribute("value", "card management");
        }
    }
} else {
      var label = document.getElementById("notify");
      if (label != null) {
    var button = stringsBundle.getString('newcard');
        var youdont = stringsBundle.getFormattedString('youdonthaveanycards', [button]);
        label.setAttribute("value", youdont);
      }
  }
  if (policy != null) {
    var serializedPolicy = JSON.stringify(policy);
    if (TokenIssuer.initialize() == true) {
      var issuerLogoURL = TokenIssuer.getIssuerLogoURL(serializedPolicy);
      IdentitySelectorDiag.logMessage("sidebar", "issuerLogoURL=" + issuerLogoURL);
      if (issuerLogoURL != undefined) {
        var issuerlogo = document.getElementById("issuerlogo");
        issuerlogo.src = issuerLogoURL;
        issuerlogo.hidden = false;
        var issuerlogo_label = document.getElementById("issuerlogo_label");
              issuerlogo_label.hidden = false;
        var issuer_hbox = document.getElementById("issuer_hbox");
              issuer_hbox.hidden = false;
      }
    } else {
      IdentitySelectorDiag.logMessage("sidebar", "load: error initializing the TokenIssuer");
      IdentitySelectorDiag.logMessage("sidebar", "load: window.java=" + window.java + " window.document.location.href=" + window.document.location.href);
// alert("Error: Initializing the TokenIssuer");
      // return;
    }
  }
  if (policy != null) {
      var privacyUrl = policy["privacyUrl"];
        if (privacyUrl != null) {
      IdentitySelectorDiag.logMessage("sidebar", "privacyUrl " + privacyUrl);
             var showPrivacyStatementElm = document.getElementById('privacy_label');
             showPrivacyStatementElm.addEventListener("click", showPrivacyStatement, false);
             showPrivacyStatementElm.hidden = false;
      }
  }
  if (!beenThere) {
    if (policy != null) {
       if (policy.hasOwnProperty("cn")) {
         IdentitySelectorDiag.logMessage("sidebar", "never been here: " + policy["cn"]);
       } else if (policy.hasOwnProperty("url")) {
         IdentitySelectorDiag.logMessage("sidebar", "never been here: " + policy["url"]);
       } else {
         IdentitySelectorDiag.logMessage("sidebar", "never been here");
       }
    }
    if (policy !== null) { // no card management
      var firstTimeVisit = document.getElementById('firstTimeVisit');
      if (firstTimeVisit != null) {
        var labelText;
        try {
          labelText = stringsBundle.getString('firsttimevisit');
        } catch (e) {
         IdentitySelectorDiag.logMessage("sidebar", "firstTime: exception=" + e);
         labelText = "This is your first visit to this site. Think!";
        }
        IdentitySelectorDiag.logMessage("sidebar", "firstTime: " + labelText);
        firstTimeVisit.setAttribute("value", labelText);
        var firstTimeVisitBox = document.getElementById('firstTimeVisitBox');
        firstTimeVisitBox.setAttribute("hidden", "false");
      }
    } else { // else card management
      var firstTimeVisitBox = document.getElementById('firstTimeVisitBox');
      if (firstTimeVisitBox !== null) {
        firstTimeVisitBox.setAttribute("hidden", "false");
      } // else sidebar
    }
  }
  
  if (extraParamsCardId) {
    var choosenCard = getCard(extraParamsCardId);
    if ((choosenCard === undefined) || (choosenCard === null)) {
      Components.utils.reportError("sidebarLoad: extraParamsCardId=" + extraParamsCardId + " but card not found");
      alert("internal error: card not found in cardstore.");
      window.close();
      return;
    }
    setCard(choosenCard);
    var chossenCardElement = document.getElementById(extraParamsCardId);
    if (chossenCardElement !== null) {
      var xpcomInterface = cardArea.scrollBoxObject.QueryInterface(Components.interfaces.nsIScrollBoxObject);
      xpcomInterface.ensureElementIsVisible(chossenCardElement);
    }
  } else {
     IdentitySelectorDiag.logMessage("sidebar", "extraParamsCardId === undefined");
  }

}

//function sidebardOnLoad() {
//  IdentitySelectorDiag.logMessage("sidebar", "onLoad event");
// }
//
try {
  IdentitySelectorDiag.logMessage("sidebar", "start");
  
// var mainWindow =
// window.QueryInterface(Components.interfaces.nsIInterfaceRequestor)
// .getInterface(Components.interfaces.nsIWebNavigation)
// .QueryInterface(Components.interfaces.nsIDocShellTreeItem)
// .rootTreeItem
// .QueryInterface(Components.interfaces.nsIInterfaceRequestor)
// .getInterface(Components.interfaces.nsIDOMWindow);
//
// window.addEventListener( "load",
// function(event){mainWindow.gBrowser.addEventListener("load", sidebardOnLoad,
// true);}, false);
           
    // window.addEventListener( "unload", IcXrdsStartHelper.onUnload, false);
} catch( e) {
  IdentitySelectorDiag.reportError( "sidebar: ", e);
}

