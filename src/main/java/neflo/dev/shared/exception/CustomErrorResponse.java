package neflo.dev.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Getter
public class CustomErrorResponse {

    private final String errorCode;
    private final String detail;
    private final int statusCode;
    private final String statusName;
    private final LocalDateTime timestamp;

    public CustomErrorResponse(HttpStatus httpStatus, String errorCode, String detail) {
        this.errorCode = errorCode;
        this.detail = detail;
        this.statusCode = httpStatus.value();
        this.statusName = httpStatus.name();
        this.timestamp = LocalDateTime.now();
    }

}
