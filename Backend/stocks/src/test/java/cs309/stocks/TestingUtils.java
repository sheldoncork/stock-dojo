package cs309.stocks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cs309.stocks.Portfolios.Portfolio;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.json.JSONObject;

import java.util.List;

public class TestingUtils {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static RequestSpecification request() {
        return request(null);
    }

    public static RequestSpecification request(String cookie) {
        RequestSpecification request = RestAssured.given()
                .header("Host", "localhost");
        if (cookie != null) {
            request.header("Cookie", "JSESSIONID=" + cookie);
        }
        return request;
    }

    public static String login(String username, String password) {
        Response response = request().queryParam("username", username)
                .queryParam("password", password)
                .post("/auth/login");
        return response.getCookie("JSESSIONID");
    }

    public static Response registerUser(JSONObject user) {
        return request().body(user.toString())
                .header("Content-Type", "application/json")
                .post("/auth/register");
    }

    public static Portfolio createPortfolio(String cookie, Portfolio portfolio) throws JsonProcessingException {
        Response response = TestingUtils.request(cookie).body(mapper.writeValueAsString(portfolio))
                .header("Content-Type", "application/json")
                .post("/portfolio/create");
        assert response.getStatusCode() == 200;

        response = TestingUtils.request(cookie).get("/portfolio/all");
        List<Portfolio> portfolios = mapper.readValue(response.asString(), new TypeReference<List<Portfolio>>() {
        });
        return findByName(portfolios, portfolio.getName());
    }

    private static Portfolio findByName(List<Portfolio> portfolios, String name) {
        for (Portfolio portfolio : portfolios) {
            if (portfolio.getName().equals(name)) {
                return portfolio;
            }
        }
        return null;
    }
}
