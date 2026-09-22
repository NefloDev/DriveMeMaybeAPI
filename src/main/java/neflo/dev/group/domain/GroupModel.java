package neflo.dev.group.domain;

import jakarta.persistence.*;
import lombok.*;
import neflo.dev.group.api.GroupDTO;
import neflo.dev.trip.domain.TripModel;
import neflo.dev.user.domain.UserModel;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.data.domain.Persistable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "GRP_GROUPS")
public class GroupModel implements Persistable<UUID> {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    private UUID id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "GROUP_CODE", nullable = false, unique = true, length = 9)
    private String groupCode;

    @Lob
    @Column(name = "PFP")
    private String pfp;

    @OneToMany(
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @JoinColumn(name = "GRP_ID")
    private Set<TripModel> trips = new HashSet<>();

    @ManyToMany(mappedBy = "groups")
    private Set<UserModel> members = new HashSet<>();

    @Override
    public boolean isNew() {
        return id == null;
    }

    public boolean equalsDto(GroupDTO dto) {
        if (dto == null) return false;

        return new EqualsBuilder()
                .append(name, dto.name())
                .isEquals();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        GroupModel that = (GroupModel) o;

        return new EqualsBuilder()
                .append(id, that.id)
                .append(name, that.name)
                .append(groupCode, that.groupCode)
                .append(pfp, that.pfp)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(name)
                .append(groupCode)
                .append(pfp)
                .toHashCode();
    }
}
