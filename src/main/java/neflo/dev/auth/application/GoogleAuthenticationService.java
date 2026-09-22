package neflo.dev.auth.application;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import lombok.extern.slf4j.Slf4j;
import neflo.dev.auth.api.LoginResponse;
import neflo.dev.shared.exception.UnexpectedException;
import neflo.dev.shared.exception.ValidationException;
import neflo.dev.shared.security.JwtService;
import neflo.dev.user.domain.UserModel;
import neflo.dev.user.infrastructure.persistence.UserRepository;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GoogleAuthenticationService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final GoogleIdTokenVerifier verifier;

    public GoogleAuthenticationService(UserRepository userRepository, JwtService jwtService, GoogleIdTokenVerifier verifier) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.verifier = verifier;
    }

    public LoginResponse authenticate(String idToken) {
        GoogleIdToken googleIdToken;
        try {
            googleIdToken = verifier.verify(idToken);
        } catch (Exception e) {
            throw new UnexpectedException("unable-google-login", "We had a problem performing google login.", e);
        }

        if (googleIdToken == null) {
            throw new ValidationException("invalid-google-token", "Invalid google token.");
        }

        GoogleIdToken.Payload payload = googleIdToken.getPayload();

        String email = payload.getEmail();

        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new ValidationException("email-not-verified", "Google email is not verified.");
        }

        UserModel user = userRepository.findByEmail(email)
                .orElseGet(() -> createUser(email, payload));

        log.info("DriveMeMaybeAPIAPI.Authentication >> Google Login :: Authentication successful");

        String token = jwtService.generateToken(user);
        log.info("DriveMeMaybeAPIAPI.Authentication >> Google Login :: Token generated");

        return new LoginResponse(token, jwtService.getExpirationTime());
    }

    private UserModel createUser(String email, GoogleIdToken.Payload payload) {
        return userRepository.save(UserModel.builder()
                .email(email)
                .name((String) payload.get("name"))
                .build());
    }

}
