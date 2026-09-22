package neflo.dev.trip.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import neflo.dev.user.domain.UserModel;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "REL_TRIP_PASSENGERS")
public class TripPassenger {

    @Id
    @OneToOne()
    @JoinColumn(name = "TRP_ID", referencedColumnName = "ID")
    private TripModel trip;

    @Id
    @OneToOne()
    @JoinColumn(name = "USR_ID", referencedColumnName = "ID")
    private UserModel passenger;

}
