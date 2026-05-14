package kr.devslab.easypaging.support;

/**
 * Raised when a client-supplied sort expression fails validation.
 */
public class InvalidSortParameterException extends IllegalArgumentException {

    public InvalidSortParameterException(String message, Throwable cause) {
        super(message, cause);
    }
}
