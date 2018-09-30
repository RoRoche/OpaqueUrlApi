package fr.guddy.sample;

import fr.guddy.sample.ciphering.ciphers.EncryptionCipher;
import fr.guddy.sample.cookies.RefererCookie;
import fr.guddy.sample.redirection.OpaqueUrlRedirection;
import io.javalin.Context;

public final class BeforeHandler {
    private final EncryptionCipher cipher;

    public BeforeHandler(final EncryptionCipher cipher) {
        this.cipher = cipher;
    }

    public void handleBefore(final Context ctx) {
        final String path = ctx.path().substring(1);
        final RefererCookie refererCookie = new RefererCookie(ctx);
        if (refererCookie.isPresent()) {
            new OpaqueUrlRedirection(cipher, refererCookie).check(path, ctx);
        } else {
            new OpaqueUrlRedirection(path, cipher).redirect(ctx);
        }
    }
}
