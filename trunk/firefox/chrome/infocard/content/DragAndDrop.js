var InformationCardDragAndDrop = {

	extractDragData : function() {
		var cardId = null;
		try {
			var dragService = Cc["@mozilla.org/widget/dragservice;1"]
					.getService(Ci.nsIDragService);
			var dragSession = dragService.getCurrentSession();
			var sourceNode = dragSession.sourceNode;
			// Setup a transfer item to retrieve the file data
			var trans = Cc["@mozilla.org/widget/transferable;1"]
					.createInstance(Ci.nsITransferable);
			trans.addDataFlavor("application/x-informationcard+id");

			for ( var i = 0; i < dragSession.numDropItems; i++) {
				var uri = null;
				dragSession.getData(trans, i);
				var flavor = {}, data = {}, length = {};
				trans.getAnyTransferData(flavor, data, length);
				if (data) {
					var str = null;
					try {
						str = data.value.QueryInterface(Ci.nsISupportsString);
					} catch (ex) {
					}
					if (str !== null) {
						IdentitySelector.logMessage(
								"InformationCardDragAndDrop.extractDragData",
								"data=" + str);
						cardId = str;
						break;
						// only one
					} else {
						IdentitySelector.logMessage(
								"InformationCardDragAndDrop.extractDragData",
								"data=null");
					}
				}
			}
		} catch (e) {
			IdentitySelector.logMessage(
					"InformationCardDragAndDrop.extractDragData",e);
		}
		return cardId;
	},

	// ***********************************************************************
	// Method: onWindowDragDrop
	// ***********************************************************************
	onWindowDragDrop : function(event) {
		IdentitySelector.logMessage(
				"InformationCardDragAndDrop.onWindowDragDrop",
				"target.nodeName=", event.target.nodeName
						+ "\noriginalTarget.nodeName="
						+ event.originalTarget.nodeName
						+ "\ncurrentTarget.nodeName="
						+ event.currentTarget.nodeName);
		if (this.disabled === true) {
			IdentitySelector.logMessage(
					"InformationCardDragAndDrop.onWindowDragDrop",
					" ID selector is disabled. Exiting");
			return;
		}
		var fired = false;
		var target = event.target;
		if (target.wrappedJSObject) {
			target = target.wrappedJSObject;
		}
		var targetId = target.id;
		var doc = target.ownerDocument;
		var cardId = InformationCardDragAndDrop.extractDragData();
		
		if (cardId === null) {
			IdentitySelector.logMessage(
					"InformationCardDragAndDrop.onWindowDragDrop", "cardId == null");
			return;
		}
		
		if (targetId !== null) {
			IdentitySelector.logMessage(
					"InformationCardDragAndDrop.onWindowDragDrop", "targetId="+targetId);
			var object = InformationCardHelper.findRelatedObject(doc, targetId);
			if (object !== null) {
				// launch IdentitySelector with cardId
				IdentitySelector.logMessage(
						"InformationCardDragAndDrop.onWindowDragDrop",
						"launching IdentitySelector for card: " + cardId);
				doc.__identityselector__.targetElem = object;
				doc.__identityselector__.cardId = cardId;
				var form = target;
				while (form != null) {
					if (form.tagName != undefined && form.tagName == "FORM") {
						// the droptarget is inside a form -> submit it
						var trgt = form;
						var evnt = doc.createEvent("Event");
						evnt.initEvent("submit", true, true);
						trgt.dispatchEvent(evnt);
						fired = true;
						break;
					}
					form = form.parentNode;
				}
				if (!fired) {
					alert("The drop target is not inside a form\nDon't know how to submit token.");
				}
				// if (!fired) {
				// var trgt = doc;
				// var evnt = doc.createEvent( "Event");
				// evnt.initEvent( "CallIdentitySelector", true, true);
				// trgt.dispatchEvent( evnt);
				// }
				event.preventDefault();
				event.stopPropagation();
			} else {
				IdentitySelector.logMessage(
						"InformationCardDragAndDrop.onWindowDragDrop",
						"no object found for targetId=" + targetId
								+ " in document " + doc.location.href);
			}
		}
		if (fired == false) {
			IdentitySelector.logMessage(
					"InformationCardDragAndDrop.onWindowDragDrop", "cardId="+cardId);
			if (doc.__identityselector__.icLoginService != undefined) {
				IdentitySelector.logMessage(
						"InformationCardDragAndDrop.onWindowDragDrop",
						"doc.__identityselector__.icLoginService="
								+ doc.__identityselector__.icLoginService);
			}
			if (doc.__identityselector__.icLoginPolicy != undefined) {
				IdentitySelector.logMessage(
						"InformationCardDragAndDrop.onWindowDragDrop",
						"doc.__identityselector__.icLoginPolicy="
								+ doc.__identityselector__.icLoginPolicy);
			}
			doc.__identityselector__.cardId = cardId;
			InformationCardHelper.callIdentitySelector(doc);
			event.preventDefault();
			event.stopPropagation();
		}
		return false;
	}

};
