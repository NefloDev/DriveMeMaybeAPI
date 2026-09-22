package neflo.dev.user.infrastructure.persistence;

import neflo.dev.user.domain.LanguagesModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LanguagesRepository extends JpaRepository<LanguagesModel, UUID> {

    @Query("select l from LanguagesModel l where upper(l.code) = upper(:code)")
    LanguagesModel findByCode(@Param("code") String code);

}
