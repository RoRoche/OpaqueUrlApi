package fr.guddy.sample;

import fr.guddy.sample.ciphering.ciphers.EncryptionCipher;
import fr.guddy.sample.exceptions.DecryptUrlException;
import io.javalin.Javalin;

public final class OpaqueApi {
    private final Javalin app;
    private final int port;

    public OpaqueApi(final Javalin app, final int port) {
        this.app = app;
        this.port = port;
    }

    public OpaqueApi(final int port, final EncryptionCipher cipher) {
        this(
                Javalin.create()
                        .before(new BeforeHandler(cipher)::handleBefore)
                        .get(
                                "/greetings/:user",
                                ctx ->
                                        ctx.result(String.format("Hello %s", ctx.pathParam("user")))
                        )
                        .post(
                                "/room",
                                ctx ->
                                        ctx.result("OK")
                        )
                        .exception(DecryptUrlException.class, (e, ctx) -> ctx.status(403)),
                port
        );
    }

    public void start() {
        app.start(port);
    }

    public void stop() {
        app.stop();
    }
}
