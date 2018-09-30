package fr.guddy.sample.ciphering.ciphers;

import fr.guddy.sample.ciphering.keys.EncryptionKey;
import io.vavr.control.Try;

import javax.crypto.Cipher;

public final class AesCipher implements EncryptionCipher {
    private static final String CIPHER_ALGORITHM = "AES";

    private final EncryptionKey key;
    private final int mode;

    public AesCipher(final EncryptionKey key, final int mode) {
        this.key = key;
        this.mode = mode;
    }

    @Override
    public Cipher cipher() {
        return Try.of(() -> {
            final Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(mode, key.key());
            return cipher;
        }).get();
    }
}
