package cs309.stocks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cs309.stocks.Portfolios.Portfolio;
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

import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SamuelSystemTest {
    private static String cookie;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @LocalServerPort
    private int port;
    private int portfolioId;
    private Portfolio testPortfolio;

    @AfterAll
    public static void tearDown() {
        // Delete testuser
        TestingUtils.request(cookie).queryParam("password", "password")
                .delete("/user/delete");

        // Login and delete testuser2
        String newCookie = TestingUtils.login("testuser2", "password");
        TestingUtils.request(newCookie).queryParam("password", "password")
                .delete("/user/delete");
    }

    @BeforeAll
    public void setUp() throws JSONException {
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

        // Create another standard user
        registration.put("username", "testuser2").put("role", "STANDARD").put("email", "email.email1@gmail.com");
        TestingUtils.registerUser(registration);

        cookie = TestingUtils.login("testuser", "password");
    }

    @Test
    @Order(1)
    public void testCreate() throws JsonProcessingException {
        testPortfolio = new Portfolio();
        testPortfolio.setName("Test Portfolio");
        testPortfolio.setCash(5000);

        Portfolio gotPortfolio = TestingUtils.createPortfolio(cookie, testPortfolio);
        assert gotPortfolio != null;
        assert gotPortfolio.getCash() == 5000;

        portfolioId = gotPortfolio.getId();
        testPortfolio.setId(portfolioId);
    }

    @Test
    @Order(2)
    public void testGet() {
        Response response = TestingUtils.request(cookie).queryParam("id", portfolioId)
                .get("/portfolio");
        assert response.getStatusCode() == 200;
        Portfolio gotPortfolio = response.as(Portfolio.class);
        assert gotPortfolio.getId() == portfolioId;
        assert gotPortfolio.getName().equals("Test Portfolio");
        assert gotPortfolio.getCash() == 5000;

        // Get nonexistent portfolio
        response = TestingUtils.request(cookie).queryParam("id", -1)
                .get("/portfolio");
        assert response.getStatusCode() == 404;
    }

    @Test
    @Order(3)
    public void testUpdate() {
        testPortfolio.setName("Test Portfolio Edited");
        testPortfolio.setCash(7000);
        Response response = TestingUtils.request(cookie).header("Content-Type", "application/json")
                .body(testPortfolio)
                .put("/portfolio");
        assert response.getStatusCode() == 200;

        // Get and compare
        response = TestingUtils.request(cookie).queryParam("id", portfolioId)
                .get("/portfolio");
        assert response.getStatusCode() == 200;

        Portfolio gotPortfolio = response.as(Portfolio.class);
        assert gotPortfolio.getName().equals("Test Portfolio Edited");
        assert gotPortfolio.getCash() == 7000;

        // Update nonexistent portfolio
        testPortfolio.setId(-1);
        response = TestingUtils.request(cookie).header("Content-Type", "application/json")
                .body(testPortfolio)
                .put("/portfolio");
        assert response.getStatusCode() == 404;
        testPortfolio.setId(portfolioId);
    }

    @Test
    @Order(4)
    public void testAllDefaultPortfolio() throws JsonProcessingException {
        Response response = TestingUtils.request(cookie).get("/portfolio/all");
        assert response.getStatusCode() == 200;
        List<Portfolio> portfolios = objectMapper.readValue(response.asString(), new TypeReference<List<Portfolio>>() {
        });
        assert portfolios.size() == 2;
        Portfolio defaultPortfolio = findByName(portfolios);
        assert defaultPortfolio != null;
        assert defaultPortfolio.getCash() == 10000;
    }

    @Test
    @Order(5)
    public void testPermissions() throws JsonProcessingException {
        // Login as the other user
        String newCookie = TestingUtils.login("testuser2", "password");

        // Create permissions
        Response response = TestingUtils.request(newCookie).body(objectMapper.writeValueAsString(testPortfolio))
                .header("Content-Type", "application/json")
                .post("/portfolio/create");
        assert response.getStatusCode() == 403;
        assert response.asString().equals("Standard accounts can only have one portfolio");

        // Read permissions
        response = TestingUtils.request(newCookie).queryParam("id", portfolioId)
                .get("/portfolio");
        assert response.getStatusCode() == 403;

        // Update permissions
        response = TestingUtils.request(newCookie).header("Content-Type", "application/json")
                .body(testPortfolio)
                .put("/portfolio");
        assert response.getStatusCode() == 403;

        // Delete permissions
        response = TestingUtils.request(newCookie).queryParam("id", portfolioId)
                .delete("/portfolio");
        assert response.getStatusCode() == 403;
    }

    @Test
    @Order(6)
    public void testDelete() {
        Response response = TestingUtils.request(cookie).queryParam("id", portfolioId)
                .delete("/portfolio");
        assert response.getStatusCode() == 200;

        // Try to get the deleted portfolio
        response = TestingUtils.request(cookie).queryParam("id", portfolioId)
                .get("/portfolio");
        assert response.getStatusCode() == 404;

        // Delete a nonexistent portfolio
        response = TestingUtils.request(cookie).queryParam("id", -1)
                .delete("/portfolio");
        assert response.getStatusCode() == 404;
    }

    private Portfolio findByName(List<Portfolio> portfolios) {
        for (Portfolio portfolio : portfolios) {
            if (portfolio.getName().equals("Default Portfolio")) {
                return portfolio;
            }
        }
        return null;
    }
}
