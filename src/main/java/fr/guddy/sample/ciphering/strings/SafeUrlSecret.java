package fr.guddy.sample.ciphering.strings;

import fr.guddy.sample.exceptions.DecryptUrlException;
import io.vavr.control.Try;

public final class SafeUrlSecret implements Secret {
    private final Secret delegate;

    public SafeUrlSecret(final Secret delegate) {
        this.delegate = delegate;
    }

    @Override
    public String plainText() {
        return Try.of(delegate::plainText).getOrElseThrow(DecryptUrlException::new);
    }
}
