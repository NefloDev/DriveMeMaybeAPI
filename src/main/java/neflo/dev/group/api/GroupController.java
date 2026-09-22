package neflo.dev.group.api;

import lombok.RequiredArgsConstructor;
import neflo.dev.group.application.GroupService;
import neflo.dev.trip.api.TripRequestDTO;
import neflo.dev.user.domain.UserModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/groups")
public class GroupController {

    private final GroupService service;

    @PostMapping("/group")
    public ResponseEntity<GroupDTO> createGroup(@AuthenticationPrincipal UserModel user, @RequestBody GroupRequestDTO dto) {
        return ResponseEntity.ok(service.createGroup(user.getId(), dto));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupDTO> getGroupDetail(@AuthenticationPrincipal UserModel user, @PathVariable("groupId") UUID groupId) {
        return ResponseEntity.ok(service.getGroupDetail(user.getId(), groupId));
    }

    @GetMapping("/{groupId}/members/balance")
    public ResponseEntity<List<GroupMemberBalanceDTO>> getGroupMemberBalanceList(@AuthenticationPrincipal UserModel user, @PathVariable("groupId") UUID groupId) {
        return ResponseEntity.ok(service.getGroupMembersBalance(user.getId(), groupId));
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<GroupMemberDTO>> getGroupMemberList(@AuthenticationPrincipal UserModel user, @PathVariable("groupId") UUID groupId) {
        return ResponseEntity.ok(service.getGroupMembers(user.getId(), groupId));
    }

    @GetMapping("/{groupId}/trips")
    public ResponseEntity<List<TripRequestDTO>> getGroupTrips(@AuthenticationPrincipal UserModel user, @PathVariable("groupId") UUID groupId) {
        return ResponseEntity.ok(service.getGroupTrips(user.getId(), groupId));
    }

    @PostMapping("/{groupId}/insights")
    public ResponseEntity<GroupInsights> getGroupInsights(@AuthenticationPrincipal UserModel user, @PathVariable("groupId") UUID groupId, @RequestBody GroupInsightsRequest groupInsightsRequest) {
        return ResponseEntity.ok(service.getGroupInsights(user.getId(), groupId, groupInsightsRequest));
    }

    @PutMapping("/{groupId}/update")
    public ResponseEntity<GroupDTO> updateGroup(@AuthenticationPrincipal UserModel user, @PathVariable("groupId") UUID groupId, @RequestBody GroupDTO dto) {
        return ResponseEntity.ok(service.updateGroup(user.getId(), groupId, dto));
    }

    @PutMapping("/{groupId}/leave")
    public ResponseEntity<Void> leaveGroup(@AuthenticationPrincipal UserModel user, @PathVariable("groupId") UUID groupId) {
        service.leaveGroup(user.getId(), groupId);

        return ResponseEntity.ok().build();
    }
}
