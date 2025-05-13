package cs309.stocks.Transactions;

import cs309.stocks.Portfolios.Portfolio;
import cs309.stocks.Users.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {
    List<Transaction> findByUser(User user);

    List<Transaction> findByPortfolio(Portfolio portfolio);
}
