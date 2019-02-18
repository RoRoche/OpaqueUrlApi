# Opaque URLs API

![Elegant Objects](https://www.elegantobjects.org/badge.svg)
![intellij-idea](https://www.elegantobjects.org/intellij-idea.svg)

## Requirements

As front-end developer (mobile and Web), I'm used to consuming REST API. Smart libraries like Retrofit  (Android), Unirest (Java) and Axios (JavsScript) allow me to do so simply, neatly, and easily. All is neat, easy to set up and use. The only criterion that remains is security. Indeed, if a bad guy is sniffing network calls (with Wireshark, for example), he will see the calls I make, with the URLs and the REST contract clearly visible and understandable.
To prevent this, I need to add a bit more complexity. While it's impossible to hide everything, we can make it much more difficult to understand.

## Concepts

The API base URL can't be touched. But we can cipher the path and parameters with a symmetric technique. Both front-end and back-end will share the secret key to build the "opaque" URL. The client will collect all the data to build its request. Once the plaintext URL is ready, the client encrypt it and perform is final call.
Back-end side, when receiving the request, a pre-execution hook is performed to check the URL. It gets the path and try to decrypt it the way it knows, if anything goes wrong, it throws an exception. If the URL can be reverted, it redirects the call to the suitable endpoint.

## Implementation

To build my back-end, I'm going to set-up a Java/Gradle-based stack. As a Web framework, I will choose Javalin (lightweight, easy to use and efficient). It has convenient and easy-to-use ["before handlers"](https://javalin.io/documentation#before-handlers). About ciphering concerns, I'm going to use Apache Commons Codec. And for a bit of convenience, I'll use the functionnal-driven library called vavr (I really love its Try API to focus on a specific pro tip). Here is a piece of my `build.gradle`:

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

Next steps are the following:

1. Define passphrase, i.e. the secret to use to encrypt and decrypt URL (see `Passphrase` interface and its `DefaultPassphrase` implementation), here is an example:

```java
public final class DefaultPassphrase implements Passphrase {
    @Override
    public String value() {
        return "Your Default Security PassPhrase";
    }
}
```

2. Defined the way to build `java.security.Key`, here is a short example:

```java
@Override
public Key key() {
    final MessageDigest digester = Try.of(() ->
        MessageDigest.getInstance("SHA-256")
    ).get();
    Try.run(() -> digester.update(String.valueOf(password.value()).getBytes(Charsets.UTF_8.name())));
    final byte[] key = digester.digest();
    return new SecretKeySpec(key, "AES");
}
```

3. Set-up a `Cipher` instance to encrypt and decrypt URL (see `EncryptionCipher` interface and its `AesCipher` implementations ; last implementation is decorated by `AesEncryptCipher` for encryption and `AesDecryptCipher` for decryption)
4. Define the way `String`s are going to be encrypted in the application (see `PlainText` interface and its `Base64PlainText` implementation), here is an example:

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

5. Define the reverse operation to decrypt a `String` (see `Secret` interface and its `Base64Secret` implementation), here is an example:

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

The main idea is the REST consumer build the URL using secret mechanism, then calls it with something that may look like `https://{host}/{secret}`. To keep it simple, our API defines all routes in the traditional way, such as:

```java
Javalin.create()
    .get(
        "/greetings/:user",
        ctx ->
            ctx.result(String.format("Hello %s", ctx.pathParam("user")))
)
```

When receiving a call to a secret path, the API has to resolve it (i.e., determine the clear call behind it) and redirect call to the proper URL. To do so, it enough to call `Context::redirect(String)` with Javalin API.

### Referrer cooking

But I need to keep a trace, i.e. when catching a call to a clear resource, does it come from my redirection concern, or from usual call?
So I decided to have a cookie, which will act as a witness of my previous opaque call. It named it "referrer".

### Handler and URL checking

To put it all together, I need to specify a handler in javalin configuration.
This handler will catch every call to the API.
If a referrer cookie is present, it will check its validity (if the actual URI is matching the decrypted original URI).
If there is no referrer cookie, it will try to decrypt the URI: if it succeeds, it redirects the call, or else it throws a dedicated exception.
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

The fact to change the context's status (here with the 403 HTTP status code) is sufficient to stop the call.

## Unit tests

Now comes the time to validate (sorry, no pure TDD here) the whole things.

First I need a workaround test to get my ciphered URL for the call I want to test. Here is how I get it:

```java
@Test
public void encrypt() {
    assertThat(
            new Base64PlainText(
                    "greetings/Romain",
                    new AesEncryptCipher(new AesSha256Key(new DefaultPassphrase()))
            ).secret()
    ).isEqualToIgnoringCase("LZ1Cndg8ikOegxY6sVPFzaKpKv7domLvmMQHpc7Cbo0");
}
```

Here I use the default passphrase of the application. The first time I run my test, I don't have the ciphered value. It's when I've ran it once that I can get it and update my test.

Now I want to perform the request. To achieve this point, I use [unirest](http://unirest.io/) as a REST Java client.

I have to configure my test to start/stop the API, as follows:

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
```

This validates all the process described above.

## To go further

- add dynamic (i.e., variable) elements to build the passphrase
    - for example, the client may include the timestamp in the headers, then this one is used to compute the passphrase dynamically
- use [REST-Assured](http://rest-assured.io/) or [Karate DSL](https://intuit.github.io/karate/) to write tests in a more fluent way
- cipher JSON content

## Outcome

This is a way to deal with these security concerns. 
Hiding endpoints URL this way is a first step to protect your API from some basic attacks.
Using pure OOP, I keep things small (single responsibility), cohesive and reusable.

The source code is available [here](https://github.com/RoRoche/OpaqueUrlApi).

## References

- <http://blog.ploeh.dk/2013/05/01/rest-lesson-learned-avoid-hackable-urls/>
- <https://www.javacube.fr/chiffrer-dechiffrer-simplement-avec-aes-en-java/>
