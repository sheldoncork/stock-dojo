package cs309.stocks.authentication;

import cs309.stocks.Users.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {
    PasswordResetToken findByUser(User user);

    PasswordResetToken findByToken(String token);
}
