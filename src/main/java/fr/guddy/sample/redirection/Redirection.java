package fr.guddy.sample.redirection;

import io.javalin.Context;

public interface Redirection {
    void redirect(final Context context);

    void check(final String path, final Context context);
}
