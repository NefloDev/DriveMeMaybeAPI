package neflo.dev.trip.domain;

import jakarta.persistence.*;
import lombok.*;
import neflo.dev.group.domain.GroupModel;
import neflo.dev.trip.api.TripCreateDTO;
import neflo.dev.user.domain.UserModel;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.data.domain.Persistable;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "GRP_TRIPS")
public class TripModel implements Persistable<UUID> {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    private UUID id;

    @OneToOne()
    @JoinColumn(name = "GRP_ID", referencedColumnName = "ID")
    private GroupModel group;

    @OneToOne()
    @JoinColumn(name = "DRIVER", referencedColumnName = "ID")
    private UserModel driver;

    @Column(name = "DATE")
    private LocalDate date;

    @Column(name = "DURATION_MINUTES")
    private Integer durationMinutes;

    @Column(name = "DISTANCE_KM")
    private Integer distanceKm;

    @Column(name = "ORIGIN")
    private String origin;

    @Column(name = "DESTINATION")
    private String destination;

    @Column(name = "NOTES")
    private String notes;

    @ManyToMany(
            cascade = {
                    CascadeType.PERSIST,
                    CascadeType.MERGE
            }
    )
    @JoinTable(
            name = "REL_TRIP_PASSENGERS",
            joinColumns = @JoinColumn(name = "TRP_ID"),
            inverseJoinColumns = @JoinColumn(name = "USR_ID")
    )
    private Set<UserModel> passengers = new HashSet<>();

    @Override
    public boolean isNew() {
        return id == null;
    }

    public boolean equalsDto(TripCreateDTO dto) {
        if (dto == null) return false;

        return new EqualsBuilder()
                .append(driver.getId(), dto.driver())
                .append(date, dto.date())
                .append(durationMinutes, dto.durationMinutes())
                .append(distanceKm, dto.distanceKm())
                .isEquals();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        TripModel tripModel = (TripModel) o;

        return new EqualsBuilder()
                .append(id, tripModel.id)
                .append(group, tripModel.group)
                .append(driver, tripModel.driver)
                .append(date, tripModel.date)
                .append(durationMinutes, tripModel.durationMinutes)
                .append(distanceKm, tripModel.distanceKm)
                .append(origin, tripModel.origin)
                .append(destination, tripModel.destination)
                .append(notes, tripModel.notes)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(group)
                .append(driver)
                .append(date)
                .append(durationMinutes)
                .append(distanceKm)
                .append(origin)
                .append(destination)
                .append(notes)
                .toHashCode();
    }
}
