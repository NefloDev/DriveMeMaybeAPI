package neflo.dev.user.api;

import lombok.RequiredArgsConstructor;
import neflo.dev.group.api.GroupDTO;
import neflo.dev.user.application.UserService;
import neflo.dev.user.domain.UserModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService service;

    @GetMapping("/profile")
    public ResponseEntity<UserResponseDTO> getUserProfile(@AuthenticationPrincipal UserModel user) {
        return ResponseEntity.ok(service.getUserDetail(user.getId()));
    }

    @GetMapping("/prefs")
    public ResponseEntity<UserPreferencesFilters> getUserPreferencesFilters() {
        return ResponseEntity.ok(service.getUserPreferencesFilters());
    }

    @GetMapping("/groups")
    public ResponseEntity<List<GroupDTO>> getUserGroups(@AuthenticationPrincipal UserModel user) {
        return ResponseEntity.ok(service.getUserGroups(user.getId()));
    }

    @GetMapping("/groups/join/{groupCode}")
    public ResponseEntity<GroupDTO> joinGroup(@AuthenticationPrincipal UserModel user, @PathVariable("groupCode") String groupCode) {
        return ResponseEntity.ok(service.joinGroup(user.getId(), groupCode));
    }

    @PutMapping("/update")
    public ResponseEntity<UserResponseDTO> updateUser(@AuthenticationPrincipal UserModel user, @RequestBody UserDTO dto) {
        return ResponseEntity.ok(service.updateUser(user.getId(), dto));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteUser(@AuthenticationPrincipal UserModel user) {
        service.deleteUser(user.getId());

        return ResponseEntity.ok().build();
    }

}
