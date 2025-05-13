package cs309.stocks;

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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthTests {
    private static String cookie;
    @LocalServerPort
    private int port;

    @AfterAll
    public static void tearDown() {
        cookie = TestingUtils.login("testuser", "password");
        TestingUtils.request(cookie).queryParam("password", "password")
                .delete("/user/delete");
    }

    @BeforeEach
    public void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "https://localhost";
        RestAssured.config = RestAssured.config().sslConfig(new SSLConfig().relaxedHTTPSValidation());
    }

    @Test
    @Order(1)
    public void testIncompleteRegistration() throws JSONException {
        JSONObject registration = new JSONObject();
        registration.put("username", "testuser");
        Response response = TestingUtils.registerUser(registration);
        assert response.getStatusCode() == 400;
        assert response.getBody().asString().equals("Password is required");

        registration.put("password", "password");
        response = TestingUtils.registerUser(registration);
        assert response.getStatusCode() == 400;
        assert response.getBody().asString().equals("Role is required");

        registration.put("role", "STUDENT");
        response = TestingUtils.registerUser(registration);
        assert response.getStatusCode() == 400;
        assert response.getBody().asString().equals("Email is required");

        registration.put("email", "test@test.com");
        response = TestingUtils.registerUser(registration);
        assert response.getStatusCode() == 200;
        assert response.getBody().asString().equals("User registered successfully");
    }

    @Test
    @Order(2)
    public void testDuplicateRegistration() throws JSONException {
        JSONObject registration = new JSONObject();
        registration.put("username", "testuser");
        registration.put("password", "password");
        registration.put("role", "STUDENT");
        registration.put("email", "test@test.com");
        Response response = TestingUtils.registerUser(registration);
        assert response.getStatusCode() == 200;
        assert response.getBody().asString().equals("Username is already in use");
    }

    @Test
    @Order(3)
    public void testEmptyLogin() {
        Response response = TestingUtils.request()
                .header("Content-Type", "application/json")
                .post("/auth/login");
        assert response.getStatusCode() == 401;
    }

    @Test
    @Order(4)
    public void testInvalidLogin() {
        Response response = TestingUtils.request().queryParam("username", "nosuchuser")
                .queryParam("password", "password")
                .post("/auth/login");
        assert response.getStatusCode() == 401;

        response = TestingUtils.request().queryParam("username", "testuser")
                .queryParam("password", "incorrectpassword")
                .post("/auth/login");
        assert response.getStatusCode() == 401;
    }

    @Test
    @Order(5)
    public void testValidLogin() {
        Response response = TestingUtils.request().queryParam("username", "testuser")
                .queryParam("password", "password")
                .post("/auth/login");
        cookie = response.getCookie("JSESSIONID");
        assert response.getStatusCode() == 200;
//        assert response.getBody().asString().equals("STUDENT");
    }

    @Test
    @Order(6)
    public void testLogout() {
        Response response = TestingUtils.request(cookie).get("/auth/logout");
        assert response.getStatusCode() == 200;
        response = TestingUtils.request(cookie).get("/portfolio/all");
        assert response.getStatusCode() == 401;
    }

    @Test
    @Order(7)
    public void testForgotPassword() {
        Response response = TestingUtils.request().queryParam("email", "not-here-test@test.com")
                .get("/auth/forgot-password");
        assert response.getStatusCode() == 404;
        assert response.getBody().asString().equals("User not found");

        response = TestingUtils.request().queryParam("email", "test@test.com")
                .get("/auth/forgot-password");
        assert response.getStatusCode() == 200;
        assert response.getBody().asString().equals("Email sent");

        response = TestingUtils.request().queryParam("token", "nonexistenttoken")
                .queryParam("newPassword", "password")
                .post("/auth/forgot-password");
        assert response.getStatusCode() == 404;
        assert response.getBody().asString().equals("Token not found");

    }
}
