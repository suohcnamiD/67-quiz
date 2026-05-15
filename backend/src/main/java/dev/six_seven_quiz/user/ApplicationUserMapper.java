package dev.six_seven_quiz.user;

import dev.six_seven_quiz.authorization.Role;
import dev.six_seven_quiz.user.dto.ApplicationUserDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.Set;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ApplicationUserMapper {
    ApplicationUserDTO toDTO(ApplicationUser user);

    default Set<String> rolesToRoleNames(Set<Role> roles) {
        return roles.stream()
                .map(Role::getName)
                .collect(java.util.stream.Collectors.toSet());
    }
}
