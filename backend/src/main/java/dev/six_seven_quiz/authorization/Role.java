package dev.six_seven_quiz.authorization;

import dev.six_seven_quiz.user.ApplicationUser;
import jakarta.persistence.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "roles")
public class Role {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @ManyToMany(mappedBy = "roles")
    private List<ApplicationUser> users;

    public UUID getId() {
        return id;
    }

    public List<ApplicationUser> getUsers() {
        return users;
    }

    public String getName() {
        return name;
    }
}
