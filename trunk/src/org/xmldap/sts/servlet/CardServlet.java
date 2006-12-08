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


package org.xmldap.sts.servlet;

import org.xmldap.exceptions.InfoCardProcessingException;
import org.xmldap.exceptions.KeyStoreException;
import org.xmldap.exceptions.SerializationException;
import org.xmldap.infocard.InfoCard;
import org.xmldap.infocard.TokenServiceReference;
import org.xmldap.infocard.policy.SupportedClaim;
import org.xmldap.infocard.policy.SupportedClaimList;
import org.xmldap.infocard.policy.SupportedToken;
import org.xmldap.infocard.policy.SupportedTokenList;
import org.xmldap.sts.db.ManagedCard;
import org.xmldap.sts.db.CardStorage;
import org.xmldap.sts.db.impl.CardStorageEmbeddedDBImpl;
import org.xmldap.util.KeystoreUtil;
import org.xmldap.util.ServletUtil;
import org.xmldap.util.XSDDateTime;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;


public class CardServlet extends HttpServlet {

    private static ServletUtil _su;
    private static CardStorage storage = new CardStorageEmbeddedDBImpl();

    public void init() throws ServletException {
         _su = new ServletUtil(getServletConfig());
        storage.startup();
    }



    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        HttpSession session = request.getSession(true);
        String username = (String)session.getAttribute("username");
        if (username == null) {

            RequestDispatcher dispatcher = request.getRequestDispatcher("/cardmanager/");
            dispatcher.forward(request,response);
	    return;
        }


        String url = request.getRequestURL().toString();
        int index = url.lastIndexOf("/");
        index++;
        String cardID = url.substring(index, url.length() - 4);
        System.out.println(cardID);
        ManagedCard managedCard = storage.getCard(cardID);
        if (managedCard == null) {
            /* log */
            return;
        }

        if (_su == null) {
            _su = new ServletUtil(getServletConfig());
        }

        //Get my keystore
        KeystoreUtil keystore = null;
        try {
	    keystore = _su.getKeystore();
        } catch (KeyStoreException e) {
            e.printStackTrace();
	    return;
        }

        X509Certificate cert = null;
        try {
            cert = _su.getCertificate();
        } catch (KeyStoreException e) {
            e.printStackTrace();
	    return;
        }

        PrivateKey pKey = null;
        try {
	    pKey = _su.getPrivateKey();
        } catch (KeyStoreException e) {
            throw new ServletException(e);
        }

	    String domainname = _su.getDomainName();

        InfoCard card = new InfoCard(cert, pKey);
        card.setCardId("https://" + domainname + "/sts/card/" + managedCard.getCardId() );
        card.setCardName(managedCard.getCardName());
        card.setCardVersion(1);
        card.setIssuerName(domainname);
        card.setIssuer("https://" + domainname + "/sts/tokenservice");
        XSDDateTime issued = new XSDDateTime();
        XSDDateTime expires = new XSDDateTime(525600);

        card.setTimeIssued(issued.getDateTime());
        card.setTimeExpires(expires.getDateTime());

        TokenServiceReference tsr = new TokenServiceReference("https://" + domainname + "/sts/tokenservice", "https://" + domainname + "/sts/mex", cert);
        tsr.setUserName(username);
        card.setTokenServiceReference(tsr);


        SupportedTokenList tokenList = new SupportedTokenList();
        SupportedToken token = new SupportedToken(SupportedToken.SAML11);
        tokenList.addSupportedToken(token);
        card.setTokenList(tokenList);

        SupportedClaimList claimList = new SupportedClaimList();
        SupportedClaim given = new SupportedClaim("GivenName", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname");
        SupportedClaim sur = new SupportedClaim("Surname", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname");
        SupportedClaim email = new SupportedClaim("EmailAddress", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress");
        SupportedClaim ppid = new SupportedClaim("PPID", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/privatepersonalidentifier");
        claimList.addSupportedClaim(given);
        claimList.addSupportedClaim(sur);
        claimList.addSupportedClaim(email);
        claimList.addSupportedClaim(ppid);
        card.setClaimList(claimList);

        card.setPrivacyPolicy("https://" + domainname + "/PrivacyPolicy.xml");


        PrintWriter out = response.getWriter();
        response.setContentType("application/soap+xml; charset=utf-8");

        try {
            out.println(card.toXML());
        } catch (SerializationException e) {
        	throw new ServletException(e);
        }

        out.flush();
        out.close();
        return;


    }


}
