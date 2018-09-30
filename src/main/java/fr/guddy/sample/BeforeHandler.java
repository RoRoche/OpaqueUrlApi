package fr.guddy.sample;

import fr.guddy.sample.ciphering.ciphers.EncryptionCipher;
import fr.guddy.sample.ciphering.strings.Base64Secret;
import fr.guddy.sample.exceptions.DecryptUrlException;
import io.javalin.Context;
import io.vavr.control.Try;

public final class BeforeHandler {
    private static final String REFERER = "Referer";

    private final EncryptionCipher cipher;

    public BeforeHandler(final EncryptionCipher cipher) {
        this.cipher = cipher;
    }

    public void handleBefore(final Context ctx) {
        final String path = ctx.path().substring(1);
        if (ctx.cookieMap().containsKey(REFERER)) {
            checkReferer(ctx, path);
        } else {
            final String decrypted = safeDecrypt(path);
            ctx.cookie(REFERER, path)
                    .redirect(decrypted);
        }
    }

    private void checkReferer(final Context ctx, final String path) {
        final String crypted = ctx.cookie(REFERER);
        final String decrypted = safeDecrypt(crypted);
        if (!path.equalsIgnoreCase(decrypted)) {
            ctx.status(403);
        } else {
            ctx.clearCookieStore();
        }
    }

    private String safeDecrypt(final String path) {
        return Try.of(() ->
                new Base64Secret(
                        path,
                        cipher
                ).plainText()
        ).getOrElseThrow(DecryptUrlException::new);
    }
}
