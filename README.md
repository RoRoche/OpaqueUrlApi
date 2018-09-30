# Opaque URLs API

![Elegant Objects](https://www.elegantobjects.org/badge.svg)
![intellij-idea](https://www.elegantobjects.org/intellij-idea.svg)

## Requirements

As front-end developer (mobile and Web), I'm used to consume REST API. Smart libraries like Retrofit  (Android), Unirest (Java) and Axios (JavsScript) allow me to do it simple. All is neat, easy to set up and use. All me requirements as a developer are fullfilled.
The only criteria that remains is about security. Indeed, what if a bad guy is sniffing network calls (with Wireshark for a exemple). He will see the calls I make, with URLs and REST contract cleary visible and understable.
So to prevent myself for it, I need a little bit more complexity. It's impossible to hide everything, but we can make it digusting to understand.

## Concepts

The API base URL can't be touched. But we can cipher the path and parameters with a symmetric technique. Both front-end and back-end will share the secret key to build the "opaque" URL I'm talking about. The client will collect all the data to build its request. Once the clear URL is ready, the client encrypt it and perform is final call.
Back-end side, when receiving the request, a pre-execution hook is performed to check the URL. It gets the path and try to decrypt it the way it knows, if anything goes wrong, it throws an exception. If the URL can be reverted, it redirects the call to the suitable endpoint.

## Implementation

To build my back-end, I'm going to set-up a Java/Gradle-based stack. As a Web framework, I will choose Javalin (lightweight, easy to use and efficient). About ciphering concerns, I'm going to use Apache Commons Codec. And for a bit of convenience, I'll use the functionnal-driven library called vavr (I really love its Try API to focus on a specific pro tip). Here is a piece of my `build.gradle`:

```groovy
dependencies {
    implementation group: 'commons-codec', name: 'commons-codec', version: '1.11'
    implementation group: 'io.javalin', name: 'javalin', version: '2.1.1'
    implementation group: 'org.slf4j', name: 'slf4j-simple', version: '1.7.25' // optional but recommended when using Javalin
    implementation group: 'io.vavr', name: 'vavr', version: '0.9.2'
}
```

OK, now start with a very simple endpoint that greets the calling user:

```java
Javalin.create()
    .get(
        "/greetings/:user", 
        ctx -> 
            ctx.result(String.format("Hello %s", ctx.pathParam("user")))
    )
```

So let's define the classes that will managed passphrase concerns:

```java
public interface Passphrase {
    String value();
}

public final class DefaultPassphrase implements Passphrase {
    @Override
    public String value() {
        return "Your Default Security PassPhrase";
    }
}
```

Now have a look to the part that will provide the necessary `javax.crypto.Cipher` instances:

```java
public interface EncryptionCipher {
    Cipher cipher();
}

/** The implementation that deals with AES algorithm */
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
```

Here I use the decorator pattern to ease the configuration of my objects.

TODO

## Improvements

To add a little more complexity, I will add a variable element: the request timestamp. The idea is to put a header containing the timestamp representing the moment the call was fired. The key will be computed according to this value. So now, both client and server are sharing a more sophisticated mechanism.

TODO

## Drawbacks

- not convenient to debug
- not a silver bullet (if the bad guy get my source code, if not enough obfuscated, I will reverse-engineer it and get secret to call the API)

## Perspectives

- cipher JSON content

## References

- <http://blog.ploeh.dk/2013/05/01/rest-lesson-learned-avoid-hackable-urls/>
- <https://www.javacube.fr/chiffrer-dechiffrer-simplement-avec-aes-en-java/>