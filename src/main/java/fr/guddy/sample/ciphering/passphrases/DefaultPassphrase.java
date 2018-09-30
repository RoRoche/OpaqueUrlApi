package fr.guddy.sample.ciphering.passphrases;

public final class DefaultPassphrase implements Passphrase {
    @Override
    public String value() {
        return "Your Default Security PassPhrase";
    }
}
