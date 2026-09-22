package neflo.dev.shared.exception;

public class UnexpectedException extends CustomRuntimeException {

    public UnexpectedException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

}
