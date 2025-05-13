package cs309.stocks.Portfolios;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cs309.stocks.Stocks.Stock;
import cs309.stocks.Users.User;
import cs309.stocks.api.FinnHub;
import cs309.stocks.classroom.Classroom;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;
    private double cash;

    @Transient
    private double value;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @OneToMany(mappedBy = "portfolio", fetch = FetchType.EAGER)
    private List<Stock> stocks;

    @ManyToOne
    @JoinColumn(name = "classroom_id")
    @JsonIgnore
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Classroom classroom;

    public Portfolio(String name, double cash, User user, Classroom classroom) {
        this.name = name;
        this.cash = cash;
        this.user = user;
        this.classroom = classroom;
    }

    public void calculateValue(FinnHub api) {
        double value = this.getCash();

        for (Stock stock : this.getStocks()) {
            double price = api.quote(stock.getTicker()).getCurrentPrice();
            value += price * stock.getShares();
            stock.setPrice(price);
        }

        this.value = value;
    }
}
