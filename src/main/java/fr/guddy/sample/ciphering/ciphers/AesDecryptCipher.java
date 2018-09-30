package fr.guddy.sample.ciphering.ciphers;

import fr.guddy.sample.ciphering.keys.EncryptionKey;

import javax.crypto.Cipher;

public final class AesDecryptCipher implements EncryptionCipher {
    private final EncryptionCipher delegate;

    public AesDecryptCipher(final EncryptionCipher delegate) {
        this.delegate = delegate;
    }

    public AesDecryptCipher(final EncryptionKey key) {
        this(new AesCipher(key, Cipher.DECRYPT_MODE));
    }

    @Override
    public Cipher cipher() {
        return delegate.cipher();
    }
}
