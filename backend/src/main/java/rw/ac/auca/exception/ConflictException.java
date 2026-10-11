package rw.ac.auca.exception;

/** 409: the request clashes with existing data (duplicate account, already paid, ...). */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
