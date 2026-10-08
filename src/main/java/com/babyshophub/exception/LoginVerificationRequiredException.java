package com.babyshophub.exception;

public class LoginVerificationRequiredException extends RuntimeException {

    public LoginVerificationRequiredException(String message) {
        super(message);
    }
}
