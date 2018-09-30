package fr.guddy.sample.ciphering.ciphers;

import fr.guddy.sample.ciphering.keys.EncryptionKey;

import javax.crypto.Cipher;

public final class AesEncryptCipher implements EncryptionCipher {
    private final EncryptionCipher delegate;

    public AesEncryptCipher(final EncryptionCipher delegate) {
        this.delegate = delegate;
    }

    public AesEncryptCipher(final EncryptionKey key) {
        this(new AesCipher(key, Cipher.ENCRYPT_MODE));
    }

    @Override
    public Cipher cipher() {
        return delegate.cipher();
    }
}
