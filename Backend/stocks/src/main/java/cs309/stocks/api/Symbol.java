package cs309.stocks.api;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Symbol {
    private String description;
    private String displaySymbol;
    private String symbol;
    private String type;
}
