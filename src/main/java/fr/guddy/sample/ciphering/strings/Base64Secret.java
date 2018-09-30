package fr.guddy.sample.ciphering.strings;

import fr.guddy.sample.ciphering.ciphers.EncryptionCipher;
import io.vavr.control.Try;
import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;

public final class Base64Secret implements Secret {
    private final String original;
    private final Cipher cipher;

    public Base64Secret(final String original, final Cipher cipher) {
        this.original = original;
        this.cipher = cipher;
    }

    public Base64Secret(final String original, final EncryptionCipher cipher) {
        this(original, cipher.cipher());
    }

    @Override
    public String plainText() {
        final byte[] encryptedData = Base64.decodeBase64(original);
        final byte[] data = Try.of(() -> cipher.doFinal(encryptedData)).get();
        return new String(data, Charsets.UTF_8);
    }
}
