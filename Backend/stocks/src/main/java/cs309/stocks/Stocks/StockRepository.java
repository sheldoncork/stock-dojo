package cs309.stocks.Stocks;

import cs309.stocks.Portfolios.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, Integer> {
    Stock findByPortfolioAndTicker(Portfolio portfolio, String ticker);
}
