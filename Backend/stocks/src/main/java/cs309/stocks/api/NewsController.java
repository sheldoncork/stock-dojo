package cs309.stocks.api;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/news")
@Tag(name = "News API", description = "API for fetching market news from Finnhub.")
public class NewsController {

    private final FinnHub finnhub;

    @Autowired
    public NewsController() {
        this.finnhub = FinnHub.getInstance();
    }

    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    type = "array",
                                    description = "List of market news articles based on the specified category",
                                    example = "[" +
                                            "  {" +
                                            "    \"category\": \"general\"," +
                                            "    \"datetime\": 1596589501," +
                                            "    \"headline\": \"Square surges after reporting 64% jump in revenue, more customers using Cash App\"," +
                                            "    \"id\": 5085164," +
                                            "    \"image\": \"https://image.cnbcfm.com/api/v1/image/105569283-1542050972462rts25mct.jpg?v=1542051069\"," +
                                            "    \"related\": \"\"," +
                                            "    \"source\": \"CNBC\"," +
                                            "    \"summary\": \"Shares of Square soared on Tuesday evening after posting better-than-expected quarterly results and strong growth in its consumer payments app.\"," +
                                            "    \"url\": \"https://www.cnbc.com/2020/08/04/square-sq-earnings-q2-2020.html\"" +
                                            "  }," +
                                            "  {" +
                                            "    \"category\": \"business\"," +
                                            "    \"datetime\": 1596588232," +
                                            "    \"headline\": \"B&G Foods CEO expects pantry demand to hold up post-pandemic\"," +
                                            "    \"id\": 5085113," +
                                            "    \"image\": \"https://image.cnbcfm.com/api/v1/image/106629991-1595532157669-gettyimages-1221952946-362857076_1-5.jpeg?v=1595532242\"," +
                                            "    \"related\": \"\"," +
                                            "    \"source\": \"CNBC\"," +
                                            "    \"summary\": \"\\\"I think post-Covid, people will be working more at home, which means people will be eating more breakfast\\\" and other meals at home, B&G CEO Ken Romanzi said.\"," +
                                            "    \"url\": \"https://www.cnbc.com/2020/08/04/bg-foods-ceo-expects-pantry-demand-to-hold-up-post-pandemic.html\"" +
                                            "  }" +
                                            "]"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    content = @Content(
                            mediaType = "text/plain",
                            schema = @Schema(
                                    type = "string",
                                    description = "Error message if the request is invalid",
                                    example = "Invalid category"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    content = @Content(
                            mediaType = "text/plain",
                            schema = @Schema(
                                    type = "string",
                                    description = "Error message if there is an internal server error",
                                    example = "Internal server error"
                            )
                    )
            )
    })

    @GetMapping
    public List<News> getNewsByCategory(
            @RequestParam(required = false, defaultValue = "general") String category) {
        try {
            return finnhub.getNews(category);
        } catch (Exception e) {
            throw new RuntimeException("Error fetching news: " + e.getMessage(), e);
        }
    }

    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    type = "array",
                                    description = "List of company news articles for the past 5 days",
                                    example = "[" +
                                            "  {" +
                                            "    \"category\": \"company news\"," +
                                            "    \"datetime\": 1569550360," +
                                            "    \"headline\": \"More sops needed to boost electronic manufacturing: Top govt official\"," +
                                            "    \"id\": 25286," +
                                            "    \"image\": \"https://img.etimg.com/thumb/msid-71321314,width-1070,height-580,imgsize-481831,overlay-economictimes/photo.jpg\"," +
                                            "    \"related\": \"AAPL\"," +
                                            "    \"source\": \"The Economic Times India\"," +
                                            "    \"summary\": \"India may have to offer electronic manufacturers additional sops...\"," +
                                            "    \"url\": \"https://economictimes.indiatimes.com/industry/cons-products/electronics/more-sops-needed-to-boost-electronic-manufacturing-top-govt-official/articleshow/71321308.cms\"" +
                                            "  }," +
                                            "  {" +
                                            "    \"category\": \"company news\"," +
                                            "    \"datetime\": 1569528720," +
                                            "    \"headline\": \"How to disable comments on your YouTube videos in 2 different ways\"," +
                                            "    \"id\": 25287," +
                                            "    \"image\": \"https://amp.businessinsider.com/images/5d8d16182e22af6ab66c09e9-1536-768.jpg\"," +
                                            "    \"related\": \"AAPL\"," +
                                            "    \"source\": \"Business Insider\"," +
                                            "    \"summary\": \"You can disable comments on your own YouTube video if you don't want people to comment on it...\"," +
                                            "    \"url\": \"https://www.businessinsider.com/how-to-disable-comments-on-youtube\"" +
                                            "  }" +
                                            "]"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    content = @Content(
                            mediaType = "text/plain",
                            schema = @Schema(
                                    type = "string",
                                    description = "Error message if the symbol is invalid or missing",
                                    example = "Invalid symbol"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    content = @Content(
                            mediaType = "text/plain",
                            schema = @Schema(
                                    type = "string",
                                    description = "Error message if there is an internal server error",
                                    example = "Internal server error"
                            )
                    )
            )
    })

    @GetMapping("/company")
    public List<News> getCompanyNews(@RequestParam String symbol) {
        try {
            // Get current date and calculate date range
            LocalDate currentDate = LocalDate.now();
            LocalDate startDate = currentDate.minusDays(5);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String from = startDate.format(formatter);
            String to = currentDate.format(formatter);

            return finnhub.getCompanyNews(symbol, from, to);
        } catch (Exception e) {
            throw new RuntimeException("Error fetching company news: " + e.getMessage(), e);
        }
    }

}
