package neflo.dev.trip.api;

import lombok.RequiredArgsConstructor;
import neflo.dev.trip.application.TripService;
import neflo.dev.user.domain.UserModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/trips")
public class TripController {

    private final TripService service;

    @PostMapping("/{groupId}/trip")
    public ResponseEntity<TripDTO> createTrip(@AuthenticationPrincipal UserModel user, @PathVariable("groupId") UUID groupId, @RequestBody TripCreateDTO dto) {
        return ResponseEntity.ok(service.createTrip(user.getId(), groupId, dto));
    }

    @PutMapping("/{groupId}/{tripId}")
    public ResponseEntity<TripDTO> updateTrip(@AuthenticationPrincipal UserModel user, @PathVariable("groupId") UUID groupId, @PathVariable("tripId") UUID tripId, @RequestBody TripCreateDTO dto) {
        return ResponseEntity.ok(service.updateTrip(user.getId(), groupId, tripId, dto));
    }

    @GetMapping("/{groupId}/{tripId}")
    public ResponseEntity<TripDTO> getTripDetail(@AuthenticationPrincipal UserModel user, @PathVariable("groupId") UUID groupId, @PathVariable("tripId") UUID tripId) {
        return ResponseEntity.ok(service.getTripDetail(user.getId(), groupId, tripId));
    }

    @DeleteMapping("/{groupId}/{tripId}")
    public ResponseEntity<Void> deleteTrip(@AuthenticationPrincipal UserModel user, @PathVariable("groupId") UUID groupId, @PathVariable("tripId") UUID tripId) {
        service.deleteTrip(user.getId(), groupId, tripId);

        return ResponseEntity.ok().build();
    }

}
