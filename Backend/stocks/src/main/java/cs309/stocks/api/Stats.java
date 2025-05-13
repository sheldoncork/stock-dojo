package cs309.stocks.api;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Stats {
    private Number yearHigh;
    private Number yearLow;
    private Number beta;
    private Number dividendYield;
    private Number epsAnnual;
    private Number marketCap;
    private Number peRatio;

    public Stats(AllStats allStats) {
        yearHigh = (Number) allStats.getMetric().get("52WeekHigh");
        yearLow = (Number) allStats.getMetric().get("52WeekLow");
        beta = (Number) allStats.getMetric().get("beta");
        dividendYield = (Number) allStats.getMetric().get("currentDividendYieldTTM");
        epsAnnual = (Number) allStats.getMetric().get("epsAnnual");
        marketCap = (Number) allStats.getMetric().get("marketCapitalization");
        peRatio = (Number) allStats.getMetric().get("peTTM");
    }
}
