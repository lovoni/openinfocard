package org.xmldap.infocard.roaming;

import org.xmldap.exceptions.ParsingException;
import org.xmldap.ws.WSConstants;

import nu.xom.Element;
import nu.xom.Elements;

public class ManagedInformationCardPrivateData implements
		InformationCardPrivateData {
	String masterKey = null;

	public ManagedInformationCardPrivateData(Element managedInformationCardPrivateData) throws ParsingException {
	   	if ("InformationCardPrivateData".equals(managedInformationCardPrivateData.getLocalName())) {
	   		Elements elts = managedInformationCardPrivateData.getChildElements("MasterKey", WSConstants.INFOCARD_NAMESPACE);
	   		if (elts.size() != 1) {
	   			throw new ParsingException("Found " + elts.size() + " MasterKey elements in  ic:InformationCardPrivateData");
	   		} else {
	   			Element masterkeyElement = elts.get(0);
	   			masterKey = masterkeyElement.getValue();
	   		}
	   	} else {
	   		throw new ParsingException("expected ic:InformationCardPrivateData");
	   	}
	}
	
	public Element serialize() {
        Element informationCardPrivateData = new Element("InformationCardPrivateData", WSConstants.INFOCARD_NAMESPACE);

        Element masterKeyElt = new Element("MasterKey", WSConstants.INFOCARD_NAMESPACE);
        masterKeyElt.appendChild(masterKey);
        informationCardPrivateData.appendChild(masterKeyElt);
        return informationCardPrivateData;
	}
}
