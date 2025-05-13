package cs309.stocks.Stocks;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class StockInformationDTO {
    private Number currentPrice;
    private Number yearHigh;
    private Number yearLow;
    private Number beta;
    private Number dividendYield;
    private Number epsAnnual;
    private Number marketCap;
    private Number peRatio;

    public StockInformationDTO() {
    }
}