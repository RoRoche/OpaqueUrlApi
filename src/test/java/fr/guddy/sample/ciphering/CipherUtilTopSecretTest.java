package fr.guddy.sample.ciphering;

import fr.guddy.sample.ciphering.ciphers.AesDecryptCipher;
import fr.guddy.sample.ciphering.ciphers.AesEncryptCipher;
import fr.guddy.sample.ciphering.keys.AesSha256Key;
import fr.guddy.sample.ciphering.passphrases.DefaultPassphrase;
import fr.guddy.sample.ciphering.strings.Base64PlainText;
import fr.guddy.sample.ciphering.strings.Base64Secret;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public final class CipherUtilTopSecretTest {

    private final AesSha256Key key = new AesSha256Key(new DefaultPassphrase());

    @Test
    public void encrypt() {
        assertThat(
                new Base64PlainText(
                        "greetings/Romain",
                        new AesEncryptCipher(key)
                ).secret()
        ).isEqualToIgnoringCase("LZ1Cndg8ikOegxY6sVPFzaKpKv7domLvmMQHpc7Cbo0");
    }

    @Test
    public void decrypt() {
        assertThat(
                new Base64Secret(
                        "LZ1Cndg8ikOegxY6sVPFzaKpKv7domLvmMQHpc7Cbo0",
                        new AesDecryptCipher(key)
                ).plainText()
        ).isEqualToIgnoringCase("greetings/Romain");
    }
}