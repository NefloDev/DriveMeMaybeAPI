package neflo.dev.shared.exception;

public class DatabaseException extends CustomRuntimeException {

    public DatabaseException(String errorCode, String message) {
        super(errorCode, message);
    }

    public DatabaseException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
