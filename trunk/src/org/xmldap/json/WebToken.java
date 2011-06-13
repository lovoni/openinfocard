/*
 * Copyright (c) 2011, Axel Nennker - http://axel.nennker.de/
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
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY
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

package org.xmldap.json;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import net.sourceforge.lightcrypto.SafeObject;

import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.sec.SECObjectIdentifiers;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.digests.SHA384Digest;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmldap.crypto.CryptoUtils;
import org.xmldap.util.Base64;

public class WebToken {
  
  public static final String SIGN_ALG_HS256 = "HS256";
  public static final String SIGN_ALG_HS383 = "HS384";
  public static final String SIGN_ALG_HS512 = "HS512";
  
  public static final String SIGN_ALG_ES256 = "ES256";
  public static final String SIGN_ALG_ES383 = "ES384";
  public static final String SIGN_ALG_ES512 = "ES512";
  
  public static final String SIGN_ALG_RS256 = "RS256";
  public static final String SIGN_ALG_RS383 = "RS384";
  public static final String SIGN_ALG_RS512 = "RS512";
  
  public static final String ENC_ALG_RE128 = "RE128"; // RSA-OAEP encrypted AES-CBC key with 128 bits
  public static final String ENC_ALG_RE192 = "RE192"; // RSA-OAEP encrypted AES-CBC key with 192 bits
  public static final String ENC_ALG_RE256 = "RE256"; // RSA-OAEP encrypted AES-CBC key with 256 bits
  
  public static final String ENC_ALG_AE128 = "AE128"; // AES-CBC with 128 bit key size
  public static final String ENC_ALG_AE192 = "AE192"; // AES-CBC with 192 bit key size
  public static final String ENC_ALG_AE256 = "AE256"; // AES-CBC with 256 bit key size
  
  public static final String ENC_ALG_PE820 = "PE820"; // Password based encryption with 8 byte salt and 20 rounds

  String mJsonStr = null;
  PrivateKey mPrivateKey = null;
  String mPKAlgorithm = null; 
  
  public WebToken(String jso, String algorithm) {
    mJsonStr = jso;
    mPKAlgorithm = algorithm;
  }
  
  static public boolean verify(String jwt, RSAPublicKey pubkey) throws Exception {
    String jwtHeaderSegment;
    String jwtPayloadSegment;
    String jwtCryptoSegment;
    String[] split = jwt.split("\\.");
    jwtHeaderSegment = split[0];
    jwtPayloadSegment = split[1];
    jwtCryptoSegment = split[2];
    
    String algorithm;
    JSONObject header = new JSONObject(jwtHeaderSegment);
    String jwtAlgStr = (String) header.get("alg");
    if ("RS256".equals(jwtAlgStr)) {
      algorithm = "SHA256withRSA";
    } else if ("RS384".equals(jwtAlgStr)) {
      algorithm = "SHA384withRSA";
    } else if ("RS512".equals(jwtAlgStr)) {
      algorithm = "SHA512withRSA";
    } else {
      throw new NoSuchAlgorithmException("JWT algorithm: " + jwtAlgStr);
    }
    
    String stringToSign = jwtHeaderSegment + "." + jwtPayloadSegment;
    Signature signature = Signature.getInstance(algorithm);
    signature.initVerify(pubkey);
    signature.update(stringToSign.getBytes("utf-8"));

    byte[] signatureBytes = Base64.decodeUrl(jwtCryptoSegment);

    return signature.verify(signatureBytes);
  }
  
  static public boolean verify(String jwt, byte[] x, byte[] y) throws Exception {
    String jwtHeaderSegment;
    String jwtPayloadSegment;
    String jwtCryptoSegment;
    String[] split = jwt.split("\\.");
    jwtHeaderSegment = split[0];
    jwtPayloadSegment = split[1];
    jwtCryptoSegment = split[2];

    byte[] signatureBytes = Base64.decodeUrl(jwtCryptoSegment);
    byte[] rBytes = new byte[32];
    System.arraycopy(signatureBytes, 0, rBytes, 0, 32);
    byte[] sBytes = new byte[32];
    System.arraycopy(signatureBytes, 32, sBytes, 0, 32);
    
    BigInteger r = new BigInteger(1, rBytes);
    BigInteger s = new BigInteger(1, sBytes);
    
    DERObjectIdentifier oid;
    Digest digest;
    
    String header = new String(Base64.decodeUrl(jwtHeaderSegment));
    JSONObject headerO = new JSONObject(header);
    String jwtAlgStr = (String) headerO.get("alg");
    if ("ES256".equals(jwtAlgStr)) {
      oid = SECObjectIdentifiers.secp256r1;
      digest = new SHA256Digest();
    } else if ("ES384".equals(jwtAlgStr)) {
      oid = SECObjectIdentifiers.secp384r1;
      digest = new SHA384Digest();
    } else if ("ES512".equals(jwtAlgStr)) {
      oid = SECObjectIdentifiers.secp521r1;
      digest = new SHA512Digest();
    } else {
      throw new NoSuchAlgorithmException("JWT algorithm: " + jwtAlgStr);
    }

    X9ECParameters x9ECParameters = SECNamedCurves.getByOID(oid);

    ECDSASigner verifier = new ECDSASigner();
    BigInteger xB = new BigInteger(1, x);
    BigInteger yB = new BigInteger(1, y);
    ECCurve curve = x9ECParameters.getCurve();
    ECPoint qB = curve.createPoint(xB, yB, false);
    ECPoint q = new ECPoint.Fp(curve, qB.getX(), qB.getY());
    ECDomainParameters ecDomainParameters = new ECDomainParameters(
        curve, 
        x9ECParameters.getG(), 
        x9ECParameters.getN(), 
        x9ECParameters.getH(),
        x9ECParameters.getSeed());
    ECPublicKeyParameters ecPublicKeyParameters = new ECPublicKeyParameters(
        q, ecDomainParameters);
    verifier.init(false, ecPublicKeyParameters);
    String hp = jwtHeaderSegment + "." + jwtPayloadSegment;
    byte[] bytes = hp.getBytes("utf-8");
    digest.update(bytes, 0, bytes.length);
    byte[] out = new byte[digest.getDigestSize()];
    /*int result =*/ digest.doFinal(out, 0);
    
    boolean verified = verifier.verifySignature(out, r, s);
    return verified;
  }
  
  
   public String serialize(BigInteger D) 
    throws NoSuchAlgorithmException, JSONException, InvalidKeyException, SignatureException, IOException, InvalidKeySpecException {
    
    DERObjectIdentifier oid;
    Digest digest;
    JSONObject header = new JSONObject(mPKAlgorithm);
    String jwtAlgStr = (String) header.get("alg");
    if ("ES256".equals(jwtAlgStr)) {
      oid = SECObjectIdentifiers.secp256r1;
      digest = new SHA256Digest();
    } else if ("ES384".equals(jwtAlgStr)) {
      oid = SECObjectIdentifiers.secp384r1;
      digest = new SHA384Digest();
    } else if ("ES512".equals(jwtAlgStr)) {
      oid = SECObjectIdentifiers.secp521r1;
      digest = new SHA512Digest();
    } else {
      throw new NoSuchAlgorithmException("JWT algorithm: " + jwtAlgStr);
    }

    X9ECParameters x9ECParameters = SECNamedCurves.getByOID(oid);
    ECDomainParameters ecParameterSpec = new ECDomainParameters(
        x9ECParameters.getCurve(), 
        x9ECParameters.getG(), 
        x9ECParameters.getN(), 
        x9ECParameters.getH(), 
        x9ECParameters.getSeed());
    ECPrivateKeyParameters ecPrivateKeyParameters = new ECPrivateKeyParameters(
        D, ecParameterSpec);

    
    String b64 = Base64.encodeBytes(mPKAlgorithm.getBytes("utf-8"), 
        org.xmldap.util.Base64.DONT_BREAK_LINES | org.xmldap.util.Base64.URL);
    StringBuffer sb = new StringBuffer(b64);
    sb.append('.');
    b64 = Base64.encodeBytes(mJsonStr.getBytes("utf-8"), 
        org.xmldap.util.Base64.DONT_BREAK_LINES | org.xmldap.util.Base64.URL);
    sb.append(b64);
    
    String stringToSign = sb.toString();
    byte[] bytes = stringToSign.getBytes("utf-8");
    digest.update(bytes, 0, bytes.length);
    byte[] out = new byte[digest.getDigestSize()];
    /*int result =*/ digest.doFinal(out, 0);

    sb.append('.');
    
    String signed = signECDSA(ecPrivateKeyParameters, out);

    sb.append(signed);
    return sb.toString();
  }

  private String signECDSA(ECPrivateKeyParameters ecPrivateKeyParameters, byte[] bytes) throws UnsupportedEncodingException {
    ECDSASigner signer = new ECDSASigner();
    signer.init(true, ecPrivateKeyParameters);
    BigInteger[] res = signer.generateSignature(bytes);
    BigInteger r = res[0];
    BigInteger s = res[1];
    
    String signed = rs2jwt(r, s);
    //System.out.println("Signed:" + signed);
    return signed;
  }

  private String rs2jwt(BigInteger r, BigInteger s) {
    //    System.out.println("R:" + r.toString());
    //    System.out.println("S:" + s.toString());
        byte[] rBytes = r.toByteArray();
    //    System.out.println("rBytes.length:" + rBytes.length);
        byte[] sBytes = s.toByteArray();
    //    System.out.println("sBytes.length:" + sBytes.length);
    //    StringBuffer sb = new StringBuffer();
    //    for (int i=0; i<rBytes.length;i++) {
    //      sb.append(String.valueOf((int)rBytes[i]));
    //      sb.append(',');
    //    }
    //    System.out.println("Rbytes:" + sb.toString());
    //    sb = new StringBuffer();
    //    for (int i=0; i<sBytes.length;i++) {
    //      sb.append(String.valueOf((int)sBytes[i]));
    //      sb.append(',');
    //    }
    //    System.out.println("Sbytes:" + sb.toString());
        byte[] rsBytes = new byte[64];
        for (int i=0; i<rsBytes.length; i++) {
          rsBytes[i] = 0;
        }
        if (rBytes.length >= 32) {
          System.arraycopy(rBytes, rBytes.length - 32, rsBytes, 0, 32);
        } else {
          System.arraycopy(rBytes, 0, rsBytes, 32-rBytes.length, rBytes.length);
        }
        if (sBytes.length >= 32) {
          System.arraycopy(sBytes, sBytes.length - 32, rsBytes, 32, 32);
        } else {
          System.arraycopy(sBytes, 0, rsBytes, 64-sBytes.length, sBytes.length);
        }
        String signed = Base64.encodeBytes(rsBytes, 
            org.xmldap.util.Base64.DONT_BREAK_LINES | org.xmldap.util.Base64.URL);
    return signed;
  }

  public String serialize(RSAPrivateKey privateKey) throws JSONException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException {
    String b64 = Base64.encodeBytes(mPKAlgorithm.getBytes("utf-8"), 
        org.xmldap.util.Base64.DONT_BREAK_LINES | org.xmldap.util.Base64.URL);
    StringBuffer sb = new StringBuffer(b64);
    sb.append('.');
    b64 = Base64.encodeBytes(mJsonStr.getBytes("utf-8"), 
        org.xmldap.util.Base64.DONT_BREAK_LINES | org.xmldap.util.Base64.URL);
    sb.append(b64);
    
    String stringToSign = sb.toString();
    
    sb.append('.');
    
    JSONObject algO = new JSONObject(mPKAlgorithm);
    String jwtAlgStr = algO.getString("alg");
    Signature signature;
    String algorithm;
    if ("RS256".equals(jwtAlgStr)) {
      algorithm = "SHA256withRSA";
    } else if ("RS384".equals(jwtAlgStr)) {
      algorithm = "SHA384withRSA";
    } else if ("RS512".equals(jwtAlgStr)) {
      algorithm = "SHA512withRSA";
    } else {
      throw new NoSuchAlgorithmException("JWT algorithm: " + jwtAlgStr);
    }
    signature = Signature.getInstance(algorithm);
    signature.initSign(privateKey);
    signature.update(stringToSign.getBytes("utf-8"));
    byte[] bytes = signature.sign();

    b64 = Base64.encodeBytes(bytes, 
        org.xmldap.util.Base64.DONT_BREAK_LINES | org.xmldap.util.Base64.URL);
    sb.append(b64);
    return sb.toString();
  }

//  public String serialize(PrivateKey privateKey) 
//    throws UnsupportedEncodingException, JSONException, 
//    NoSuchAlgorithmException, InvalidKeyException, SignatureException 
//  {
//    String b64 = Base64.encodeBytes(mPKAlgorithm.getBytes("utf-8"), 
//        org.xmldap.util.Base64.DONT_BREAK_LINES | org.xmldap.util.Base64.URL);
//    StringBuffer sb = new StringBuffer(b64);
//    sb.append('.');
//    b64 = Base64.encodeBytes(mJsonStr.getBytes("utf-8"), 
//        org.xmldap.util.Base64.DONT_BREAK_LINES | org.xmldap.util.Base64.URL);
//    sb.append(b64);
//    sb.append('.');
//    
//    JSONObject algO = new JSONObject(mPKAlgorithm);
//    String jwtAlgStr = algO.getString("alg");
//
//    Signature signature = Signature.getInstance(jwtAlgStr);
//    signature.initSign(privateKey);
//    signature.update(mJsonStr.getBytes("utf-8"));
//    byte[] bytes = signature.sign();
//    String signed = new String(bytes);
//    sb.append(signed);
//    return sb.toString();
//  }

public String serialize(byte[] passphraseBytes) 
    throws JSONException, NoSuchAlgorithmException, InvalidKeyException, IllegalStateException, UnsupportedEncodingException {
    String     b64 = Base64.encodeBytes(mPKAlgorithm.getBytes("utf-8"), 
        org.xmldap.util.Base64.DONT_BREAK_LINES | org.xmldap.util.Base64.URL);
    StringBuffer sb = new StringBuffer(b64);
    sb.append('.');
    b64 = Base64.encodeBytes(mJsonStr.getBytes("utf-8"), 
        org.xmldap.util.Base64.DONT_BREAK_LINES | org.xmldap.util.Base64.URL);
    sb.append(b64);
    
    String stringToSign = sb.toString();
    
    sb.append('.');
    String signed;
    
    JSONObject algO = new JSONObject(mPKAlgorithm);
    String jwtAlgStr = algO.getString("alg");
    String algorithm;
    if ("HS256".equals(jwtAlgStr)) { // HMAC SHA-256
      algorithm = "HMACSHA256";
    } else if ("HS384".equals(jwtAlgStr)) { // HMAC SHA-384
      algorithm = "HMACSHA384";
    } else if ("HS512".equals(jwtAlgStr)) { // HMAC SHA-512
      algorithm = "HMACSHA512";
    } else {
      throw new NoSuchAlgorithmException("JWT shared secret" + jwtAlgStr);
    }
    Mac mac = Mac.getInstance(algorithm);
    mac.init(new SecretKeySpec(passphraseBytes, mac.getAlgorithm()));
    mac.update(stringToSign.getBytes("utf-8"));
    byte[] bytes = mac.doFinal();
    signed = Base64.encodeBytes(bytes, 
        org.xmldap.util.Base64.DONT_BREAK_LINES | org.xmldap.util.Base64.URL);
    sb.append(signed);
    return sb.toString();
}

public static String decrypt(String encrypted, String password) throws Exception {
  String[] split = encrypted.split("\\.");
  String headerB64 = split[0];
  String jwtKeySegmentB64 = split[1];
  String jwtCryptoSegmentB64 = split[2];

  String jwtHeaderSegment = new String(Base64.decodeUrl(headerB64));
  JSONObject jwtHeaderJSON = new JSONObject(jwtHeaderSegment);
  String alg = jwtHeaderJSON.getString("alg");
  if ("PE20".equals(alg)) {
    
  }
  String jwtKeySegment = new String(Base64.decodeUrl(jwtKeySegmentB64));
  JSONObject jwtKeyJSON = new JSONObject(jwtKeySegment);
  String wrappedKeyB64 = jwtKeyJSON.getString("wrp");
  byte[] wrappedKey = Base64.decodeUrl(wrappedKeyB64);
  String saltB64 = jwtKeyJSON.getString("slt");
  byte[] salt = Base64.decodeUrl(saltB64);
  
  final String algorithm = "PBEWithMD5AndDES";
  
  PBEParameterSpec paramSpec = new PBEParameterSpec(salt, 20);
  PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray());
  SecretKeyFactory kf = SecretKeyFactory.getInstance(algorithm);
  SecretKey passwordKey = kf.generateSecret(keySpec);

  Cipher c = Cipher.getInstance("PBEWithMD5AndDES");

  c.init(Cipher.UNWRAP_MODE, passwordKey, paramSpec);
  Key unwrappedKey = c.unwrap(wrappedKey, "DESede", Cipher.SECRET_KEY);

  c = Cipher.getInstance("DESede");
  c.init(Cipher.DECRYPT_MODE, unwrappedKey);

  byte[] jwtCryptoSegment = Base64.decodeUrl(jwtCryptoSegmentB64);
  return new String(c.doFinal(jwtCryptoSegment));
}

public String encrypt(SecretKey key) throws Exception {
  String b64 = Base64.encodeBytes(mPKAlgorithm.getBytes("utf-8"), 
      org.xmldap.util.Base64.DONT_BREAK_LINES | org.xmldap.util.Base64.URL);
  StringBuffer sb = new StringBuffer(b64);
  sb.append('.');

  SafeObject keyBytes = new SafeObject();
  byte[] secretKey = key.getEncoded();
  keyBytes.setText(secretKey);

  StringBuffer clearTextBuffer = new StringBuffer(mJsonStr);
  String cipherText = CryptoUtils.encryptAESCBC(clearTextBuffer, keyBytes).toString();
  b64 = Base64.encodeBytes(cipherText.getBytes("utf-8"), 
      org.xmldap.util.Base64.DONT_BREAK_LINES | org.xmldap.util.Base64.URL);
  sb.append(b64);
//  System.out.println("AES jwtCryptoSegment base64:" + b64);
  
  return sb.toString();

}

public static String decrypt(String encrypted, SecretKey key) throws Exception {
  String[] split = encrypted.split("\\.");
  String headerB64 = split[0];
  String jwtCryptoSegmentB64 = split[1];

  String jwtHeaderSegment = new String(Base64.decodeUrl(headerB64));
  
  String algorithm = key.getAlgorithm();
  if (!"AES".equals(algorithm)) {
    throw new NoSuchAlgorithmException("unsupported JWT AES algorithm: " + algorithm);
  }
  
  int keylength;
  JSONObject header = new JSONObject(jwtHeaderSegment);
  String jwtAlgStr = (String) header.get("alg");
  if (ENC_ALG_AE128.equals(jwtAlgStr)) {
    keylength = 128;
  } else if (ENC_ALG_AE192.equals(jwtAlgStr)) {
    keylength = 192;
  } else if (ENC_ALG_AE256.equals(jwtAlgStr)) {
    keylength = 256;
  } else {
    throw new NoSuchAlgorithmException("unsupported keylength JWT AES algorithm: " + jwtAlgStr);
  }
  
  SafeObject keyBytes = new SafeObject();
  byte[] secretKey = key.getEncoded();
  keyBytes.setText(secretKey);
  
  byte[] jwtCryptoSegmentBytes = Base64.decodeUrl(jwtCryptoSegmentB64);
  StringBuffer clearTextBuffer = CryptoUtils.decryptAESCBC(new StringBuffer(new String(jwtCryptoSegmentBytes)), keyBytes);

  return clearTextBuffer.toString();
}

public String encrypt(String password) throws Exception {
  final String algorithm = "PBEWithMD5AndDES";
  String b64 = Base64.encodeBytes(mPKAlgorithm.getBytes("utf-8"), 
      org.xmldap.util.Base64.DONT_BREAK_LINES | org.xmldap.util.Base64.URL);
  StringBuffer sb = new StringBuffer(b64);
  sb.append('.');

  KeyGenerator kg = KeyGenerator.getInstance("DESede");
  Key sharedKey = kg.generateKey();

  byte[] salt = new byte[8];
  SecureRandom random = new SecureRandom();
  random.nextBytes(salt);

  PBEParameterSpec paramSpec = new PBEParameterSpec(salt, 20);
  PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray());
  SecretKeyFactory kf = SecretKeyFactory.getInstance(algorithm);
  SecretKey passwordKey = kf.generateSecret(keySpec);
  Cipher c = Cipher.getInstance(algorithm);
  c.init(Cipher.WRAP_MODE, passwordKey, paramSpec);
  byte[] wrappedKey = c.wrap(sharedKey);

  JSONObject keyInfoO = new JSONObject();
  b64 = Base64.encodeBytes(wrappedKey, 
      org.xmldap.util.Base64.DONT_BREAK_LINES | org.xmldap.util.Base64.URL);
  keyInfoO.put("wrp", b64);
  b64 = Base64.encodeBytes(salt, 
      org.xmldap.util.Base64.DONT_BREAK_LINES | org.xmldap.util.Base64.URL);
  keyInfoO.put("slt", b64);
  String keyInfoString = keyInfoO.toString();
  
  b64 = Base64.encodeBytes(keyInfoString.getBytes("utf-8"), 
      org.xmldap.util.Base64.DONT_BREAK_LINES | org.xmldap.util.Base64.URL);
  sb.append(b64);
  sb.append('.');

  c = Cipher.getInstance("DESede");
  c.init(Cipher.ENCRYPT_MODE, sharedKey);
  byte[] encrypted = c.doFinal(mJsonStr.getBytes());

  b64 = Base64.encodeBytes(encrypted, 
      org.xmldap.util.Base64.DONT_BREAK_LINES | org.xmldap.util.Base64.URL);
  sb.append(b64);

  return sb.toString();
}

public String encrypt(RSAPublicKey rsaPublicKey) throws Exception {
    String b64 = Base64.encodeBytes(mPKAlgorithm.getBytes("utf-8"), 
        org.xmldap.util.Base64.DONT_BREAK_LINES | org.xmldap.util.Base64.URL);
    StringBuffer sb = new StringBuffer(b64);
    sb.append('.');
  
    int keylength;
    JSONObject header = new JSONObject(mPKAlgorithm);
    String jwtAlgStr = (String) header.get("alg");
    if (ENC_ALG_RE256.equals(jwtAlgStr)) {
      keylength = 256;
    } else if (ENC_ALG_RE192.equals(jwtAlgStr)) {
      keylength = 192;
    } else if (ENC_ALG_RE128.equals(jwtAlgStr)) {
      keylength = 128;
    } else {
      throw new NoSuchAlgorithmException("JWT algorithm: " + jwtAlgStr);
    }
  
    byte[] secretKey = CryptoUtils.genKey(keylength);
//    System.out.print("keybytes\n[");
//    for (int i=0; i<(keylength/8)-1; i++) {
//      System.out.print(Integer.toString(secretKey[i]) + ", ");
//    }
//    System.out.println(Integer.toString(secretKey[(keylength/8)-1]) + "]");

    byte[] cipheredKeyBytes = CryptoUtils.rsaoaepEncryptBytes(secretKey, rsaPublicKey);
//    System.out.print("ciphered keybytes\n[");
//    for (int i=0; i<(cipheredKeyBytes.length/8)-1; i++) {
//      System.out.print(Integer.toString(cipheredKeyBytes[i]) + ", ");
//    }
//    System.out.println(Integer.toString(cipheredKeyBytes[(cipheredKeyBytes.length/8)-1]) + "]");

    b64 = Base64.encodeBytes(cipheredKeyBytes, 
        org.xmldap.util.Base64.DONT_BREAK_LINES | org.xmldap.util.Base64.URL);
    sb.append(b64);
//    System.out.println("jwtSymmetricKeySegment base64:" + b64);
    sb.append('.');
    
    SafeObject keyBytes = new SafeObject();
    keyBytes.setText(secretKey);
  
    StringBuffer clearTextBuffer = new StringBuffer(mJsonStr);
    String cipherText = CryptoUtils.encryptAESCBC(clearTextBuffer, keyBytes).toString();
    b64 = Base64.encodeBytes(cipherText.getBytes("utf-8"), 
        org.xmldap.util.Base64.DONT_BREAK_LINES | org.xmldap.util.Base64.URL);
    sb.append(b64);
//    System.out.println("jwtCryptoSegment base64:" + b64);
    
    return sb.toString();
  }
  
  public static String decrypt(String encrypted, RSAPrivateKey rsaPrivateKey) throws Exception {
    String[] split = encrypted.split("\\.");
    String headerB64 = split[0];
    String secretkeyB64 = split[1];
    String jwtCryptoSegmentB64 = split[2];

    String jwtHeaderSegment = new String(Base64.decodeUrl(headerB64));
    
    int keylength;
    JSONObject header = new JSONObject(jwtHeaderSegment);
    String jwtAlgStr = (String) header.get("alg");
    if (ENC_ALG_RE128.equals(jwtAlgStr)) {
      keylength = 128;
    } else if (ENC_ALG_RE192.equals(jwtAlgStr)) {
      keylength = 192;
    } else if (ENC_ALG_RE256.equals(jwtAlgStr)) {
      keylength = 256;
    } else {
      throw new NoSuchAlgorithmException("JWT algorithm: " + jwtAlgStr);
    }

    byte[] cipheredKeyBytes = Base64.decodeUrl(secretkeyB64);
    
    byte[] secretKey = CryptoUtils.decryptRSAOAEP(cipheredKeyBytes, rsaPrivateKey);
    if (8* secretKey.length != keylength) {
      throw new Exception("WebToken.decrypt RSA symmetric key length mismatch: " + secretKey.length + " != " +  keylength);
    }
    
    SafeObject keyBytes = new SafeObject();
    keyBytes.setText(secretKey);
    
    byte[] jwtCryptoSegmentBytes = Base64.decodeUrl(jwtCryptoSegmentB64);
    StringBuffer clearTextBuffer = CryptoUtils.decryptAESCBC(new StringBuffer(new String(jwtCryptoSegmentBytes)), keyBytes);

    return clearTextBuffer.toString();
  }
}