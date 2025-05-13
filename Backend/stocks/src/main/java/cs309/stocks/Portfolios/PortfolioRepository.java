package cs309.stocks.Portfolios;

import cs309.stocks.Users.User;
import cs309.stocks.classroom.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;


public interface PortfolioRepository extends JpaRepository<Portfolio, Integer> {
    Portfolio findById(int id);

    Portfolio findByUserAndClassroom(User user, Classroom classroom);
}
