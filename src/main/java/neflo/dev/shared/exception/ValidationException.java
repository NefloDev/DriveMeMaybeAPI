package neflo.dev.shared.exception;

public class ValidationException extends CustomRuntimeException {

    public ValidationException(String errorCode, String message) {
        super(errorCode, message);
    }

}
