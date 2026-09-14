package neflo.dev.service.authentication;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import neflo.dev.exceptions.AuthenticationException;
import neflo.dev.exceptions.ValidationException;
import neflo.dev.model.dto.LoginResponse;
import neflo.dev.model.dto.user.UserDTO;
import neflo.dev.model.dto.user.UserLoginDTO;
import neflo.dev.model.entity.UserModel;
import neflo.dev.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public boolean isUserRegistered(UserDTO userDTO) {
        return userRepository.existsByEmail(userDTO.email());
    }

    public LoginResponse signup(UserDTO userDTO) {
        if (isUserRegistered(userDTO)) {
            throw new AuthenticationException("user-registered", "That email is already in use");
        }

        if (userDTO.password().isEmpty()) {
            throw new ValidationException("empty-password", "Password field is empty.");
        }

        UserModel user = UserModel.builder()
                .email(userDTO.email())
                .name(userDTO.name())
                .nickname(userDTO.nickname())
                .password(passwordEncoder.encode(userDTO.password().get()))
                .build();

        if (userDTO.pfp().isPresent()) {
            user.setPfp(userDTO.pfp().get());
        }

        user = userRepository.save(user);
        log.info("DriveMeMaybeAPIAPI.Authentication >> SignUp :: Signup successful");

        String jwtToken = jwtService.generateToken(user);
        log.info("DriveMeMaybeAPIAPI.Authentication >> SignUp :: Token generated");

        return new LoginResponse(jwtToken, jwtService.getExpirationTime());
    }

    public LoginResponse authenticate(UserLoginDTO userLoginDTO) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userLoginDTO.email(), userLoginDTO.password())
            );
        } catch (Exception e) {
            throw new AuthenticationException("user-authentication-exception", "There was an issue authenticating the current user, try again later.");
        }
        log.info("DriveMeMaybeAPIAPI.Authentication >> Login :: Authentication successful");

        UserModel user = userRepository.findByEmail(userLoginDTO.email()).orElseThrow();

        String jwtToken = jwtService.generateToken(user);
        log.info("DriveMeMaybeAPIAPI.Authentication >> Login :: Token generated");

        return new LoginResponse(jwtToken, jwtService.getExpirationTime());
    }

    public LoginResponse refreshToken(UserModel user) {
        String jwtToken = jwtService.generateToken(user);
        log.info("DriveMeMaybeAPIAPI.Authentication >> Refresh :: Token generated");

        return new LoginResponse(jwtToken, jwtService.getExpirationTime());
    }

}
