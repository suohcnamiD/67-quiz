package net.stachetopia.template.web.user;

import net.stachetopia.template.web.authorization.Role;
import net.stachetopia.template.web.user.exception.UserNotFoundException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ApplicationUserService {

    private final ApplicationUserRepository applicationUserRepository;

    public List<String> getUserRolesAsStringList(UserDetails userDetails) {
        ApplicationUser user = getAuthenticatedUserFromDetails(userDetails);
        return user.getRoles().stream().map(Role::getName).toList();
    }

    public ApplicationUserService(ApplicationUserRepository applicationUserRepository) {
        this.applicationUserRepository = applicationUserRepository;
    }

    public ApplicationUser getAuthenticatedUserFromDetails(UserDetails userDetails) {
        try {
            return getUserFromDetails(userDetails);
        } catch (UserNotFoundException e) {
            throw new RuntimeException("Authenticated user not found");
        }
    }

    public ApplicationUser getUserFromDetails(UserDetails userDetails) throws UserNotFoundException {
        Optional<ApplicationUser> userOptional = applicationUserRepository.findByUsername(userDetails.getUsername());
        if (userOptional.isEmpty()) throw new UserNotFoundException();
        return userOptional.get();
    }
}
