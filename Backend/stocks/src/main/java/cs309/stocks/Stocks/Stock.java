package cs309.stocks.Stocks;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cs309.stocks.Portfolios.Portfolio;
import cs309.stocks.Transactions.Transaction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String ticker;
    private int shares;

    @ManyToOne
    @JoinColumn(name = "portfolio_id")
    @JsonIgnore
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Portfolio portfolio;

    @Transient
    private double price;

    public Stock(Transaction t, Portfolio portfolio) {
        this.ticker = t.getTicker();
        this.shares = t.getShares();
        this.portfolio = portfolio;
    }
}
