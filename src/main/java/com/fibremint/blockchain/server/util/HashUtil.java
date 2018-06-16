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

    public static PublicKey generatePublicKey(byte[] keyBinary) {
        PublicKey publicKey = null;
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("ECDSA", "BC");
            publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(keyBinary));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return publicKey;
    }

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


    public static String getStringFromKey(Key key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public static String getEncodedKey(Key key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public static byte[] getDecodedKey(String keyString) {
        return Base64.getDecoder().decode(keyString);
    }

    public static String getEncodedString(byte[] byteArray) {
        return Base64.getEncoder().encodeToString(byteArray);
    }

    public static byte[] getDecoded(String encodedString) {
        return Base64.getDecoder().decode(encodedString);
    }

    public static String getDifficultyString(int difficulty) {
        return new String(new char[difficulty]).replace('\0', '0');
    }
}