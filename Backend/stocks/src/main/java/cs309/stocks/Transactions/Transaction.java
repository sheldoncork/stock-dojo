package cs309.stocks.Transactions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cs309.stocks.Portfolios.Portfolio;
import cs309.stocks.Users.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.sql.Timestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String ticker;
    private double price;
    private int shares;
    private Timestamp transactionDate;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne
    @JoinColumn(name = "portfolio_id")
    @JsonIgnore
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Portfolio portfolio;

    public Transaction(String ticker, int shares) {
        this.ticker = ticker;
        this.shares = shares;
    }
}
