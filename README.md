# 	Beginner's guide to opaque URLs API

![Elegant Objects](https://www.elegantobjects.org/badge.svg)
![intellij-idea](https://www.elegantobjects.org/intellij-idea.svg)

## Requirements

As front-end developer (mobile and Web), I'm used to consuming REST API. Smart libraries like Retrofit  (Android), Unirest (Java) and Axios (JavsScript) allow me to do so simply, neatly, and easily. All is neat, easy to set up and use. The only criterion that remains is security. Indeed, if a bad guy is sniffing network calls (with Wireshark, for example), he can see the calls I make, with the URLs and the REST contract clearly visible and understandable.
To prevent this, I need to add a bit more complexity. While it's impossible to hide everything, we can make it much more difficult to understand.

## Concepts

The API base URL can't be touched. But we can cipher the path and parameters with a symmetric technique. Both front-end and back-end share the secret key to build the "opaque" URL. The client collects all the data to build its request. Once the plaintext URL is ready, the client can encrypt it and perform the call.
When the back-end receives the call, a pre-execution hook is called to decrypt the URL. If the decryption fails (for example the URL was encrypted with the wrong key), it throws an exception. Otherwise, the call is redirected to the proper endpoint.

## Implementation

To build my back-end, I set-up a Java/Gradle-based stack. I chose the Javalin Web framework because it's lightweight, easy to use and efficient. It also has convenient and easy-to-use ["before handlers"](https://javalin.io/documentation#before-handlers). To manage encoded URLs, I use the Apache Commons Codec library. For a bit of convenience, I use the Vavr library. I really love its [Try API](https://www.vavr.io/vavr-docs/#_try). Here are the dependencies in my `build.gradle`:

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

## Ciphering concerns

In order to define ciphering portions, we need to:

1. define a passphrase, i.e. the secret to use to encrypt and decrypt URL (see `Passphrase` interface and its `DefaultPassphrase` implementation):

```java
public final class DefaultPassphrase implements Passphrase {
    @Override
    public String value() {
        return "Your Default Security PassPhrase";
    }
}
```

2. define the way to build `java.security.Key`:

```java
@Override
public Key key() {
    final MessageDigest digester = Try.of(() ->
        MessageDigest.getInstance("SHA-256")
    ).get();
    Try.run(() -> 
        digester.update(String.valueOf(password.value()).getBytes(Charsets.UTF_8.name()))
    );
    final byte[] key = digester.digest();
    return new SecretKeySpec(key, "AES");
}
```

3. set-up a `Cipher` instance to encrypt and decrypt the URL (see `EncryptionCipher` interface and its `AesCipher` implementations; the latter is decorated by `AesEncryptCipher` for encryption and `AesDecryptCipher` for decryption)
4. define the way `String`s are going to be encoded in the application (see `PlainText` interface and its `Base64PlainText` implementation):

```java
public final class Base64PlainText implements PlainText {
    // ...
    @Override
    public String secret() {
        final byte[] dataToSend = original.getBytes(Charsets.UTF_8);
        final byte[] encryptedData = Try.of(() -> cipher.doFinal(dataToSend)).get();
        return Base64.encodeBase64URLSafeString(encryptedData);
    }
}
```

5. define the reverse operation to decrypt a `String` (see `Secret` interface and its `Base64Secret` implementation):

```java
public final class Base64Secret implements Secret {
    // ...
    @Override
    public String plainText() {
        final byte[] encryptedData = Base64.decodeBase64(original);
        final byte[] data = Try.of(() -> cipher.doFinal(encryptedData)).get();
        return new String(data, Charsets.UTF_8);
    }
}
```

## Redirection concerns

The REST consumer builds the URL using the secret mechanism, then calls it with something that looks like `https://{host}/{secret}`. Our API then defines all routes in the traditional way:

```java
Javalin.create()
    .get(
        "/greetings/:user",
        ctx ->
            ctx.result(String.format("Hello %s", ctx.pathParam("user")))
)
```

When receiving a call to a secret path, the API has to resolve it (i.e., determine the plaintext call behind it) and redirect call to the proper URL. To do so, we simply call Javalin's `Context::redirect(String)`.

### Referrer cookie

I need to determine if my call comes the decryption mechanism or a direct call.
To do so, I use a "referrer" cookie, which acts as a witness of my previous opaque call.

### Handler and URL checking

To put it all together, I specified a handler in Javalin's configuration.
This handler catches every call to the API.
If a referrer cookie is present, it checks its validity (if the actual URI matches the decrypted original URI) and redirects to the plain call.
If there is no referrer cookie, it tries to decrypt the URI: if it succeeds, it redirects the call, or else it throws a dedicated exception.
Here is the logic:

```java
public void handleBefore(final Context ctx) {
    final String path = ctx.path().substring(1);
    final referrerCookie referrerCookie = new referrerCookie(ctx);
    if (referrerCookie.isPresent()) {
        new OpaqueUrlRedirection(cipher, referrerCookie).check(path, ctx);
    } else {
        new OpaqueUrlRedirection(path, cipher).redirect(ctx);
    }
}
```

where `OpaqueUrlRedirection` looks like:

```java
public final class OpaqueUrlRedirection implements Redirection {

    private final Secret secret;
    private final Cookie cookie; // the referrer cookie

    // Constructors

    @Override
    public void redirect(final Context context) {
        cookie.populate(context);
        context.redirect(secret.plainText());
    }

    @Override
    public void check(final String path, final Context context) {
        if (!path.equalsIgnoreCase(secret.plainText())) {
            context.status(403);
        }
        cookie.clear(context);
    }
}
```

Changing the context's status (with the 403 HTTP status code) is sufficient to stop the call.

## Unit tests

To perform the requests, I use [unirest](http://unirest.io/) as a REST Java client.

I configure my test to start/stop the API, as follows:

```java
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
```

Here are my 3 basic tests using unirest:

```java
@Test
public void testKoNotOpaque() throws UnirestException {
    final HttpResponse<String> resp = Unirest.get("http://localhost:7000/greetings/romain").asString();
    assertThat(resp.getStatus()).isEqualTo(403);
}

@Test
public void testKoOpaqueButUnknown() throws UnirestException {
    final String encodedPath = new Base64PlainText("hello/Romain", encryptCipher).secret(); // "hello" instead of "greetings"
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
```

## To go further

- do not send the encoded URL and plaintext URL in the same request, but a salted hash instead
- add dynamic (i.e., variable) elements to build the passphrase
    - for example, the client may include the timestamp in the headers, then this one is used to compute the passphrase dynamically
- use [REST-Assured](http://rest-assured.io/) or [Karate DSL](https://intuit.github.io/karate/) to write tests in a more fluent way
- cipher JSON content

## Outcome

Hiding endpoints URL this way is a first step to protect your API from basic attacks.
But it's not a silver bullet.
Using pure OOP, I keep things small (single responsibility), cohesive and reusable.

The source code is available [here](https://github.com/RoRoche/OpaqueUrlApi).

## Thanks

- [Adam Bertrand](https://github.com/Hydragyrum) for reviewing and challenging
- [Matthieu Poignant](https://github.com/DarwinOnLine) for discussing the idea

## References

- <http://blog.ploeh.dk/2013/05/01/rest-lesson-learned-avoid-hackable-urls/>
- <https://www.javacube.fr/chiffrer-dechiffrer-simplement-avec-aes-en-java/>
