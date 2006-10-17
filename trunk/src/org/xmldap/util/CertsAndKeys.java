/*
 * XmldapCertsAndKeys.java
 *
 * Created on 6. September 2006, 11:08
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.xmldap.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERUniversalString;
import org.bouncycastle.asn1.x509.Attribute;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.xmldap.exceptions.TokenIssuanceException;

/**
 * 
 * @author Axel Nennker
 */
public class CertsAndKeys {

	/** Creates a new instance of XmldapCertsAndKeys */
	private CertsAndKeys() {
	}

	public static KeyPair generateKeyPair() throws NoSuchAlgorithmException,
			NoSuchProviderException {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(1024, new SecureRandom());
		return keyGen.generateKeyPair();
	}

	public static X509Certificate infocard2Certificate(KeyPair kp)
			throws TokenIssuanceException {
		X509Certificate cert = null;
		return cert;
	}

	public static X509Certificate generateCaCertificate(KeyPair kp)
			throws TokenIssuanceException {
		String issuerStr = "CN=firefox, OU=infocard selector, O=xmldap, L=San Francisco, ST=California, C=US";
		X509Name issuer = new X509Name(issuerStr);
		return generateCaCertificate(kp, issuer, issuer);
	}

	static public X509V3CertificateGenerator addClientExtensions(
			X509V3CertificateGenerator gen)
			throws UnsupportedEncodingException {
		gen.addExtension(X509Extensions.BasicConstraints, true,
				new BasicConstraints(false));
		gen.addExtension(X509Extensions.KeyUsage, true, new KeyUsage(
				KeyUsage.digitalSignature | KeyUsage.keyEncipherment
						| KeyUsage.dataEncipherment | KeyUsage.keyCertSign));
		gen.addExtension(X509Extensions.ExtendedKeyUsage, true,
				new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth));

		return gen;
	}

	static public X509V3CertificateGenerator addCaExtensions(
			X509V3CertificateGenerator gen) {
		gen.addExtension(X509Extensions.BasicConstraints, true,
				new BasicConstraints(true));
		gen.addExtension(X509Extensions.KeyUsage, true, new KeyUsage(
				KeyUsage.digitalSignature | KeyUsage.keyEncipherment
						| KeyUsage.dataEncipherment | KeyUsage.keyCertSign));
		gen.addExtension(X509Extensions.ExtendedKeyUsage, true,
				new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth));
		// gen.addExtension(X509Extensions.SubjectAlternativeName, false,
		// new GeneralNames(new GeneralName(GeneralName.rfc822Name,
		// "test@test.test")));
		return gen;
	}

	/**
	 * generates an X509 certificate which is used to sign the xmlTokens in the
	 * firefox infocard selector
	 * 
	 * @param kp
	 * @param issuer
	 * @param subject
	 * @return
	 * @throws TokenIssuanceException
	 * @throws UnsupportedEncodingException
	 */
	public static X509Certificate generateClientCertificate(KeyPair kp,
			X509Name issuer, X509Name subject, String gender,
			Date dateOfBirth, String streetAddress, String telephoneNumber)
			throws TokenIssuanceException, UnsupportedEncodingException {
		if (Security.getProvider("BC") == null) {
			Security.addProvider(new BouncyCastleProvider());
		}

		X509Certificate cert = null;

		X509V3CertificateGenerator gen = new X509V3CertificateGenerator();
		gen.setIssuerDN(issuer);
		Calendar rightNow = Calendar.getInstance();
		rightNow.add(Calendar.MINUTE, -2); // 2 minutes
		gen.setNotBefore(rightNow.getTime());
		rightNow.add(Calendar.YEAR, 5);
		gen.setNotAfter(rightNow.getTime());
		gen.setSubjectDN(subject);
		gen.setPublicKey(kp.getPublic());
		gen.setSignatureAlgorithm("MD5WithRSAEncryption");
		gen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
		gen = addClientExtensions(gen);
		SubjectDirectoryAttributes sda = new SubjectDirectoryAttributes(
				gender, dateOfBirth, streetAddress, telephoneNumber);
		if (sda.size() > 0) {
			gen.addExtension(X509Extensions.SubjectDirectoryAttributes, false,
				sda);
		}


		try {
			cert = gen.generateX509Certificate(kp.getPrivate());
		} catch (InvalidKeyException e) {
			throw new TokenIssuanceException(e);
		} catch (SecurityException e) {
			throw new TokenIssuanceException(e);
		} catch (SignatureException e) {
			throw new TokenIssuanceException(e);
		}
		return cert;
	}

	/**
	 * generates an X509 certificate which is used to sign the xmlTokens in the
	 * firefox infocard selector
	 * 
	 * @param kp
	 * @param issuer
	 * @param subject
	 * @return
	 * @throws TokenIssuanceException
	 */
	public static X509Certificate generateCaCertificate(KeyPair kp,
			X509Name issuer, X509Name subject) throws TokenIssuanceException {
		if (Security.getProvider("BC") == null) {
			Security.addProvider(new BouncyCastleProvider());
		}

		X509Certificate cert = null;

		X509V3CertificateGenerator gen = new X509V3CertificateGenerator();
		gen.setIssuerDN(issuer);
		Calendar rightNow = Calendar.getInstance();
		rightNow.add(Calendar.MINUTE, -2); // 2 minutes
		gen.setNotBefore(rightNow.getTime());
		rightNow.add(Calendar.YEAR, 5);
		gen.setNotAfter(rightNow.getTime());
		gen.setSubjectDN(subject);
		gen.setPublicKey(kp.getPublic());
		gen.setSignatureAlgorithm("MD5WithRSAEncryption");
		gen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
		gen = addCaExtensions(gen);

		try {
			cert = gen.generateX509Certificate(kp.getPrivate());
		} catch (InvalidKeyException e) {
			throw new TokenIssuanceException(e);
		} catch (SecurityException e) {
			throw new TokenIssuanceException(e);
		} catch (SignatureException e) {
			throw new TokenIssuanceException(e);
		}
		return cert;
	}

	public static KeyPair bytesToKeyPair(byte[] bytes)
			throws TokenIssuanceException {
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		try {
			ObjectInputStream ois = new ObjectInputStream(bis);
			return (KeyPair) ois.readObject();
		} catch (IOException e) {
			throw new TokenIssuanceException(e);
		} catch (ClassNotFoundException e) {
			throw new TokenIssuanceException(e);
		}

	}

	public static byte[] keyPairToBytes(KeyPair kp)
			throws TokenIssuanceException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(kp);
			oos.close();
			return bos.toByteArray();
		} catch (IOException e) {
			throw new TokenIssuanceException(e);
		}
	}

}