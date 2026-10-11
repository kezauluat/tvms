package rw.ac.auca.exception;

/** 401: wrong credentials or an invalid/expired refresh token. */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
