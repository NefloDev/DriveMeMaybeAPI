package neflo.dev.auth.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import neflo.dev.auth.application.AuthenticationService;
import neflo.dev.auth.application.GoogleAuthenticationService;
import neflo.dev.user.api.UserDTO;
import neflo.dev.user.api.UserLoginDTO;
import neflo.dev.user.domain.UserModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final GoogleAuthenticationService googleAuthenticationService;

    @PostMapping("/signup")
    public ResponseEntity<LoginResponse> registerUser(@RequestBody UserDTO userDTO) {
        log.info("DriveMeMaybeAPIAPI.Authentication >> SignUp :: START");
        return ResponseEntity.ok(authenticationService.signup(userDTO));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticateUser(@RequestBody UserLoginDTO loginDTO) {
        log.info("DriveMeMaybeAPIAPI.Authentication >> Login :: START");
        return ResponseEntity.ok(authenticationService.authenticate(loginDTO));
    }

    @GetMapping("/refresh")
    public ResponseEntity<LoginResponse> authenticateUser(@AuthenticationPrincipal UserModel user) {
        log.info("DriveMeMaybeAPIAPI.Authentication >> Refresh :: START");
        return ResponseEntity.ok(authenticationService.refreshToken(user));
    }

    @PostMapping("/google/login")
    public ResponseEntity<LoginResponse> authenticateGoogleUser(@RequestBody GoogleLoginDTO request) {
        log.info("DriveMeMaybeAPIAPI.Authentication >> Google Login :: START");
        return ResponseEntity.ok(googleAuthenticationService.authenticate(request.idToken()));
    }

}
