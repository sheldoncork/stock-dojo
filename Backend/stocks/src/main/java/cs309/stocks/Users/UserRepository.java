package cs309.stocks.Users;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {
    User findByUsername(String username);

    User findByEmailIgnoreCase(String email);

    @Transactional
    void deleteByUsername(String username);
}

