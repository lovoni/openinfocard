/*
 * Copyright (c) 2006, Chuck Mortimore - xmldap.org
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the University of California, Berkeley nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.xmldap.saml;

import nu.xom.Element;
import org.xmldap.exceptions.KeyStoreException;
import org.xmldap.exceptions.SerializationException;
import org.xmldap.util.KeystoreUtil;
import org.xmldap.util.RandomGUID;
import org.xmldap.util.XSDDateTime;
import org.xmldap.ws.WSConstants;
import org.xmldap.xml.Serializable;
import org.xmldap.xmldsig.AysmmetricKeyInfo;


public class SAMLAssertion implements Serializable {

    private static final String MAJOR_VERSION = "1";
    private static final String MINOR_VERSION = "1";

    private String assertionID;
    private String issuer;
    private String issueInstant;

    private Conditions conditions;
    private AttributeStatement attributeStatement;


    public SAMLAssertion() {

        RandomGUID guidGen = new RandomGUID();
        assertionID = "uuid-" + guidGen.toString();
        issuer = "http://schemas.microsoft.com/ws/2005/05/identity/issuer/self";
        XSDDateTime dateTime = new XSDDateTime();
        issueInstant = dateTime.getDateTime();

    }

    public void setConditions(Conditions conditions) {
        this.conditions = conditions;
    }

    public void setAttributeStatement(AttributeStatement attributeStatement) {
        this.attributeStatement = attributeStatement;
    }

    private Element getSAMLAssertion() throws SerializationException {
        Element assertion = new Element(WSConstants.SAML_PREFIX + ":Assertion", WSConstants.SAML11_NAMESPACE);
        nu.xom.Attribute assertionIDAttr = new nu.xom.Attribute("AssertionID", assertionID);
        nu.xom.Attribute issuerAtr = new nu.xom.Attribute("Issuer", issuer);
        nu.xom.Attribute issueInstantAttr = new nu.xom.Attribute("IssueInstant", issueInstant);
        nu.xom.Attribute majorVersion = new nu.xom.Attribute("MajorVersion", MAJOR_VERSION);
        nu.xom.Attribute minorVersion = new nu.xom.Attribute("MinorVersion", MINOR_VERSION);
        assertion.addAttribute(assertionIDAttr);
        assertion.addAttribute(issuerAtr);
        assertion.addAttribute(issueInstantAttr);
        assertion.addAttribute(majorVersion);
        assertion.addAttribute(minorVersion);
        assertion.appendChild(conditions.serialize());
        assertion.appendChild(attributeStatement.serialize());
        return assertion;
    }


    public String toXML() throws SerializationException {

        Element assertion = serialize();
        return assertion.toXML();
    }

    public Element serialize() throws SerializationException {

        return getSAMLAssertion();
    }


    public static void main(String[] args) {

        //Get my keystore
        KeystoreUtil keystore = null;
        try {
            keystore = new KeystoreUtil("/Users/cmort/build/infocard/conf/xmldap.jks", "storepassword");
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }


        Conditions conditions = new Conditions(10);

        AysmmetricKeyInfo keyInfo = new AysmmetricKeyInfo(keystore, "xmldap");
        Subject subject = new Subject(keyInfo);
        Attribute given = new Attribute("givenname", "http://schemas.microsoft.com/ws/2005/05/identity/claims/GivenName", "Chuck");
        Attribute sur = new Attribute("surname", "http://schemas.microsoft.com/ws/2005/05/identity/claims/SurName", "Mortimore");
        Attribute email = new Attribute("email", "http://schemas.microsoft.com/ws/2005/05/identity/claims/EmailAddress", "cmortspam@gmail.com");
        AttributeStatement statement = new AttributeStatement();
        statement.setSubject(subject);
        statement.addAttribute(given);
        statement.addAttribute(sur);
        statement.addAttribute(email);

        SAMLAssertion assertion = new SAMLAssertion();
        assertion.setConditions(conditions);
        assertion.setAttributeStatement(statement);

        try {
            System.out.println(assertion.toXML());
        } catch (SerializationException e) {
            e.printStackTrace();
        }


    }
}
