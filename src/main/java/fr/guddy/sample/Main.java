package fr.guddy.sample;

import fr.guddy.sample.ciphering.ciphers.AesDecryptCipher;
import fr.guddy.sample.ciphering.keys.AesSha256Key;
import fr.guddy.sample.ciphering.passphrases.DefaultPassphrase;

public final class Main {
    public static void main(final String[] args) {
        new OpaqueApi(
                7000,
                new AesDecryptCipher(new AesSha256Key(new DefaultPassphrase()))
        ).start();
    }
}
