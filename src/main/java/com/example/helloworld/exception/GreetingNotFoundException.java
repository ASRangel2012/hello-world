package com.example.helloworld.exception;

public class GreetingNotFoundException extends RuntimeException {

    public GreetingNotFoundException(String locale) {
        super("No greeting configured for locale '%s'".formatted(locale));
    }
}
