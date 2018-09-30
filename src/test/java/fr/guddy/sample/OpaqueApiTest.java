package fr.guddy.sample;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import fr.guddy.sample.ciphering.ciphers.AesDecryptCipher;
import fr.guddy.sample.ciphering.ciphers.AesEncryptCipher;
import fr.guddy.sample.ciphering.ciphers.EncryptionCipher;
import fr.guddy.sample.ciphering.keys.AesSha256Key;
import fr.guddy.sample.ciphering.passphrases.DefaultPassphrase;
import fr.guddy.sample.ciphering.strings.Base64PlainText;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public final class OpaqueApiTest {
    private final AesSha256Key key = new AesSha256Key(new DefaultPassphrase());
    private final OpaqueApi api = new OpaqueApi(
            7000,
            new AesDecryptCipher(key)
    );
    private final EncryptionCipher encryptCipher = new AesEncryptCipher(key);

    @Before
    public void setup() {
        api.start();
    }

    @After
    public void teardown() {
        api.stop();
    }

    @Test
    public void testKoNotOpaque() throws UnirestException {
        final HttpResponse<String> resp = Unirest.get("http://localhost:7000/greetings/romain").asString();
        assertThat(resp.getStatus()).isEqualTo(403);
    }

    @Test
    public void testKoOpaqueButUnknown() throws UnirestException {
        final String encodedPath = new Base64PlainText("hello/Romain", encryptCipher).secret();
        final String url = String.format("http://localhost:7000/%s", encodedPath);
        final HttpResponse<String> resp = Unirest.get(url).asString();
        assertThat(resp.getStatus()).isEqualTo(404);
    }

    @Test
    public void testOk() throws UnirestException {
        final String encodedPath = new Base64PlainText("greetings/Romain123", encryptCipher).secret();
        final String url = String.format("http://localhost:7000/%s", encodedPath);
        final HttpResponse<String> resp = Unirest.get(url).asString();
        assertThat(resp.getStatus()).isEqualTo(200);
        assertThat(resp.getBody()).isEqualToIgnoringCase("Hello Romain123");
    }
}