package fr.guddy.sample;

import fr.guddy.sample.ciphering.ciphers.EncryptionCipher;
import fr.guddy.sample.ciphering.strings.Base64Secret;
import fr.guddy.sample.ciphering.strings.SafeUrlSecret;
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
            final String decrypted = new SafeUrlSecret(
                    new Base64Secret(
                            refererCookie.value(),
                            cipher
                    )
            ).plainText();
            if (!path.equalsIgnoreCase(decrypted)) {
                ctx.status(403);
            }
            refererCookie.clear(ctx);
        } else {
            new OpaqueUrlRedirection(path, cipher).redirect(ctx);
        }
    }
}
