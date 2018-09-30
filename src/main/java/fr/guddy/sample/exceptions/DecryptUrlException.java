package fr.guddy.sample.exceptions;

public final class DecryptUrlException extends RuntimeException {
    public DecryptUrlException(final Throwable t) {
        super(t);
    }
}
