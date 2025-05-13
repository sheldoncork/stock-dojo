package cs309.stocks.api;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class SymbolSearch {
    private int count;
    private List<Symbol> result;
}
