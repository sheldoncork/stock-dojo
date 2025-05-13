package cs309.stocks.api;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Quote {
    private double currentPrice;
    private double change;
    private double percentChange;
    private double high;
    private double low;
    private double open;
    private double previousClose;
    private int time;

    public Quote(RawQuote q) {
        this.currentPrice = q.getC();
        this.change = q.getD();
        this.percentChange = q.getDp();
        this.high = q.getH();
        this.low = q.getL();
        this.open = q.getO();
        this.previousClose = q.getPc();
        this.time = q.getT();
    }
}
