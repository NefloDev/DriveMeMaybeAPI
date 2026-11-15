package neflo.dev.shared.exception;

import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class CustomExceptionHandlerTest {

    private final CustomExceptionHandler underTest = new CustomExceptionHandler();

    @Test
    void should_mapNotFound_when_noEntities() {
        ResponseEntity<CustomErrorResponse> result =
                underTest.handleNoEntitiesFound(new NoEntitiesFoundException("group-not-found", "Group not found."));

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.getBody().getErrorCode()).isEqualTo("group-not-found");
        assertThat(result.getBody().getDetail()).isEqualTo("Group not found.");
    }

    @Test
    void should_mapBadRequest_when_validationFails() {
        ResponseEntity<CustomErrorResponse> result =
                underTest.handleValidationException(new ValidationException("non-member-access", "Non members cannot access"));

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody().getErrorCode()).isEqualTo("non-member-access");
    }

    @Test
    void should_mapInternalError_when_databaseFails() {
        ResponseEntity<CustomErrorResponse> result =
                underTest.handleDatabaseException(new DatabaseException("group-not-updated", "Could not update"));

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(result.getBody().getErrorCode()).isEqualTo("group-not-updated");
    }

    @Test
    void should_mapInternalError_when_unexpected() {
        ResponseEntity<CustomErrorResponse> result =
                underTest.handleUnexpectedException(new UnexpectedException("unable-google-login", "Google failed", new RuntimeException("cause")));

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(result.getBody().getErrorCode()).isEqualTo("unable-google-login");
    }

    @Test
    void should_mapUnauthorized_when_authenticationFails() {
        ResponseEntity<CustomErrorResponse> result =
                underTest.handleAuthenticationException(new AuthenticationException("user-registered", "Email in use"));

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(result.getBody().getErrorCode()).isEqualTo("user-registered");
    }

    @Test
    void should_mapForbidden_when_jwtInvalid() {
        ResponseEntity<CustomErrorResponse> result =
                underTest.handleFilterExceptions(new MalformedJwtException("bad token"));

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(result.getBody().getErrorCode()).isEqualTo("authorizationException");
    }

    @Test
    void should_mapInternalError_when_genericException() {
        ResponseEntity<CustomErrorResponse> result =
                underTest.handleGenericException(new RuntimeException("boom"));

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(result.getBody().getErrorCode()).isEqualTo("generic-exception");
        assertThat(result.getBody().getDetail()).contains("unexpected error");
    }
}
