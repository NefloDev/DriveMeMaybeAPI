package neflo.dev.auth.api;

import neflo.dev.auth.application.AuthenticationService;
import neflo.dev.auth.application.GoogleAuthenticationService;
import neflo.dev.user.api.UserDTO;
import neflo.dev.user.api.UserLoginDTO;
import neflo.dev.user.api.UserPreferences;
import neflo.dev.user.domain.UserModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

    @Mock
    AuthenticationService authenticationService;

    @Mock
    GoogleAuthenticationService googleAuthenticationService;

    @InjectMocks
    AuthenticationController underTest;

    @Test
    void should_signup_when_dtoGiven() {
        UserDTO dto = new UserDTO("ana@example.com", Optional.of("secret"), "Ana", "ana", Optional.empty(),
                new UserPreferences(false, "EN"));
        LoginResponse expected = new LoginResponse("jwt", 1L);

        given(authenticationService.signup(dto)).willReturn(expected);

        assertThat(underTest.registerUser(dto).getBody()).isEqualTo(expected);
    }

    @Test
    void should_login_when_credentialsGiven() {
        UserLoginDTO dto = new UserLoginDTO("ana@example.com", "secret");
        LoginResponse expected = new LoginResponse("jwt", 1L);

        given(authenticationService.authenticate(dto)).willReturn(expected);

        assertThat(underTest.authenticateUser(dto).getBody()).isEqualTo(expected);
    }

    @Test
    void should_refresh_when_principalGiven() {
        UserModel user = UserModel.builder().id(UUID.randomUUID()).email("ana@example.com").build();
        user.setGroups(new HashSet<>());
        user.setTrips(new HashSet<>());
        LoginResponse expected = new LoginResponse("refreshed", 2L);

        given(authenticationService.refreshToken(user)).willReturn(expected);

        assertThat(underTest.authenticateUser(user).getBody()).isEqualTo(expected);
    }

    @Test
    void should_loginWithGoogle_when_tokenGiven() {
        LoginResponse expected = new LoginResponse("google-jwt", 3L);

        given(googleAuthenticationService.authenticate("google-id-token")).willReturn(expected);

        assertThat(underTest.authenticateGoogleUser(new GoogleLoginDTO("google-id-token")).getBody())
                .isEqualTo(expected);
    }
}
