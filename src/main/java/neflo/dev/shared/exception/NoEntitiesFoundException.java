package neflo.dev.shared.exception;

public class NoEntitiesFoundException extends CustomRuntimeException {

    public NoEntitiesFoundException(String errorCode, String message) {
        super(errorCode, message);
    }
}
