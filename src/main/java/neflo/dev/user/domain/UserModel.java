package neflo.dev.user.domain;

import jakarta.persistence.*;
import lombok.*;
import neflo.dev.group.domain.GroupModel;
import neflo.dev.trip.domain.TripModel;
import neflo.dev.user.api.UserDTO;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.data.domain.Persistable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "USR_USERS")
public class UserModel implements Persistable<UUID>, UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    private UUID id;

    @Column(name = "EMAIL")
    private String email;

    @Column(name = "PASSWORD")
    private String password;

    @Column(name = "NAME")
    private String name;

    @Column(name = "NICKNAME")
    private String nickname;

    @Column(name = "PFP")
    private String pfp;

    @Column(name = "PREF_DARK_MODE")
    private Boolean prefDarkMode;

    @OneToOne()
    @JoinColumn(name = "PREF_LANG", referencedColumnName = "ID")
    private LanguagesModel prefLang;

    @ManyToMany
    @JoinTable(
            name = "REL_GROUP_MEMBERS",
            joinColumns = @JoinColumn(name = "USR_ID"),
            inverseJoinColumns = @JoinColumn(name = "GRP_ID")
    )
    private Set<GroupModel> groups = new HashSet<>();

    @ManyToMany(mappedBy = "passengers")
    private Set<TripModel> trips = new HashSet<>();

    @Override
    public boolean isNew() {
        return id == null;
    }

    public boolean equalsDto(UserDTO dto) {
        if (dto == null) return false;

        return new EqualsBuilder()
                .append(email, dto.email())
                .append(name, dto.name())
                .append(nickname, dto.nickname())
                .append(pfp, dto.pfp())
                .isEquals();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        UserModel userModel = (UserModel) o;

        return new EqualsBuilder()
                .append(id, userModel.id)
                .append(email, userModel.email)
                .append(password, userModel.password)
                .append(name, userModel.name)
                .append(nickname, userModel.nickname)
                .append(pfp, userModel.pfp)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(email)
                .append(password)
                .append(name)
                .append(nickname)
                .append(pfp)
                .toHashCode();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getUsername() {
        return this.email;
    }
}
