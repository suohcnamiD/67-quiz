package net.stachetopia.template.web.user;

import net.stachetopia.template.web.authorization.Role;
import jakarta.persistence.*;

import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
public class ApplicationUser {

    public ApplicationUser() {}

    public ApplicationUser(
            String username,
            String password,
            String displayName
    ) {
        this.username = username;
        this.password = password;
        this.displayName = displayName;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "password", nullable = false)
    private String password;


    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;

    public Set<Role> getRoles() {
        return roles;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    public UUID getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }
}
