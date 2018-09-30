package fr.guddy.sample.ciphering.strings;

import fr.guddy.sample.ciphering.ciphers.EncryptionCipher;
import io.vavr.control.Try;
import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;

public final class Base64PlainText implements PlainText {
    private final String original;
    private final Cipher cipher;

    public Base64PlainText(final String original, final Cipher cipher) {
        this.original = original;
        this.cipher = cipher;
    }

    public Base64PlainText(final String original, final EncryptionCipher cipher) {
        this(original, cipher.cipher());
    }

    @Override
    public String secret() {
        final byte[] dataToSend = original.getBytes(Charsets.UTF_8);
        final byte[] encryptedData = Try.of(() -> cipher.doFinal(dataToSend)).get();
        return Base64.encodeBase64URLSafeString(encryptedData);
    }
}
