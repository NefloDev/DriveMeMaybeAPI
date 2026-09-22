package neflo.dev.trip.infrastructure.mapper;

import neflo.dev.trip.api.TripCreateDTO;
import neflo.dev.trip.api.TripDTO;
import neflo.dev.trip.api.TripRequestDTO;
import neflo.dev.trip.domain.TripModel;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface TripMapper {

    @Mapping(target = "driver", source = "driver.nickname")
    @Mapping(target = "tripId", source = "id")
    TripRequestDTO entityToRequestDTO(TripModel entity);

    @Mapping(target = "driver", source = "driver.nickname")
    @Mapping(target = "driverId", source = "driver.id")
    TripDTO entityToDTO(TripModel entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "driver", ignore = true)
    @Mapping(target = "passengers", ignore = true)
    void updateEntity(@MappingTarget TripModel entity, TripCreateDTO dto);

}
