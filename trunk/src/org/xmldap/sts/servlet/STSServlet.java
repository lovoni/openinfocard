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

package org.xmldap.sts.servlet;

import nu.xom.*;
import org.xmldap.util.*;
import org.xmldap.exceptions.KeyStoreException;
import org.xmldap.exceptions.SerializationException;
import org.xmldap.crypto.CryptoUtils;
import org.xmldap.ws.WSConstants;
import org.xmldap.infocard.SelfIssuedToken;
import org.xmldap.infocard.ManagedToken;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.text.MessageFormat;
import java.security.interfaces.RSAPrivateKey;
import java.security.cert.X509Certificate;

import net.sourceforge.lightcrypto.SafeObject;


public class STSServlet  extends HttpServlet {


    RSAPrivateKey key;
    X509Certificate cert;
    private ServletUtil _su;

    public void init() throws ServletException {

        //Get my keystore
       try {

	   _su = new ServletUtil(getServletConfig());
	   KeystoreUtil keystore = _su.getKeystore();

           key = (RSAPrivateKey) _su.getPrivateKey();
           cert = _su.getCertificate();

       } catch (KeyStoreException e) {
           e.printStackTrace();
       }

    }



    private Bag parseToken(Element tokenXML) throws ParsingException{

        Bag tokenElements = new Bag();

        XPathContext context = new XPathContext();
        context.addNamespace("s","http://www.w3.org/2003/05/soap-envelope");
        context.addNamespace("a", "http://www.w3.org/2005/08/addressing");
        context.addNamespace("wst", "http://schemas.xmlsoap.org/ws/2005/02/trust");
        context.addNamespace("wsid","http://schemas.microsoft.com/ws/2005/05/identity");
        context.addNamespace("o","http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd");
        context.addNamespace("u","http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd");

        Nodes uns = tokenXML.query("//o:Username",context);
        Element un = (Element) uns.get(0);
        String userName = un.getValue();
        System.out.println("userName: " + userName);
        tokenElements.put("userName", userName);


        Nodes pws = tokenXML.query("//o:Password",context);
        Element pw = (Element) pws.get(0);
        String password = pw.getValue();
        System.out.println("password: " + password);
        tokenElements.put("password", password);

        return tokenElements;

    }


    /*
    <wst:RequestSecurityToken Context="ProcessRequestSecurityToken" xmlns:wst="http://schemas.xmlsoap.org/ws/2005/02/trust">
    <wsid:InformationCardReference xmlns:wsid="http://schemas.xmlsoap.org/ws/2005/05/identity">
        <wsid:CardId>https://xmldap.org/sts/card/2E55ECBE-1423-38AE-DA05-0B27F44907F8</wsid:CardId>
        <wsid:CardVersion>1</wsid:CardVersion>
    </wsid:InformationCardReference>
    <wst:Claims>
        <wsid:ClaimType Uri="http://schemas.microsoft.com/ws/2005/05/identity/claims/givenname"
                        xmlns:wsid="http://schemas.xmlsoap.org/ws/2005/05/identity"/>
        <wsid:ClaimType Uri="http://schemas.microsoft.com/ws/2005/05/identity/claims/surname"
                        xmlns:wsid="http://schemas.xmlsoap.org/ws/2005/05/identity"/>
        <wsid:ClaimType Uri="http://schemas.microsoft.com/ws/2005/05/identity/claims/emailaddress"
                        xmlns:wsid="http://schemas.xmlsoap.org/ws/2005/05/identity"/>
    </wst:Claims>
    <wst:KeyType>http://schemas.xmlsoap.org/ws/2005/05/identity/NoProofKey</wst:KeyType>
    <wst:TokenType>urn:oasis:names:tc:SAML:1.0:assertion</wst:TokenType>
    <wsid:RequestDisplayToken xml:lang="en" xmlns:wsid="http://schemas.xmlsoap.org/ws/2005/05/identity"/>
</wst:RequestSecurityToken>

    */

    private Bag parseRequest(Element requestXML) throws ParsingException{

        Bag requestElements = new Bag();


        XPathContext context = new XPathContext();
        context.addNamespace("s","http://www.w3.org/2003/05/soap-envelope");
        context.addNamespace("a", "http://www.w3.org/2005/08/addressing");
        context.addNamespace("wst", "http://schemas.xmlsoap.org/ws/2005/02/trust");
        context.addNamespace("wsid","http://schemas.xmlsoap.org/ws/2005/05/identity");
        context.addNamespace("o","http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd");
        context.addNamespace("u","http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd");

        Nodes cids = requestXML.query("//wsid:CardId",context);
        Element cid = (Element) cids.get(0);
        String cardId = cid.getValue();
        System.out.println("cardId: " + cardId);
        requestElements.put("cardId", cardId);


        Nodes cvs = requestXML.query("//wsid:CardVersion",context);
        Element cv = (Element) cvs.get(0);
        String cardVersion = cv.getValue();
        System.out.println("CardVersion: " + cardVersion);
        requestElements.put("cardVersion", cardVersion);


        Nodes claims = requestXML.query("//wsid:ClaimType",context);
        for (int i = 0; i < claims.size(); i++ ) {

            Element claimElm = (Element)claims.get(i);
            Attribute uri = claimElm.getAttribute("Uri");
            String claim = uri.getValue();
            System.out.println(claim);
            requestElements.put("claim", claim);

        }

        Nodes kts = requestXML.query("//wst:KeyType",context);
        Element kt = (Element) kts.get(0);
        String keyType = kt.getValue();
        System.out.println("keyType: " + keyType);
        requestElements.put("keyType", keyType);

        Nodes tts = requestXML.query("//wst:TokenType",context);
        Element tt = (Element) tts.get(0);
        String tokenType = tt.getValue();
        System.out.println("tokenType: " + tokenType);
        requestElements.put("tokenType", tokenType);

        return requestElements;


    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        System.out.println("STS got a request");
        int contentLen = request.getContentLength();

        String requestXML = null;
        if (contentLen > 0) {

            DataInputStream inStream = new DataInputStream(request.getInputStream());
            byte[] buf = new byte[contentLen];
            inStream.readFully(buf);
            requestXML = new String(buf);

            System.out.println("STS Request:");
            System.out.println(requestXML);

        }

        //let's make a doc
        Builder parser = new Builder();
        Document req = null;
        try {
            req = parser.build(requestXML, "");
        } catch (ParsingException e) {
            e.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();
        }



        System.out.println("We have a doc");

        XPathContext context = new XPathContext();
        context.addNamespace("s","http://www.w3.org/2003/05/soap-envelope");
        context.addNamespace("a", "http://www.w3.org/2005/08/addressing");
        context.addNamespace("o","http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd");
        context.addNamespace("u","http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd");
        context.addNamespace("wst", "http://schemas.xmlsoap.org/ws/2005/02/trust");


        Nodes tokenElm = req.query("//o:UsernameToken",context);
        Element token = (Element) tokenElm.get(0);
        System.out.println("Token:" + token.toXML());


        Nodes rsts = req.query("//wst:RequestSecurityToken",context);
        Element rst = (Element) rsts.get(0);
        System.out.println("RST: " + rst.toXML());


        Bag tokenElements = null;
        try {
            tokenElements = parseToken(token);
        } catch (ParsingException e) {
            e.printStackTrace();
            //TODO - SOAP Fault
        }


        Bag requestElements = null;
        try {
            requestElements = parseRequest(rst);
        } catch (ParsingException e) {
            e.printStackTrace();
            //TODO - SOAP Fault
        }




        //TODO - authenticate!


        String stsResponse = issue(requestElements);

        response.setContentType("application/soap+xml; charset=\"utf-8\"");
        response.setContentLength(stsResponse.length());
        System.out.println("STS Response:\n " + stsResponse);
        PrintWriter out = response.getWriter();
        out.println(stsResponse);
        out.flush();
        out.close();

    }

    private String issue(Bag requestElements) throws IOException {

    /*  OLD TEST ISSUE
	String issuePath = _su.getIssueFilePathString();

    if (issuePath == null) {
	    issuePath = "/home/cmort/issue.xml";
	}
        InputStream in = new FileInputStream(issuePath);

        StringBuffer issueBuff = new StringBuffer();
        DataInputStream ins = new DataInputStream(in);

        while (in.available() !=0) {
            issueBuff.append(ins.readLine());
        }

        in.close();
        ins.close();

        MessageFormat issueResponse = new MessageFormat(issueBuff.toString());

        XSDDateTime now = new XSDDateTime();
        XSDDateTime later = new XSDDateTime(10);   //one week -what's up with window's time???
        String[] args = {messageId, now.getDateTime(), later.getDateTime()};

        return issueResponse.format(args);


    } */


        Element envelope = new Element(WSConstants.SOAP_PREFIX + ":Envelope", WSConstants.SOAP12_NAMESPACE);
        envelope.addNamespaceDeclaration(WSConstants.WSA_PREFIX, WSConstants.WSA_NAMESPACE_05_08);
        envelope.addNamespaceDeclaration(WSConstants.WSU_PREFIX, WSConstants.WSU_NAMESPACE);
        envelope.addNamespaceDeclaration(WSConstants.WSSE_PREFIX, WSConstants.WSSE_NAMESPACE_OASIS_10);
        envelope.addNamespaceDeclaration(WSConstants.TRUST_PREFIX, WSConstants.TRUST_NAMESPACE_05_02);
        envelope.addNamespaceDeclaration("ic", "http://schemas.xmlsoap.org/ws/2005/05/identity");

        Element header = new Element(WSConstants.SOAP_PREFIX + ":Header", WSConstants.SOAP12_NAMESPACE);
        Element body = new Element(WSConstants.SOAP_PREFIX + ":Body", WSConstants.SOAP12_NAMESPACE);


        envelope.appendChild(header);
        envelope.appendChild(body);



        //Build headers

        /*
        Element action = new Element(WSConstants.WSA_PREFIX + ":Action", WSConstants.WSA_NAMESPACE_05_08);
        Attribute id1 = new Attribute("wsu:Id", WSConstants.WSU_NAMESPACE, "_1");
        action.addAttribute(id1);
        action.appendChild("http://schemas.xmlsoap.org/ws/2005/02/trust/RSTR/Issue");
        header.appendChild(action);


        Element relatesTo = new Element(WSConstants.WSA_PREFIX + ":RelatesTo", WSConstants.WSA_NAMESPACE_05_08);
        Attribute id2 = new Attribute("wsu:Id", WSConstants.WSU_NAMESPACE, "_2");
        relatesTo.addAttribute(id2);
        relatesTo.appendChild(messageId);
        header.appendChild(relatesTo);

        Element to = new Element(WSConstants.WSA_PREFIX + ":To", WSConstants.WSA_NAMESPACE_05_08);
        Attribute id3 = new Attribute("wsu:Id", WSConstants.WSU_NAMESPACE, "_3");
        to.addAttribute(id3);
        to.appendChild("http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous");
        header.appendChild(to);

        Element security = new Element(WSConstants.WSSE_PREFIX + ":Security", WSConstants.WSSE_NAMESPACE_OASIS_10);
        //Attribute id3 = new Attribute("wsu:Id", WSConstants.WSU_NAMESPACE, "_3");
        //to.addAttribute(id3);
        header.appendChild(security);

        */



        //Build body
        Element rstr = new Element(WSConstants.TRUST_PREFIX + ":RequestSecurityTokenResponse", WSConstants.TRUST_NAMESPACE_05_02);
        Attribute context = new Attribute("Context","ProcessRequestSecurityToken");
        rstr.addAttribute(context);

        Element tokenType = new Element(WSConstants.TRUST_PREFIX + ":TokenType", WSConstants.TRUST_NAMESPACE_05_02);
        tokenType.appendChild("urn:oasis:names:tc:SAML:1.0:assertion");
        rstr.appendChild(tokenType);

        Element requestType = new Element(WSConstants.TRUST_PREFIX + ":RequestType", WSConstants.TRUST_NAMESPACE_05_02);
        requestType.appendChild("http://schemas.xmlsoap.org/ws/2005/02/trust/Issue");
        rstr.appendChild(requestType);

        Element rst = new Element(WSConstants.TRUST_PREFIX + ":RequestedSecurityToken", WSConstants.TRUST_NAMESPACE_05_02);

        ManagedToken token = new ManagedToken(cert,key);

        token.setGivenName("Chuck");
        token.setSurname("Mortimore");
        token.setEmailAddress("charliemortimore@gmail.com");
        token.setPrivatePersonalIdentifier("1234567890");
        token.setValidityPeriod(1, 10);

        RandomGUID uuid = new RandomGUID();

        try {
            rst.appendChild(token.serialize(uuid));
        } catch (SerializationException e) {
            e.printStackTrace();
        }

        rstr.appendChild(rst);

        Element requestedAttachedReference = new Element(WSConstants.TRUST_PREFIX + ":RequestedAttachedReference", WSConstants.TRUST_NAMESPACE_05_02);
        Element securityTokenReference = new Element(WSConstants.WSSE_PREFIX + ":SecurityTokenReference", WSConstants.WSSE_NAMESPACE_OASIS_10);
        Element keyIdentifier = new Element(WSConstants.WSSE_PREFIX + ":KeyIdentifier", WSConstants.WSSE_NAMESPACE_OASIS_10);
        Attribute valueType = new Attribute("ValueType","http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.0#SAMLAssertionID");
        keyIdentifier.addAttribute(valueType);
        keyIdentifier.appendChild("uuid-" + uuid.toString());
        securityTokenReference.appendChild(keyIdentifier);
        requestedAttachedReference.appendChild(securityTokenReference);
        rstr.appendChild(requestedAttachedReference);

        Element requestedUnAttachedReference = new Element(WSConstants.TRUST_PREFIX + ":RequestedUnattachedReference", WSConstants.TRUST_NAMESPACE_05_02);
        Element securityTokenReference1 = new Element(WSConstants.WSSE_PREFIX + ":SecurityTokenReference", WSConstants.WSSE_NAMESPACE_OASIS_10);
        Element keyIdentifier1 = new Element(WSConstants.WSSE_PREFIX + ":KeyIdentifier", WSConstants.WSSE_NAMESPACE_OASIS_10);
        Attribute valueType1 = new Attribute("ValueType","http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.0#SAMLAssertionID");
        keyIdentifier1.addAttribute(valueType1);
        keyIdentifier1.appendChild("uuid-" + uuid.toString());
        securityTokenReference1.appendChild(keyIdentifier1);
        requestedUnAttachedReference.appendChild(securityTokenReference1);
        rstr.appendChild(requestedUnAttachedReference);


        Element requestedDisplayToken = new Element(WSConstants.INFOCARD_PREFIX + ":RequestedDisplayToken", WSConstants.INFOCARD_NAMESPACE);
        Element displayToken = new Element(WSConstants.INFOCARD_PREFIX + ":DisplayToken", WSConstants.INFOCARD_NAMESPACE);
        Attribute lang = new Attribute("xml:lang","http://www.w3.org/XML/1998/namespace","en");
        displayToken.addAttribute(lang);
        requestedDisplayToken.appendChild(displayToken);

        Element displayClaim = new Element(WSConstants.INFOCARD_PREFIX + ":DisplayClaim", WSConstants.INFOCARD_NAMESPACE);
        Attribute uri = new Attribute("URI","http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname");
        displayClaim.addAttribute(uri);
        Element displayTag = new Element(WSConstants.INFOCARD_PREFIX + ":DisplayTag", WSConstants.INFOCARD_NAMESPACE);
        displayTag.appendChild("Given Name");
        Element displayValue = new Element(WSConstants.INFOCARD_PREFIX + ":DisplayValue", WSConstants.INFOCARD_NAMESPACE);
        displayValue.appendChild("Chuck");
        displayClaim.appendChild(displayTag);
        displayClaim.appendChild(displayValue);
        displayToken.appendChild(displayClaim);


        Element displayClaim1 = new Element(WSConstants.INFOCARD_PREFIX + ":DisplayClaim", WSConstants.INFOCARD_NAMESPACE);
        Attribute uri1 = new Attribute("URI","http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname");
        displayClaim1.addAttribute(uri1);
        Element displayTag1 = new Element(WSConstants.INFOCARD_PREFIX + ":DisplayTag", WSConstants.INFOCARD_NAMESPACE);
        displayTag1.appendChild("Last Name");
        Element displayValue1 = new Element(WSConstants.INFOCARD_PREFIX + ":DisplayValue", WSConstants.INFOCARD_NAMESPACE);
        displayValue1.appendChild("Mortimore");
        displayClaim1.appendChild(displayTag1);
        displayClaim1.appendChild(displayValue1);
        displayToken.appendChild(displayClaim1);


        Element displayClaim2 = new Element(WSConstants.INFOCARD_PREFIX + ":DisplayClaim", WSConstants.INFOCARD_NAMESPACE);
        Attribute uri2 = new Attribute("URI","http://schemas.xmlsoap.org/ws/2005/05/identity/claims/email");
        displayClaim2.addAttribute(uri2);
        Element displayTag2 = new Element(WSConstants.INFOCARD_PREFIX + ":DisplayTag", WSConstants.INFOCARD_NAMESPACE);
        displayTag2.appendChild("Email");
        Element displayValue2 = new Element(WSConstants.INFOCARD_PREFIX + ":DisplayValue", WSConstants.INFOCARD_NAMESPACE);
        displayValue2.appendChild("charliemortimore@gmail.com");
        displayClaim2.appendChild(displayTag2);
        displayClaim2.appendChild(displayValue2);
        displayToken.appendChild(displayClaim2);


        Element displayClaim3 = new Element(WSConstants.INFOCARD_PREFIX + ":DisplayClaim", WSConstants.INFOCARD_NAMESPACE);
        Attribute uri3 = new Attribute("URI","http://schemas.xmlsoap.org/ws/2005/05/identity/claims/privatepersonalidentifier");
        displayClaim3.addAttribute(uri3);
        Element displayTag3 = new Element(WSConstants.INFOCARD_PREFIX + ":DisplayTag", WSConstants.INFOCARD_NAMESPACE);
        displayTag3.appendChild("PPID");
        Element displayValue3 = new Element(WSConstants.INFOCARD_PREFIX + ":DisplayValue", WSConstants.INFOCARD_NAMESPACE);
        displayValue3.appendChild("1234567890");
        displayClaim3.appendChild(displayTag3);
        displayClaim3.appendChild(displayValue3);
        displayToken.appendChild(displayClaim3);


        rstr.appendChild(requestedDisplayToken);

        body.appendChild(rstr);

        return envelope.toXML();
    }


}
