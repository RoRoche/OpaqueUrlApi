package fr.guddy.sample.cookies;

import io.javalin.Context;

import java.util.Map;

public final class RefererCookie implements Cookie {

    private static final String REFERER = "Referer";
    private final String value;

    public RefererCookie(final String value) {
        this.value = value;
    }

    public RefererCookie(final Map<String, String> cookies) {
        this(cookies.getOrDefault(REFERER, null));
    }

    public RefererCookie(final Context context) {
        this(context.cookieMap());
    }

    @Override
    public boolean isPresent() {
        return value != null && !value.isEmpty();
    }

    @Override
    public String value() {
        return value;
    }

    @Override
    public Context populate(final Context context) {
        return context.cookie(REFERER, value);
    }

    @Override
    public void clear(final Context context) {
        context.removeCookie(REFERER);
    }
}
