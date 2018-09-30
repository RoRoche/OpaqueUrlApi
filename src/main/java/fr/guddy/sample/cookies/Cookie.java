package fr.guddy.sample.cookies;

import io.javalin.Context;

public interface Cookie {
    boolean isPresent();

    String value();

    Context populate(final Context context);

    void clear(final Context context);
}
