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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserTests {

    private static String cookie;
    @LocalServerPort
    private int port;

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

        cookie = TestingUtils.login("testuser", "password");
    }

    @Test
    @Order(1)
    public void testChangePassword() {
        Response response = TestingUtils.request(cookie).queryParam("oldPassword", "incorrectpassword")
                .queryParam("newPassword", "password123")
                .put("/user/change-password");
        assert response.getStatusCode() == 403;
        assert response.asString().equals("Old password is incorrect");

        response = TestingUtils.request(cookie).queryParam("oldPassword", "password")
                .queryParam("newPassword", "password123")
                .put("/user/change-password");
        assert response.getStatusCode() == 200;
        assert response.asString().equals("Password changed successfully");

        TestingUtils.request(cookie).post("/auth/logout");
        cookie = "";
        cookie = TestingUtils.login("testuser", "password123");
        assert !cookie.isEmpty();
    }

    @Test
    @Order(2)
    public void testChangeUsername() {
        Response response = TestingUtils.request(cookie).queryParam("newUsername", "testuser1")
                .queryParam("password", "incorrectpassword")
                .put("/user/changeUsername");
        assert response.getStatusCode() == 403;
        assert response.asString().equals("Password is incorrect");

        response = TestingUtils.request(cookie).queryParam("newUsername", "testuser")
                .queryParam("password", "password123")
                .put("/user/changeUsername");
        assert response.getStatusCode() == 409;
        assert response.asString().equals("Username is already taken");

        response = TestingUtils.request(cookie).queryParam("newUsername", "testuser1")
                .queryParam("password", "password123")
                .put("/user/changeUsername");
        assert response.getStatusCode() == 200;
        assert response.asString().equals("Username changed successfully");

        TestingUtils.request(cookie).post("/auth/logout");
        cookie = "";
        cookie = TestingUtils.login("testuser1", "password123");
        assert cookie != null;
    }

    @Test
    @Order(3)
    public void testFailedDelete() {
        Response response = TestingUtils.request(cookie).queryParam("password", "incorrectpassword")
                .delete("/user/delete");
        assert response.getStatusCode() == 403;
        assert response.getBody().asString().equals("Password is incorrect");
    }

    @AfterAll
    public void tearDown() {
        Response response = TestingUtils.request(cookie).queryParam("password", "password123")
                .delete("/user/delete");
        System.out.println(response.getBody().asString());
    }
}
