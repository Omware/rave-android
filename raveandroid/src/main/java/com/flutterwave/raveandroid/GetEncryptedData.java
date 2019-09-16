package com.flutterwave.raveandroid;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.scottyab.aescrypt.AESCrypt.encrypt;

@Singleton
public class GetEncryptedData {

    @Inject
    public GetEncryptedData() {
    }

    public String getEncryptedData(String unEncryptedString, String encryptionKey) {

        if (unEncryptedString != null && encryptionKey != null) {
            try {
                return encrypt(unEncryptedString, encryptionKey);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "";
    }

}