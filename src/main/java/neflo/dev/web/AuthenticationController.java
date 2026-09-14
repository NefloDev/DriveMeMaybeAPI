package neflo.dev.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import neflo.dev.exceptions.AuthenticationException;
import neflo.dev.model.dto.GoogleLoginDTO;
import neflo.dev.model.dto.LoginResponse;
import neflo.dev.model.dto.user.UserDTO;
import neflo.dev.model.dto.user.UserLoginDTO;
import neflo.dev.model.entity.UserModel;
import neflo.dev.service.authentication.AuthenticationService;
import neflo.dev.service.authentication.GoogleAuthenticationService;
import neflo.dev.service.authentication.JwtService;
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
