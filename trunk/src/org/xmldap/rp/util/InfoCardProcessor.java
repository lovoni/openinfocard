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


package org.xmldap.rp.util;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.ParsingException;
import org.xmldap.exceptions.CryptoException;
import org.xmldap.exceptions.InfoCardProcessingException;
import org.xmldap.xmldsig.EnvelopedSignature;

import java.io.IOException;
import java.security.PrivateKey;
import java.util.HashMap;

public class InfoCardProcessor {


    public HashMap processCard(String encryptedXML, PrivateKey privateKey) throws InfoCardProcessingException {


        //decrypt it.
        DecryptUtil decrypter = new DecryptUtil();
        StringBuffer decryptedXML = decrypter.decryptXML(encryptedXML, privateKey);


        //let's make a doc
        Builder parser = new Builder();
        Document assertion = null;
        try {
            assertion = parser.build(decryptedXML.toString(), "");
        } catch (ParsingException e) {
            throw new InfoCardProcessingException(e);
        } catch (IOException e) {
            throw new InfoCardProcessingException(e);
        }

        //Validate it
        boolean verified = false;
        try {
            verified = EnvelopedSignature.validate(assertion);
        } catch (CryptoException e) {
            throw new InfoCardProcessingException(e);
        }
        if (!verified) throw new InfoCardProcessingException("The signiture was invalid");

        //Get the claims.
        ClaimParserUtil claimParser = new ClaimParserUtil();
        return claimParser.parseClaims(assertion);

    }


}