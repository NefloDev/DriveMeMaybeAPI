package neflo.dev.user.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "USR_PREFS_LANGUAGES")
public class LanguagesModel implements Persistable<UUID> {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    private UUID id;

    @Column(name = "CODE", length = 2)
    private String code;

    @Column(name = "FORMAL_NAME")
    private String formalName;

    @Override
    public boolean isNew() {
        return id == null;
    }
}
