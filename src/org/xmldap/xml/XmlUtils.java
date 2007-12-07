/*
 * Copyright (c) 2007, Axel Nennker - axel at nennker.de
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

package org.xmldap.xml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.xmldap.exceptions.SerializationException;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.XPathContext;
import nu.xom.canonical.Canonicalizer;

public class XmlUtils {

    public static Document parse( String xml ) throws java.io.IOException, nu.xom.ParsingException {
        Builder builder = new Builder();
        return new Document(builder.build(xml, ""));
    }

    public static String query(Element tokenXML, XPathContext context, String query) {
            Nodes uns = tokenXML.query(query,context);
    Element un = (Element) uns.get(0);
    String userName = un.getValue();
            return userName;
    }

	/**
	 * @return
	 * @throws SerializationException
	 */
	public static byte[] canonicalize(Element data, String method) throws IOException {
		byte[] dataBytes;
		ByteArrayOutputStream stream = new ByteArrayOutputStream();

        //Canonicalizer outputer = new Canonicalizer(stream, Canonicalizer.CANONICAL_XML);
        Canonicalizer outputer = new Canonicalizer(stream, method);
        outputer.write(data);

        dataBytes = stream.toByteArray();
        stream.close();
        
		return dataBytes;
	}

}