package neflo.dev.shared.exception;

public class AuthenticationException extends CustomRuntimeException {

    public AuthenticationException(String errorCode, String message) {
        super(errorCode, message);
    }

    public AuthenticationException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
