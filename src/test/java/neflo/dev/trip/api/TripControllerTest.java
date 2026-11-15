package neflo.dev.trip.api;

import neflo.dev.trip.application.TripService;
import neflo.dev.user.domain.UserModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class TripControllerTest {

    @Mock
    TripService service;

    @InjectMocks
    TripController underTest;

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID GROUP_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TRIP_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static UserModel principal() {
        UserModel user = UserModel.builder().id(USER_ID).email("ana@example.com").build();
        user.setGroups(new HashSet<>());
        user.setTrips(new HashSet<>());
        return user;
    }

    @Test
    void should_createTrip_when_requested() {
        TripCreateDTO dto = new TripCreateDTO(USER_ID, LocalDate.of(2026, 6, 1), 30, 20, "A", "B", null, List.of(USER_ID));
        TripDTO expected = new TripDTO(TRIP_ID, USER_ID, "ana", dto.date(), 30, 20, "A", "B", null);

        given(service.createTrip(USER_ID, GROUP_ID, dto)).willReturn(expected);

        assertThat(underTest.createTrip(principal(), GROUP_ID, dto).getBody()).isEqualTo(expected);
    }

    @Test
    void should_updateTrip_when_requested() {
        TripCreateDTO dto = new TripCreateDTO(USER_ID, LocalDate.of(2026, 6, 1), 30, 20, "A", "B", null, List.of(USER_ID));
        TripDTO expected = new TripDTO(TRIP_ID, USER_ID, "ana", dto.date(), 30, 20, "A", "B", null);

        given(service.updateTrip(USER_ID, GROUP_ID, TRIP_ID, dto)).willReturn(expected);

        assertThat(underTest.updateTrip(principal(), GROUP_ID, TRIP_ID, dto).getBody()).isEqualTo(expected);
    }

    @Test
    void should_returnDetail_when_requested() {
        TripDTO expected = new TripDTO(TRIP_ID, USER_ID, "ana", LocalDate.of(2026, 6, 1), 30, 20, "A", "B", null);

        given(service.getTripDetail(USER_ID, GROUP_ID, TRIP_ID)).willReturn(expected);

        assertThat(underTest.getTripDetail(principal(), GROUP_ID, TRIP_ID).getBody()).isEqualTo(expected);
    }

    @Test
    void should_deleteTrip_when_requested() {
        var result = underTest.deleteTrip(principal(), GROUP_ID, TRIP_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        then(service).should().deleteTrip(USER_ID, GROUP_ID, TRIP_ID);
    }
}
