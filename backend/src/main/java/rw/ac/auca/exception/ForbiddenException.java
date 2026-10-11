package rw.ac.auca.exception;

/** 403: the user is logged in but is not allowed to touch this record. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
