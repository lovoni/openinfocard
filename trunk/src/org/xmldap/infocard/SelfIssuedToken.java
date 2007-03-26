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
 *     * Neither the names xmldap, xmldap.org, xmldap.com nor the
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

package org.xmldap.infocard;

import nu.xom.Element;
import org.xmldap.crypto.CryptoUtils;
import org.xmldap.exceptions.SerializationException;
import org.xmldap.exceptions.SigningException;
import org.xmldap.saml.*;
import org.xmldap.xml.Serializable;
import org.xmldap.xmldsig.AsymmetricKeyInfo;
import org.xmldap.xmldsig.EnvelopedSignature;
import org.xmldap.xmldsig.KeyInfo;
import org.xmldap.xmldsig.SymmetricKeyInfo;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.Vector;

/**
 * SelfIssuedToken allows you to create Self issued tokens for passing to an RP
 */
public class SelfIssuedToken implements Serializable {

	private String namespacePrefix = null;

	private String givenName;

	private String surname;

	private String emailAddress;

	private String streetAddress;

	private String locality;

	private String stateOrProvince;

	private String postalCode;

	private String country;

	private String primaryPhone;

	private String otherPhone;

	private String mobilePhone;

	private String dateOfBirth;

	private String privatePersonalIdentifier;

	private String gender;

	private X509Certificate signingCert;

	private PrivateKey signingKey;

	private X509Certificate relyingPartyCert;

	private int nowPlus = 10; //default to 10 minutes

	private int nowMinus = 10; //default to 5 minutes

	private boolean asymmetric = false; //default to symmetric key

	public SelfIssuedToken(X509Certificate relyingPartyCert,
			X509Certificate signingCert, PrivateKey signingKey) {
		this.signingCert = signingCert;
		this.signingKey = signingKey;
		this.relyingPartyCert = relyingPartyCert;
		namespacePrefix = org.xmldap.infocard.Constants.IC_NAMESPACE_PREFIX; // default is the new (Autumn 2006) namespace
	}

	public void setNamespacePrefix(String namespacePrefix) {
		this.namespacePrefix = namespacePrefix;
	}

	public String getNamespacePrefix() {
		return namespacePrefix;
	}

	//    public int getValidityPeriod() {
	//        return nowPlus;
	//    }

	public void setValidityPeriod(int nowMinus, int nowPlus) {
		this.nowMinus = nowMinus;
		this.nowPlus = nowPlus;
	}

	public String getGivenName() {
		return givenName;
	}

	public void setGivenName(String givenName) {
		this.givenName = givenName;
	}

	public String getSurname() {
		return surname;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	public String getStreetAddress() {
		return streetAddress;
	}

	public void setStreetAddress(String streetAddress) {
		this.streetAddress = streetAddress;
	}

	public String getLocality() {
		return locality;
	}

	public void setLocality(String locality) {
		this.locality = locality;
	}

	public String getStateOrProvince() {
		return stateOrProvince;
	}

	public void setStateOrProvince(String stateOrProvince) {
		this.stateOrProvince = stateOrProvince;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getPrimaryPhone() {
		return primaryPhone;
	}

	public void setPrimaryPhone(String primaryPhone) {
		this.primaryPhone = primaryPhone;
	}

	public String getOtherPhone() {
		return otherPhone;
	}

	public void setOtherPhone(String otherPhone) {
		this.otherPhone = otherPhone;
	}

	public String getMobilePhone() {
		return mobilePhone;
	}

	public void setMobilePhone(String mobilePhone) {
		this.mobilePhone = mobilePhone;
	}

	public String getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(String dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public String getPrivatePersonalIdentifier() {
		return privatePersonalIdentifier;
	}

	public void setPrivatePersonalIdentifier(String privatePersonalIdentifier) {
		this.privatePersonalIdentifier = privatePersonalIdentifier;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public void useAsymmetricKey() {

		this.asymmetric = true;

	}

	private void addAttribute(Vector attributes, String name, String uri, String value) {
		if (value != null) {
			Attribute attr = new Attribute(name, uri, value);
			attributes.add(attr);
		}
	}
	
	public Element getSelfIssuedToken() throws SerializationException {

		Conditions conditions = new Conditions(nowMinus, nowPlus);
		KeyInfo keyInfo = null;
		if (asymmetric) {

			if (signingCert == null)
				throw new SerializationException(
						"You did not provide a certificate for use with asymetric keys");
			keyInfo = new AsymmetricKeyInfo(signingCert);

		} else {

			byte[] secretKey;
			try {
				secretKey = CryptoUtils.genKey(128);
			} catch (org.xmldap.exceptions.CryptoException e) {
				throw new SerializationException(e);
			}

			if (relyingPartyCert != null) {
				PublicKey publicKey = relyingPartyCert.getPublicKey();
				keyInfo = new SymmetricKeyInfo(publicKey, secretKey);
			} else {
				throw new SerializationException(
						"You did not provide the relying party cert");
			}

		}

		Subject subject = new Subject(keyInfo);

		Vector attributes = new Vector();

		addAttribute(attributes, "givenname", namespacePrefix + "givenname",
				givenName);
		addAttribute(attributes, "surname", namespacePrefix + "surname",
				surname);
		addAttribute(attributes, "emailaddress", namespacePrefix
				+ "emailaddress", emailAddress);
		addAttribute(attributes, "streetaddress", namespacePrefix
				+ "streetaddress", streetAddress);
		addAttribute(attributes, "locality", namespacePrefix + locality,
				locality);
		addAttribute(attributes, "stateorprovince", namespacePrefix
				+ "stateorprovince", stateOrProvince);
		addAttribute(attributes, "postalcode", namespacePrefix + "postalcode",
				postalCode);
		addAttribute(attributes, "country", namespacePrefix + "country",
				country);
		addAttribute(attributes, "primaryphone", namespacePrefix
				+ "primaryphone", primaryPhone);
		addAttribute(attributes, "otherphone", namespacePrefix + "otherphone",
				otherPhone);
		addAttribute(attributes, "mobilephone",
				namespacePrefix + "mobilephone", mobilePhone);
		addAttribute(attributes, "dateofbirth",
				namespacePrefix + "dateofbirth", dateOfBirth);
		addAttribute(attributes, "privatepersonalidentifier", namespacePrefix
				+ "privatepersonalidentifier", privatePersonalIdentifier);
		addAttribute(attributes, "gender", namespacePrefix + "gender", gender);

		AttributeStatement statement = new AttributeStatement();
		statement.setSubject(subject);

		Iterator iter = attributes.iterator();
		while (iter.hasNext()) {

			statement.addAttribute((Attribute) iter.next());

		}

		SAMLAssertion assertion = new SAMLAssertion();
		assertion.setConditions(conditions);
		assertion.setAttributeStatement(statement);

		//make this support multiple signing modes
		EnvelopedSignature signer = new EnvelopedSignature(keyInfo,	signingKey);

		Element signedXML = null;
		try {
			signedXML = signer.sign(assertion.serialize());
		} catch (SigningException e) {
			throw new SerializationException("Error signing assertion", e);
		}

		return signedXML;

	}

	public String toXML() throws SerializationException {

		Element sit = serialize();
		return sit.toXML();

	}

	public Element serialize() throws SerializationException {
		return getSelfIssuedToken();
	}

	public static String getDataValue(Element data, String claim)
	{
		Element nameElm = data.getFirstChildElement(claim);
		if (nameElm != null)
			return nameElm.getValue();
		return "";
	}

	public static SelfIssuedToken setTokenClaims(Element data,
			SelfIssuedToken token, String claims) {
		// the argument to indexOf is a kind of shorthand...
		// should we use the complete string?
		if (claims.indexOf("givenname") != -1) {
			String value = getDataValue(data, "givenname");
			if ((value != null) && !value.equals("")) {
				token.setGivenName(value);
			}
		}
		if (claims.indexOf("surname") != -1) {
			String value = getDataValue(data, "surname");
			if ((value != null) && !value.equals("")) {
				token.setSurname(value);
			}
		}
		if (claims.indexOf("emailaddress") != -1) {
			String value = getDataValue(data, "emailaddress");
			if ((value != null) && !value.equals("")) {
				token.setEmailAddress(value);
			}
		}
		if (claims.indexOf("streetladdress") != -1) {
			String value = getDataValue(data, "streetladdress");
			if ((value != null) && !value.equals("")) {
				token.setStreetAddress(value);
			}
		}
		if (claims.indexOf("locality") != -1) {
			String value = getDataValue(data, "locality");
			if ((value != null) && !value.equals("")) {
				token.setLocality(value);
			}
		}
		if (claims.indexOf("stateorprovince") != -1) {
			String value = getDataValue(data, "stateorprovince");
			if ((value != null) && !value.equals("")) {
				token.setStateOrProvince(value);
			}
		}
		if (claims.indexOf("postalcode") != -1) {
			String value = getDataValue(data, "postalcode");
			if ((value != null) && !value.equals("")) {
				token.setPostalCode(value);
			}
		}
		if (claims.indexOf("country") != -1) {
			String value = getDataValue(data, "country");
			if ((value != null) && !value.equals("")) {
				token.setCountry(value);
			}
		}
		if (claims.indexOf("primaryphone") != -1) {
			String value = getDataValue(data, "primaryphone");
			if ((value != null) && !value.equals("")) {
				token.setPrimaryPhone(value);
			}
		}
		if (claims.indexOf("otherphone") != -1) {
			String value = getDataValue(data, "otherphone");
			if ((value != null) && !value.equals("")) {
				token.setOtherPhone(value);
			}
		}
		if (claims.indexOf("mobilephone") != -1) {
			String value = getDataValue(data, "mobilephone");
			if ((value != null) && !value.equals("")) {
				token.setMobilePhone(value);
			}
		}
		if (claims.indexOf("dateofbirth") != -1) {
			String value = getDataValue(data, "dateofbirth");
			if ((value != null) && !value.equals("")) {
				token.setDateOfBirth(value);
			}
		}
		if (claims.indexOf("gender") != -1) {
			String value = getDataValue(data, "gender");
			if ((value != null) && !value.equals("")) {
				token.setGender(value);
			}
		}
		return token;
	}

}
