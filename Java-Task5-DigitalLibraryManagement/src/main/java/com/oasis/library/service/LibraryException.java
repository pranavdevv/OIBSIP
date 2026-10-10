package com.oasis.library.service;

/** Business-rule violation with a message safe to show to the user. */
public class LibraryException extends RuntimeException {
    public LibraryException(String message) {
        super(message);
    }
}
