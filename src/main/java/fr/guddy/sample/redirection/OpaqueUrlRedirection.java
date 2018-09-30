package fr.guddy.sample.redirection;

import fr.guddy.sample.ciphering.ciphers.EncryptionCipher;
import fr.guddy.sample.ciphering.strings.Base64Secret;
import fr.guddy.sample.ciphering.strings.SafeUrlSecret;
import fr.guddy.sample.ciphering.strings.Secret;
import fr.guddy.sample.cookies.Cookie;
import fr.guddy.sample.cookies.RefererCookie;
import io.javalin.Context;

public final class OpaqueUrlRedirection implements Redirection {

    private final Secret secret;
    private final Cookie cookie;

    //region Constructors
    public OpaqueUrlRedirection(final Secret secret, final Cookie cookie) {
        this.secret = secret;
        this.cookie = cookie;
    }

    public OpaqueUrlRedirection(final String cryptedPath, final EncryptionCipher cipher, final RefererCookie refererCookie) {
        this(
                new SafeUrlSecret(
                        new Base64Secret(
                                cryptedPath,
                                cipher
                        )
                ),
                refererCookie
        );
    }

    public OpaqueUrlRedirection(final String cryptedPath, final EncryptionCipher cipher) {
        this(
                cryptedPath,
                cipher,
                new RefererCookie(cryptedPath)
        );
    }

    public OpaqueUrlRedirection(final EncryptionCipher cipher, final RefererCookie refererCookie) {
        this(
                refererCookie.value(),
                cipher,
                refererCookie
        );
    }
    //endregion

    @Override
    public void redirect(final Context context) {
        cookie.populate(context)
                .redirect(secret.plainText());
    }

    @Override
    public void check(final String path, final Context context) {
        if (!path.equalsIgnoreCase(secret.plainText())) {
            context.status(403);
        }
        cookie.clear(context);
    }
}
