package io.pijun.george.crypto;

import io.pijun.george.Hex;

public class PKEncryptedMessage {

    public byte[] cipherText;
    public byte[] nonce;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("PKEncryptedMessage:\n");
        sb.append("cipherText: ");
        sb.append(Hex.toHexString(cipherText));
        sb.append('\n');
        sb.append("nonce: ");
        sb.append(Hex.toHexString(nonce));

        return sb.toString();
    }
}