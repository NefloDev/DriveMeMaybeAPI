package neflo.dev.auth.application;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import neflo.dev.auth.api.LoginResponse;
import neflo.dev.shared.exception.UnexpectedException;
import neflo.dev.shared.exception.ValidationException;
import neflo.dev.shared.security.JwtService;
import neflo.dev.user.domain.UserModel;
import neflo.dev.user.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class GoogleAuthenticationServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    JwtService jwtService;

    @Mock
    GoogleIdTokenVerifier verifier;

    @Mock
    GoogleIdToken googleIdToken;

    @Mock
    GoogleIdToken.Payload payload;

    @InjectMocks
    GoogleAuthenticationService underTest;

    private void givenValidPayload(String email, boolean verified) {
        given(payload.getEmail()).willReturn(email);
        given(payload.getEmailVerified()).willReturn(verified);
        given(googleIdToken.getPayload()).willReturn(payload);
    }

    @Test
    void should_authenticate_when_existingUser() throws GeneralSecurityException, IOException {
        givenValidPayload("ana@example.com", true);
        given(verifier.verify("id-token")).willReturn(googleIdToken);
        UserModel existing = UserModel.builder().email("ana@example.com").name("Ana").build();
        existing.setGroups(new HashSet<>());
        existing.setTrips(new HashSet<>());

        given(userRepository.findByEmail("ana@example.com")).willReturn(Optional.of(existing));
        given(jwtService.generateToken(existing)).willReturn("jwt");
        given(jwtService.getExpirationTime()).willReturn(7L);

        LoginResponse result = underTest.authenticate("id-token");

        assertThat(result.token()).isEqualTo("jwt");
        assertThat(result.expiresOn()).isEqualTo(7L);
        then(userRepository).should().findByEmail("ana@example.com");
    }

    @Test
    void should_createUser_when_firstGoogleLogin() throws GeneralSecurityException, IOException {
        givenValidPayload("new@example.com", true);
        given(payload.get("name")).willReturn("New User");
        given(verifier.verify("id-token")).willReturn(googleIdToken);
        given(userRepository.findByEmail("new@example.com")).willReturn(Optional.empty());
        UserModel saved = UserModel.builder().email("new@example.com").name("New User").build();
        saved.setGroups(new HashSet<>());
        saved.setTrips(new HashSet<>());

        given(userRepository.save(any(UserModel.class))).willReturn(saved);
        given(jwtService.generateToken(saved)).willReturn("jwt");
        given(jwtService.getExpirationTime()).willReturn(7L);

        LoginResponse result = underTest.authenticate("id-token");

        assertThat(result.token()).isEqualTo("jwt");
        then(userRepository).should().save(any(UserModel.class));
    }

    @Test
    void should_throwUnexpected_when_verifierFails() throws GeneralSecurityException, IOException {
        given(verifier.verify("bad-token")).willThrow(new IOException("network"));

        assertThatThrownBy(() -> underTest.authenticate("bad-token"))
                .isInstanceOf(UnexpectedException.class)
                .hasMessageContaining("google login");
    }

    @Test
    void should_throwValidation_when_tokenNull() throws GeneralSecurityException, IOException {
        given(verifier.verify("id-token")).willReturn(null);

        assertThatThrownBy(() -> underTest.authenticate("id-token"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid google token");
    }

    @Test
    void should_throwValidation_when_emailNotVerified() throws GeneralSecurityException, IOException {
        givenValidPayload("ana@example.com", false);
        given(verifier.verify("id-token")).willReturn(googleIdToken);

        assertThatThrownBy(() -> underTest.authenticate("id-token"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("not verified");
    }
}
