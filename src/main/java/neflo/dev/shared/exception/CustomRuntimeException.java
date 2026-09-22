package neflo.dev.shared.exception;

import lombok.Getter;

@Getter
public class CustomRuntimeException extends RuntimeException {

    private final String errorCode;
    private final Throwable cause;

    public CustomRuntimeException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.cause = null;
    }

    public CustomRuntimeException(String errorCode) {
        super("");
        this.errorCode = errorCode;
        this.cause = null;
    }

    public CustomRuntimeException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.cause = cause;
    }
}
