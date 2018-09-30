package fr.guddy.sample.ciphering.keys;

import fr.guddy.sample.ciphering.passphrases.Passphrase;
import io.vavr.control.Try;
import org.apache.commons.codec.Charsets;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.MessageDigest;

public final class AesSha256Key implements EncryptionKey {
    private static final String PASS_HASH_ALGORITHM = "SHA-256";
    private static final String KEY_ALGORITHM = "AES";

    private final Passphrase password;

    public AesSha256Key(final Passphrase password) {
        this.password = password;
    }

    @Override
    public Key key() {
        final MessageDigest digester = Try.of(() ->
                MessageDigest.getInstance(PASS_HASH_ALGORITHM)
        ).get();
        Try.run(() -> digester.update(String.valueOf(password.value()).getBytes(Charsets.UTF_8.name())));
        final byte[] key = digester.digest();
        return new SecretKeySpec(key, KEY_ALGORITHM);
    }
}
