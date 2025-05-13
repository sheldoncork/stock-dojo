package cs309.stocks.api;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Recommendation {
    private String period;
    private int buy;
    private int hold;
    private int sell;
    private int strongBuy;
    private int strongSell;
}
