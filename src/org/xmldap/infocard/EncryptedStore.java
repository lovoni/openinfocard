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

import org.xmldap.util.Base64;
import org.xmldap.util.XmlFileUtil;
import org.xmldap.crypto.CryptoUtils;
import org.xmldap.crypto.EncryptedStoreKeys;
import org.xmldap.ws.WSConstants;
import org.xmldap.exceptions.CryptoException;
import org.xmldap.exceptions.SerializationException;

import java.io.*;
import java.util.Random;
import java.util.Arrays;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import net.sourceforge.lightcrypto.SafeObject;
import nu.xom.*;


public class EncryptedStore {


    private static String sample = "";



    public Document decryptStore(InputStream encryptedStoreStream, String password) throws CryptoException, ParsingException{

        Document encryptedStore = null;
        try {
            encryptedStore = XmlFileUtil.readXml(encryptedStoreStream);
        } catch (IOException e) {
            throw new ParsingException("Error parsing EncryptedStore", e);
        }

        XPathContext context = new XPathContext();
        context.addNamespace("id","http://schemas.xmlsoap.org/ws/2005/05/identity");
        context.addNamespace("enc","http://www.w3.org/2001/04/xmlenc#");

        Nodes saltNodes = encryptedStore.query("//id:StoreSalt",context);
        Element saltElm = (Element) saltNodes.get(0);

        Nodes cipherValueNodes = encryptedStore.query("//enc:CipherValue",context);
        Element cipherValueElm = (Element) cipherValueNodes.get(0);

        String roamingStoreString =  decrypt(cipherValueElm.getValue(), password, saltElm.getValue());

        //let's make a doc
        Builder parser = new Builder();
        Document roamingStore = null;
        try {
            roamingStore = parser.build(roamingStoreString, "");
        } catch (ParsingException e) {
            e.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return roamingStore;

    }


    private String decrypt(String cipherText, String password, String salt) throws CryptoException{


        EncryptedStoreKeys keys = new EncryptedStoreKeys(password,Base64.decode(salt));

        byte[] cipherBytes = Base64.decode(cipherText);
        byte[] iv = new byte[16];
        byte[] integrityCode = new byte[32];
        byte[] data = new byte[cipherBytes.length - iv.length - integrityCode.length];
        byte[] ivPlusData = new byte[cipherBytes.length  - integrityCode.length];

        //Copy IV
        System.arraycopy(cipherBytes, 0, iv, 0, 16);

        //Copy integrityCode
        System.arraycopy(cipherBytes, 16, integrityCode, 0, 32);

        //Copy data
        System.arraycopy(cipherBytes, 48, data, 0, data.length);

        //Copy iv and data
        System.arraycopy(iv, 0, ivPlusData, 0, 16);
        System.arraycopy(data, 0, ivPlusData, 16, data.length);


        SafeObject keyBytes = new SafeObject();
        try {
            keyBytes.setText(keys.getEncryptionKey());
        } catch (Exception e) {
            throw new CryptoException("Error Parsing Roaming Store", e);
        }


        StringBuffer clearText = null;

        clearText = CryptoUtils.decryptAESCBC(new StringBuffer(Base64.encodeBytes(ivPlusData)), keyBytes);


        byte[] hashedIntegrityCode = getHashedIntegrityCode(iv, keys.getIntegrityKey(),  clearText.toString());


        boolean valid = Arrays.equals(integrityCode, hashedIntegrityCode);
        if (!valid) {

            throw new CryptoException("The cardstore did not pass the integrity check - it may have been tampered with");

        }


        //get rid of the byte order mark
        int start = clearText.indexOf("<RoamingStore");
        return clearText.substring(start);


    }


    private byte[] getHashedIntegrityCode(byte[] iv, byte[] integrityKey,  String clearText) {

        byte[] clearBytes = new byte[0];
        try {
            clearBytes = clearText.getBytes("UTF8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        byte[] lastBlock = new byte[16];
        System.arraycopy(clearBytes,clearBytes.length - 16 ,lastBlock, 0, 16);


        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {

            e.printStackTrace();
        }


        byte[] integrityCheck = new byte[64];
        System.arraycopy(iv, 0, integrityCheck, 0, 16);
        System.arraycopy(integrityKey, 0, integrityCheck, 16, 32);
        System.arraycopy(lastBlock, 0, integrityCheck, 48, 16);
        digest.update(integrityCheck);
        return digest.digest();

    }

    public String encryptStore(RoamingStore roamingStore, String password) throws CryptoException {


        Element encryptedStore = new Element("EncryptedStore", WSConstants.INFOCARD_NAMESPACE);
        Element storeSalt = new Element("StoreSalt", WSConstants.INFOCARD_NAMESPACE);
        Random rand = new Random();
        byte[] salt = new byte[16];
        rand.nextBytes(salt);
        storeSalt.appendChild(Base64.encodeBytes(salt));
        encryptedStore.appendChild(storeSalt);
        Element encryptedData = new Element("EncryptedData", WSConstants.ENC_NAMESPACE);
        Element cipherData = new Element("CipherData", WSConstants.ENC_NAMESPACE);
        Element cipherValue = new Element("CipherValue", WSConstants.ENC_NAMESPACE);

        encryptedStore.appendChild(encryptedData);
        encryptedData.appendChild(cipherData);
        cipherData.appendChild(cipherValue);
        cipherValue.appendChild(cipherValue);
        cipherValue.appendChild(encrypt(roamingStore, password, salt));

        return null;
    }


    public String encrypt(RoamingStore roamingStore, String password, byte[] salt) throws CryptoException{


        EncryptedStoreKeys keys = new EncryptedStoreKeys(password,salt);

        Random rand = new Random();
        byte[] iv = new byte[16];
        rand.nextBytes(iv);

        byte[] integrityKey = new byte[32];

        String dataString = null;
        try {
            dataString = roamingStore.toXML();
        } catch (SerializationException e) {
            throw new CryptoException("Error getting RoamingStore XML", e);
        }

        byte[] data = new byte[0];
        try {
            data = dataString.getBytes("UTF-16LE");
        } catch (UnsupportedEncodingException e) {
            throw new CryptoException("Error getting RoamingStore XML bytes in UTF-16LE", e);
        }



        return null;
    }



    public static void main(String[] args) {

        String password = "password";

        EncryptedStore encryptedStore = new EncryptedStore();
        Document roamingStore = null;
        try {
            InputStream stream =  new FileInputStream("/Users/cmort/Desktop/backup.crds");
            roamingStore = encryptedStore.decryptStore(stream, password);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (ParsingException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (CryptoException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


        Serializer serializer = null;
        try {
            serializer = new Serializer(System.out, "UTF8");
            serializer.setIndent(4);
            serializer.setMaxLength(64);
            serializer.setPreserveBaseURI(false);
            serializer.write(roamingStore);
            serializer.flush();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
