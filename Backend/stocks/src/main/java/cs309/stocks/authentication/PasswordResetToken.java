package cs309.stocks.authentication;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cs309.stocks.Users.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class PasswordResetToken {
    public static final int EXPIRATION_MINUTES = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(unique = true)
    private String token;
    private Instant createdAt;

    @OneToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private User user;

    public PasswordResetToken(String token, User user, Instant createdAt) {
        this.token = token;
        this.user = user;
        this.createdAt = createdAt;
    }
}
