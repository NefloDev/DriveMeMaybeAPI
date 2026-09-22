package neflo.dev.group.infrastructure.mapper;

import neflo.dev.group.api.GroupDTO;
import neflo.dev.group.domain.GroupModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.Optional;

@Mapper(componentModel = "spring")
public interface GroupMapper {

    @Mapping(target = "groupMembers", expression = "java(entity.getMembers().size())")
    @Mapping(target = "groupTrips", expression = "java(entity.getTrips().size())")
    GroupDTO entityToDTO(GroupModel entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "groupCode", ignore = true)
    void updateEntity(@MappingTarget GroupModel entity, GroupDTO dto);

    default String optionalToString(Optional<String> source) {
        return source.orElse(null);
    }

    default Optional<String> stringToOptional(String source) {
        return Optional.ofNullable(source);
    }

}
