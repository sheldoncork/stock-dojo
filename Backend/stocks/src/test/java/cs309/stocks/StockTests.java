package cs309.stocks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cs309.stocks.Portfolios.Portfolio;
import cs309.stocks.Stocks.Stock;
import cs309.stocks.Stocks.StockInformationDTO;
import cs309.stocks.Transactions.Transaction;
import cs309.stocks.api.DayStats;
import cs309.stocks.api.Recommendation;
import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;
import io.restassured.response.Response;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith({SpringExtension.class})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StockTests {
    private final ObjectMapper objectMapper = new ObjectMapper();
    @LocalServerPort
    private int port;
    private String cookie;
    private String otherCookie;
    private Portfolio testPortfolio;
    private List<Transaction> transactions;
    private int transactionIndex = 0;

    @BeforeAll
    public void setUp() throws JSONException, JsonProcessingException {
        RestAssured.port = port;
        RestAssured.baseURI = "https://localhost";
        RestAssured.config = RestAssured.config().sslConfig(new SSLConfig().relaxedHTTPSValidation());

        // Register a test user
        JSONObject registration = new JSONObject()
                .put("username", "testuser")
                .put("password", "password")
                .put("role", "STUDENT")
                .put("email", "email.email@gmail.com");
        TestingUtils.registerUser(registration);
        TestingUtils.registerUser(registration.put("username", "testuser2").put("email", "email1.email@gmail.com"));

        cookie = TestingUtils.login("testuser", "password");

        testPortfolio = new Portfolio();
        testPortfolio.setName("Test Portfolio");
        testPortfolio.setCash(5000);

        Portfolio gotPortfolio = TestingUtils.createPortfolio(cookie, testPortfolio);
        assert gotPortfolio != null;
        testPortfolio = gotPortfolio;

        transactions = new ArrayList<>();
        transactions.add(new Transaction("AAPL", 5));
        transactions.add(new Transaction("TSLA", 5));
        transactions.add(new Transaction("AAPL", 5));
        transactions.add(new Transaction("TSLA", -5));
        transactions.add(new Transaction("AAPL", -5));
    }

    @Test
    @Order(1)
    public void testBuy() throws JsonProcessingException {
        Transaction t = transactions.get(transactionIndex);

        Response response = TestingUtils.request(cookie).queryParam("portfolioId", -1)
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(t))
                .post("/stock/buy");
        assert response.getStatusCode() == 404;
        assert response.getBody().asString().equals("Portfolio not found");

        response = TestingUtils.request(cookie).queryParam("portfolioId", testPortfolio.getId())
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(t))
                .post("/stock/buy");

        assert response.getStatusCode() == 200;
        assert response.getBody().asString().equals("Purchase successful");

        response = TestingUtils.request(cookie).queryParam("id", testPortfolio.getId())
                .get("/portfolio");
        assert response.getStatusCode() == 200;

        Portfolio gotPortfolio = response.as(Portfolio.class);
        assert checkBuyPortfolio(gotPortfolio, t);
        testPortfolio = gotPortfolio;
        transactionIndex++;
    }

    @Test
    @Order(2)
    public void testBuyOther() throws JsonProcessingException {
        Transaction t = transactions.get(transactionIndex);

        Response response = TestingUtils.request(cookie).queryParam("portfolioId", testPortfolio.getId())
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(t))
                .post("/stock/buy");

        assert response.getStatusCode() == 200;
        assert response.getBody().asString().equals("Purchase successful");

        response = TestingUtils.request(cookie).queryParam("id", testPortfolio.getId())
                .get("/portfolio");
        assert response.getStatusCode() == 200;

        Portfolio gotPortfolio = response.as(Portfolio.class);
        assert checkBuyPortfolio(gotPortfolio, t);
        testPortfolio = gotPortfolio;
        transactionIndex++;
    }

    @Test
    @Order(3)
    public void testBuyExisting() throws JsonProcessingException {
        Transaction t = transactions.get(transactionIndex);

        Response response = TestingUtils.request(cookie).queryParam("portfolioId", testPortfolio.getId())
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(t))
                .post("/stock/buy");

        assert response.getStatusCode() == 200;
        assert response.getBody().asString().equals("Purchase successful");

        response = TestingUtils.request(cookie).queryParam("id", testPortfolio.getId())
                .get("/portfolio");
        assert response.getStatusCode() == 200;

        Portfolio gotPortfolio = response.as(Portfolio.class);
        assert checkBuyPortfolio(gotPortfolio, t);
        testPortfolio = gotPortfolio;
        transactionIndex++;
    }

    @Test
    @Order(4)
    public void testBuyISF() throws JsonProcessingException {
        Transaction t = new Transaction("AAPL", 50000);

        Response response = TestingUtils.request(cookie).queryParam("portfolioId", testPortfolio.getId())
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(t))
                .post("/stock/buy");
        assert response.getStatusCode() == 403;
        assert response.getBody().asString().equals("Insufficient funds");
    }

    @Test
    @Order(5)
    public void testSell() throws JsonProcessingException {
        Transaction t = new Transaction("TSLA", 5);

        Response response = TestingUtils.request(cookie).queryParam("portfolioId", -1)
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(t))
                .post("/stock/sell");
        assert response.getStatusCode() == 404;
        assert response.getBody().asString().equals("Portfolio not found");

        response = TestingUtils.request(cookie).queryParam("portfolioId", testPortfolio.getId())
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(t))
                .post("/stock/sell");

        assert response.getStatusCode() == 200;
        assert response.getBody().asString().equals("Sale successful");

        response = TestingUtils.request(cookie).queryParam("id", testPortfolio.getId())
                .get("/portfolio");
        assert response.getStatusCode() == 200;

        Portfolio gotPortfolio = response.as(Portfolio.class);
        assert checkSellPortfolio(gotPortfolio, t);
        testPortfolio = gotPortfolio;
    }

    @Test
    @Order(6)
    public void testSellExisting() throws JsonProcessingException {
        Transaction t = new Transaction("AAPL", 5);

        Response response = TestingUtils.request(cookie).queryParam("portfolioId", testPortfolio.getId())
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(t))
                .post("/stock/sell");

        assert response.getStatusCode() == 200;
        assert response.getBody().asString().equals("Sale successful");

        response = TestingUtils.request(cookie).queryParam("id", testPortfolio.getId())
                .get("/portfolio");
        assert response.getStatusCode() == 200;

        Portfolio gotPortfolio = response.as(Portfolio.class);
        assert checkSellPortfolio(gotPortfolio, t);
        testPortfolio = gotPortfolio;
    }

    @Test
    @Order(7)
    public void testSellISF() throws JsonProcessingException {
        Transaction t = new Transaction("TSLA", 10);

        Response response = TestingUtils.request(cookie).queryParam("portfolioId", testPortfolio.getId())
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(t))
                .post("/stock/sell");

        assert response.getStatusCode() == 404;
        assert response.getBody().asString().equals("Stock not owned");

        t.setTicker("AAPL");

        response = TestingUtils.request(cookie).queryParam("portfolioId", testPortfolio.getId())
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(t))
                .post("/stock/sell");

        assert response.getStatusCode() == 403;
        assert response.getBody().asString().equals("Insufficient shares");
    }

    @Test
    @Order(8)
    public void testPermissions() throws JsonProcessingException {
        otherCookie = TestingUtils.login("testuser2", "password");
        Response response = TestingUtils.request(otherCookie).get("/portfolio/all");
        assert response.getStatusCode() == 200;
        List<Portfolio> portfolios = objectMapper.readValue(response.asString(), new TypeReference<List<Portfolio>>() {
        });
        Portfolio defaultPortfolio = findPortfolioByName(portfolios);

        Transaction t = new Transaction("TSLA", 5);

        assert defaultPortfolio != null;
        response = TestingUtils.request(cookie).queryParam("portfolioId", defaultPortfolio.getId())
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(t))
                .post("/stock/buy");
        assert response.getStatusCode() == 403;
        System.out.println(response.getBody().asString());
        assert response.getBody().asString().equals("Cannot access this portfolio");

        response = TestingUtils.request(cookie).queryParam("portfolioId", defaultPortfolio.getId())
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(t))
                .post("/stock/sell");
        assert response.getStatusCode() == 403;
        assert response.getBody().asString().equals("Cannot access this portfolio");

        response = TestingUtils.request(cookie).queryParam("portfolioId", -1)
                .get("/transaction");
        assert response.getStatusCode() == 404;
        System.out.println(response.getBody().asString());
        assert response.getBody().asString().isEmpty();

        response = TestingUtils.request(cookie).queryParam("portfolioId", defaultPortfolio.getId())
                .get("/transaction");
        assert response.getStatusCode() == 403;
        assert response.getBody().asString().isEmpty();
    }

    @Test
    @Order(9)
    public void testAllTransactions() throws JsonProcessingException {
        Response response = TestingUtils.request(cookie).get("/transaction/all");
        assert response.getStatusCode() == 200;
        List<Transaction> gotTransactions = objectMapper.readValue(response.asString(), new TypeReference<List<Transaction>>() {
        });
        for (int i = 0; i < gotTransactions.size(); i++) {
            Transaction t = gotTransactions.get(i);
            Transaction expected = transactions.get(i);
            assert t.getShares() == expected.getShares();
            assert t.getTicker().equals(expected.getTicker());
        }
    }

    @Test
    @Order(10)
    public void testPortfolioTransactions() throws JsonProcessingException {
        Portfolio otherPortfolio = new Portfolio();
        otherPortfolio.setName("Other Portfolio");
        otherPortfolio.setCash(10000);
        otherPortfolio = TestingUtils.createPortfolio(cookie, otherPortfolio);

        Transaction t = new Transaction("TSLA", 5);
        Response response = TestingUtils.request(cookie).queryParam("portfolioId", otherPortfolio.getId())
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(t))
                .post("/stock/buy");
        assert response.getStatusCode() == 200;

        response = TestingUtils.request(cookie).queryParam("portfolioId", otherPortfolio.getId())
                .get("/transaction");
        assert response.getStatusCode() == 200;
        Transaction gotTransaction = objectMapper.readValue(response.asString(), new TypeReference<List<Transaction>>() {
        }).get(0);
        assert gotTransaction.getShares() == t.getShares();
        assert gotTransaction.getTicker().equals(t.getTicker());
    }

    @Test
    public void testHistorical() {
        Response response = TestingUtils.request(cookie).queryParam("symbol", "AAPL")
                .get("/stock/historical");
        assert response.getStatusCode() == 200;
        try {
            objectMapper.readValue(response.asString(), new TypeReference<List<DayStats>>() {
            });
            assert true;
        } catch (JsonProcessingException e) {
            assert false;
        }
    }

    @Test
    public void testRecommendation() {
        Response response = TestingUtils.request(cookie).queryParam("symbol", "AAPL")
                .get("/stock/recommendations");
        assert response.getStatusCode() == 200;
        try {
            objectMapper.readValue(response.asString(), new TypeReference<List<Recommendation>>() {
            });
            assert true;
        } catch (JsonProcessingException e) {
            assert false;
        }
    }

    @Test
    public void testSearch() {
        Response response = TestingUtils.request(cookie).queryParam("query", "AAPL").get("/stock/search");
        assert response.getStatusCode() == 200;
        assert response.getBody().jsonPath().getString("[0].symble").equals("AAPL");

        response = TestingUtils.request(cookie).queryParam("query", "Apple").get("/stock/search");
        assert response.getStatusCode() == 200;
        assert response.getBody().jsonPath().getString("[0].symble").equals("AAPL");
        assert response.getBody().as(List.class).size() > 1;
    }

    @Test
    public void testInfo() {
        Response response = TestingUtils.request(cookie).queryParam("symbol", "AAPL").get("/stock/info");
        assert response.getStatusCode() == 200;
        System.out.println(response.asString());
        try {
            objectMapper.readValue(response.asString(), StockInformationDTO.class);
            assert true;
        } catch (JsonProcessingException e) {
            assert false;
        }
    }

    @AfterAll
    public void tearDown() {
        TestingUtils.request(cookie).queryParam("password", "password")
                .delete("/user/delete");
        TestingUtils.request(otherCookie).queryParam("password", "password")
                .delete("/user/delete");
    }

    /*
    Verifies transaction was applied to the portfolio
     */
    private boolean checkBuyPortfolio(Portfolio p, Transaction t) {
        boolean cashUsed = p.getCash() < testPortfolio.getCash();
        boolean stockListChanged;
        if (p.getStocks().size() == testPortfolio.getStocks().size() + 1) {
            Stock s = findByName(p.getStocks(), t.getTicker());
            stockListChanged = s != null && s.getShares() == t.getShares();
        } else if (p.getStocks().size() == testPortfolio.getStocks().size()) {
            Stock s1 = findByName(p.getStocks(), t.getTicker());
            Stock s2 = findByName(testPortfolio.getStocks(), t.getTicker());
            stockListChanged = s1 != null && s2 != null && s1.getShares() == s2.getShares() + t.getShares();
        } else {
            stockListChanged = false;
        }
        return cashUsed && stockListChanged;
    }

    private boolean checkSellPortfolio(Portfolio p, Transaction t) {
        boolean cashUsed = p.getCash() > testPortfolio.getCash();
        boolean stockListChanged;
        if (p.getStocks().size() == testPortfolio.getStocks().size() - 1) {
            Stock s = findByName(p.getStocks(), t.getTicker());
            stockListChanged = s == null;
        } else if (p.getStocks().size() == testPortfolio.getStocks().size()) {
            Stock s1 = findByName(p.getStocks(), t.getTicker());
            Stock s2 = findByName(testPortfolio.getStocks(), t.getTicker());
            stockListChanged = s1 != null && s2 != null && s1.getShares() == s2.getShares() - t.getShares();
        } else {
            stockListChanged = false;
        }
        return cashUsed && stockListChanged;
    }

    private Stock findByName(List<Stock> stocks, String ticker) {
        for (Stock stock : stocks) {
            if (stock.getTicker().equals(ticker)) {
                return stock;
            }
        }
        return null;
    }

    private Portfolio findPortfolioByName(List<Portfolio> portfolios) {
        for (Portfolio portfolio : portfolios) {
            if (portfolio.getName().equals("Default Portfolio")) {
                return portfolio;
            }
        }
        return null;
    }

}
