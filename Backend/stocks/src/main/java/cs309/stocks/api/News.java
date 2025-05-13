package cs309.stocks.api;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class News {
    private String category;
    private long datetime;
    private String headline;
    private int id;
    private String image;
    private String related;
    private String source;
    private String summary;
    private String url;
}
