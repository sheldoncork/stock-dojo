package cs309.stocks.api;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DayStats {
    private String date;
    private double open;
    private double high;
    private double low;
    private double close;
}
