package com.fibremint.blockchain.server.util;

import com.fibremint.blockchain.server.blockchain.Transaction;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.ECPointUtil;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECNamedCurveSpec;
import org.bouncycastle.openssl.*;
import org.bouncycastle.openssl.jcajce.*;

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.*;
import java.util.ArrayList;
import java.util.Base64;

public class HashUtil {

    public static String applySHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) hexString.append(0);
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] applyECDSASignature(PrivateKey privateKey, String input) {
        Signature dsa;
        byte[] output = new byte[0];
        try {
            dsa = Signature.getInstance("ECDSA", "BC");
            dsa.initSign(privateKey);
            byte[] stringByte = input.getBytes();
            dsa.update(stringByte);
            byte[] realSignature = dsa.sign();
            output = realSignature;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return output;
    }

    public static boolean verifyECDSASignature(PublicKey publicKey, String data, byte[] signature) {
        try {
            Signature ecdsaVerify = Signature.getInstance("ECDSA", "BC");
            ecdsaVerify.initVerify(publicKey);
            ecdsaVerify.update(data.getBytes());
            return ecdsaVerify.verify(signature);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /*public static PrivateKey generatePrivateKey(String string) {
        byte[] keyBin = Base64.getDecoder().decode(string.getBytes());
        try {
            ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("prime192v1");
            KeyFactory kf = KeyFactory.getInstance("ECDSA", "BC");
            ECNamedCurveSpec params = new ECNamedCurveSpec("prime192v1", spec.getCurve(), spec.getG(), spec.getN());
            ECPrivateKeySpec privKeySpec = new ECPrivateKeySpec(new BigInteger(keyBin), params);
            return kf.generatePrivate(privKeySpec);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static PublicKey generatePublicKey(String string) {
        byte[] keyBin = getByteArrayFromString(string);
        try {
            ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("prime192v1");
            KeyFactory kf = KeyFactory.getInstance("ECDSA", "BC");
            ECNamedCurveSpec params = new ECNamedCurveSpec("prime192v1", spec.getCurve(), spec.getG(), spec.getN());
            ECPoint point = ECPointUtil.decodePoint(params.getCurve(), keyBin);
            ECPublicKeySpec pubKeySpec = new ECPublicKeySpec(point, params);
            return kf.generatePublic(pubKeySpec);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }*/

    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDSA", "BC");
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            ECGenParameterSpec ecGenParameterSpec = new ECGenParameterSpec("prime192v1");
            keyPairGenerator.initialize(ecGenParameterSpec, secureRandom);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // source:
    // https://www.programcreek.com/java-api-examples/?code=joyent/java-http-signature/java-http-signature-master/common/src/test/java/com/joyent/http/signature/KeyPairLoaderTest.java#
    // TODO: handle exception with throw
    public static void writePrivateKeyToPEM(String fileName, PrivateKey privateKey, String passphrase) {
        try (final JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(new OutputStreamWriter(new FileOutputStream(fileName)))){
            final PEMEncryptor pemEncryptor = new JcePEMEncryptorBuilder("AES-128-CBC")
                    .build(passphrase.toCharArray());
            final JcaMiscPEMGenerator pemGenerator = new JcaMiscPEMGenerator(privateKey, pemEncryptor);

            jcaPEMWriter.writeObject(pemGenerator);
            jcaPEMWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();;
        }
    }

    // TODO: To make verify method?
    // https://connect2id.com/products/nimbus-jose-jwt/openssl-key-generation
    // https://www.programcreek.com/java-api-examples/?code=joyent/java-http-signature/java-http-signature-master/common/src/test/java/com/joyent/http/signature/KeyPairLoaderTest.java#

    public static KeyPair getKeyPairFromPEM(String fileName, String passPhrase) {
        KeyPair keyPair = null;
        try(PEMParser pemParser = new PEMParser(new InputStreamReader(new FileInputStream(fileName)))) {
            final PEMDecryptorProvider pemDecryptorProvider = new JcePEMDecryptorProviderBuilder()
                    .build(passPhrase.toCharArray());
            final PEMEncryptedKeyPair pemEncryptedKeyPair = (PEMEncryptedKeyPair) pemParser.readObject();
            final PEMKeyPair pemKeyPair = pemEncryptedKeyPair.decryptKeyPair(pemDecryptorProvider);
            JcaPEMKeyConverter jcaPEMKeyConverter = new JcaPEMKeyConverter().setProvider("BC");

            keyPair = jcaPEMKeyConverter.getKeyPair(pemKeyPair);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return keyPair;
    }

    public static String getMerkleRoot(ArrayList<Transaction> transactions) {
        int count = transactions.size();
        ArrayList<String> treeLayer = new ArrayList<>();
        ArrayList<String> previousTreeLayer = new ArrayList<>();

        for(Transaction transaction : transactions)
            previousTreeLayer.add(transaction.hash);

        while(count > 1) {
            treeLayer = new ArrayList<>();
            for(int i=1; i < previousTreeLayer.size(); i++)
                treeLayer.add(applySHA256(previousTreeLayer.get(i-1) + previousTreeLayer.get(i)));
            count = treeLayer.size();
            previousTreeLayer = treeLayer;
        }

        return (treeLayer.size() == 1) ? treeLayer.get(0) : "";
    }


    /*public static String getStringFromByteArray(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static String getStringFromKey(Key key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public static byte[] getByteArrayFromString(String string) {
        return Base64.getDecoder().decode(string);
    }*/


    public static String getStringFromByteArray(byte[] bytes) {
        return new String(Base64.getDecoder().decode(bytes));
    }

    public static String getStringFromKey(Key key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public static byte[] getByteArrayFromString(String string) {
        return Base64.getEncoder().encode(string.getBytes());
    }

    public static PrivateKey getPvFromString(String key) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("ECDSA", "BC");
            return keyFactory.generatePrivate(new X509EncodedKeySpec(getByteArrayFromString(key)));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getDifficultyString(int difficulty) {
        return new String(new char[difficulty]).replace('\0', '0');
    }
}