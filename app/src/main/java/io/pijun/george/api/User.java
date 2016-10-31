package io.pijun.george.api;

import android.support.annotation.Nullable;

public class User {

    public String username;
    public byte[] id;
    public byte[] passwordSalt;
    public long passwordHashOperationsLimit;
    public long passwordHashMemoryLimit;
    public byte[] publicKey;
    public byte[] wrappedSecretKey;
    public byte[] wrappedSecretKeyNonce;
    public byte[] wrappedSymmetricKey;
    public byte[] wrappedSymmetricKeyNonce;

}