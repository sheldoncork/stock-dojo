package cs309.stocks.api;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RawQuote {
    private double c;
    private double d;
    private double dp;
    private double h;
    private double l;
    private double o;
    private double pc;
    private int t;
}
