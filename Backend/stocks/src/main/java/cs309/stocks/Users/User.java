package cs309.stocks.Users;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cs309.stocks.Portfolios.Portfolio;
import cs309.stocks.classroom.Classroom;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(unique = true) // ensures unique usernames at the database level
    private String username;

    private String password;
    private UserRole role;

    @Column(unique = true) // ensures unique usernames at the database level
    private String email;

    @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Classroom> teacherClassrooms = new ArrayList<>();


    @ManyToMany
    @JoinTable(
            name = "user_classroom",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "classroom_id")
    )
    @JsonIgnore
    private List<Classroom> studentClassrooms = new ArrayList<>(); // Classrooms associated as a student

    @OneToMany(mappedBy = "user", fetch = FetchType.EAGER)
    @JsonIgnore
    private List<Portfolio> portfolios;

    public User(String username, String password, UserRole role, String email) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.email = email;
        portfolios = new ArrayList<>();
    }

    public void deletePortfolio(Portfolio portfolio) {
        portfolios.remove(portfolio);
    }
}
