package cs309.stocks;

import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;
import io.restassured.response.Response;
import org.json.JSONArray;
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
public class AnnouncementsTests {

    private static String teacherCookie;
    private static String studentCookie;
    private static int setupClassroomId;
    private static int setupAnnouncementId = 0;
    @LocalServerPort
    private int port;

    @BeforeAll
    public void setUp() throws JSONException {
        RestAssured.port = port;
        RestAssured.baseURI = "https://localhost";
        RestAssured.config = RestAssured.config().sslConfig(new SSLConfig().relaxedHTTPSValidation());

        // Register a teacher user
        JSONObject teacherRegistration = new JSONObject()
                .put("username", "teacherUser")
                .put("password", "password")
                .put("role", "TEACHER")
                .put("email", "teacher.email@example.com");
        Response teacherResponse = RestAssured.given().body(teacherRegistration.toString())
                .header("Content-Type", "application/json")
                .post("/auth/register");
        Assertions.assertEquals(200, teacherResponse.getStatusCode(), "Failed to register teacher");

        // Register a student user
        JSONObject studentRegistration = new JSONObject()
                .put("username", "studentUser")
                .put("password", "password")
                .put("role", "STUDENT")
                .put("email", "student.email@example.com");
        Response studentResponse = RestAssured.given().body(studentRegistration.toString())
                .header("Content-Type", "application/json")
                .post("/auth/register");
        Assertions.assertEquals(200, studentResponse.getStatusCode(), "Failed to register student");

        // Log in as the teacher
        Response teacherLoginResponse = RestAssured.given()
                .queryParam("username", "teacherUser")
                .queryParam("password", "password")
                .header("Content-Type", "application/json")
                .post("/auth/login");
        Assertions.assertEquals(200, teacherLoginResponse.getStatusCode(), "Failed to log in as teacher");
        teacherCookie = teacherLoginResponse.getCookie("JSESSIONID");

        // Log in as the student
        Response studentLoginResponse = RestAssured.given()
                .queryParam("username", "studentUser")
                .queryParam("password", "password")
                .header("Content-Type", "application/json")
                .post("/auth/login");
        Assertions.assertEquals(200, studentLoginResponse.getStatusCode(), "Failed to log in as student");
        studentCookie = studentLoginResponse.getCookie("JSESSIONID");

        // Teacher creates a classroom
        Response classroomResponse = RestAssured.given()
                .header("Cookie", "JSESSIONID=" + teacherCookie)
                .queryParam("name", "Test Classroom")
                .queryParam("startingAmount", 1000.0)
                .post("/classroom/create");
        Assertions.assertEquals(200, classroomResponse.getStatusCode(), "Failed to create classroom");

        JSONObject classroomData = new JSONObject(classroomResponse.getBody().asString());
        Assertions.assertTrue(classroomData.has("classId"), "Response does not contain 'classId'");
        setupClassroomId = classroomData.getInt("classId");
        String setupJoinCode = classroomData.getString("joinCode");

        // Add the student to the classroom
        Response addStudentResponse = RestAssured.given()
                .header("Cookie", "JSESSIONID=" + studentCookie)
                .queryParam("joinCode", setupJoinCode)
                .post("/classroom/join-classroom");
        Assertions.assertEquals(200, addStudentResponse.getStatusCode(), "Failed to add student to classroom");
    }

    @BeforeEach
    public void setPort() {
        RestAssured.port = port;
    }

    @Test
    @Order(1)
    public void testCreateAnnouncement() throws JSONException {
        // Create the announcement payload
        JSONObject announcement = new JSONObject();
        announcement.put("title", "First Test Announcement");
        announcement.put("content", "This is the content of the first test announcement.");

        // Send a POST request to create the announcement
        Response response = RestAssured.given()
                .header("Cookie", "JSESSIONID=" + teacherCookie) // Use teacher's session cookie
                .queryParam("classroomId", setupClassroomId) // Use the classroom ID from setup
                .header("Content-Type", "application/json")
                .body(announcement.toString()) // Send announcement payload
                .post("/announcements");

        // Log the response body for debugging
        System.out.println("Response Body: " + response.getBody().asString());

        // Assert the response status code
        Assertions.assertEquals(200, response.getStatusCode(), "Failed to create announcement");

        // Assert the response contains the expected message
        Assertions.assertTrue(response.getBody().asString().contains("Announcement created"),
                "Unexpected response message: " + response.getBody().asString());
    }

    @Test
    @Order(2)
    public void testFetchClassroomAnnouncements() throws JSONException {
        // Send a GET request to fetch classroom announcements
        Response response = RestAssured.given()
                .header("Cookie", "JSESSIONID=" + teacherCookie) // Use teacher's session cookie
                .queryParam("classroomId", setupClassroomId) // Use the classroom ID from setup
                .get("/announcements/classroom");

        // Log the response body for debugging
        String responseBody = response.getBody().asString();
        System.out.println("Response Body: " + responseBody);

        // Assert the response status code
        Assertions.assertEquals(200, response.getStatusCode(), "Failed to fetch classroom announcements");

        // Parse the response as a JSON array
        JSONArray announcements = new JSONArray(responseBody);

        // Assert that the array is not empty
        Assertions.assertTrue(announcements.length() > 0, "No announcements found");

        // Extract the first announcement's ID
        JSONObject firstAnnouncement = announcements.getJSONObject(0);
        setupAnnouncementId = firstAnnouncement.getInt("id");

        // Log the extracted announcement ID for debugging
        System.out.println("Saved Announcement ID: " + setupAnnouncementId);

        // Optional: Assert fields in the first announcement
        Assertions.assertTrue(firstAnnouncement.has("title"), "First announcement does not have a 'title'");
        Assertions.assertTrue(firstAnnouncement.has("content"), "First announcement does not have 'content'");
        Assertions.assertTrue(firstAnnouncement.has("postDate"), "First announcement does not have 'postDate'");
    }

    @Test
    @Order(3)
    public void testGetAnnouncementById() throws JSONException {
        // Assume an announcement ID was saved in the previous test

        // Send a GET request to fetch the announcement
        Response response = RestAssured.given()
                .header("Cookie", "JSESSIONID=" + teacherCookie) // Use teacher's session cookie
                .queryParam("announcementId", setupAnnouncementId) // Use the announcement ID
                .get("/announcements");

        // Log the response body for debugging
        String responseBody = response.getBody().asString();
        System.out.println("Response Body: " + responseBody);

        // Assert the response status code
        Assertions.assertEquals(200, response.getStatusCode(), "Failed to fetch announcement by ID");

        // Parse the response as a JSON object
        JSONObject announcement = new JSONObject(responseBody);

        // Assert that certain fields are present in the response
        Assertions.assertTrue(announcement.has("id"), "Response does not contain 'id'");
        Assertions.assertTrue(announcement.has("title"), "Response does not contain 'title'");
        Assertions.assertTrue(announcement.has("content"), "Response does not contain 'content'");
        Assertions.assertTrue(announcement.has("postDate"), "Response does not contain 'postDate'");

        // Optional: Log specific values for debugging
        System.out.println("Announcement Title: " + announcement.getString("title"));
        System.out.println("Announcement Content: " + announcement.getString("content"));
    }

    @Test
    @Order(4)
    public void testEditAnnouncement() throws JSONException {
        // Assume an announcement ID was saved in a previous test

        // Create the updated announcement payload
        JSONObject updatedAnnouncement = new JSONObject();
        updatedAnnouncement.put("id", setupAnnouncementId); // Include the ID of the announcement to edit
        updatedAnnouncement.put("title", "Updated Test Announcement");
        updatedAnnouncement.put("content", "This is the updated content of the announcement.");

        // Send a PUT request to edit the announcement
        Response response = RestAssured.given()
                .header("Cookie", "JSESSIONID=" + teacherCookie) // Use teacher's session cookie
                .header("Content-Type", "application/json")
                .body(updatedAnnouncement.toString()) // Send the updated announcement payload
                .put("/announcements");

        // Log the response body for debugging
        String responseBody = response.getBody().asString();
        System.out.println("Response Body: " + responseBody);

        // Assert the response status code
        Assertions.assertEquals(200, response.getStatusCode(), "Failed to edit announcement");

        // Assert the response contains the expected message
        Assertions.assertTrue(responseBody.contains("Announcement updated successfully"),
                "Unexpected response message: " + responseBody);
    }

    @Test
    @Order(5)
    public void testFetchAllAnnouncements() throws JSONException {
        // Send a GET request to fetch all announcements
        Response response = RestAssured.given()
                .header("Cookie", "JSESSIONID=" + teacherCookie) // Use teacher's session cookie
                .get("/announcements/all");

        // Log the response body for debugging
        String responseBody = response.getBody().asString();
        System.out.println("Response Body: " + responseBody);

        // Assert the response status code
        Assertions.assertEquals(200, response.getStatusCode(), "Failed to fetch all announcements");

        // Parse the response as a JSON array
        JSONArray announcements = new JSONArray(responseBody);

        // Assert that the array is not empty
        Assertions.assertTrue(announcements.length() > 0, "No announcements found");

        // Optional: Assert fields in the first announcement
        JSONObject firstAnnouncement = announcements.getJSONObject(0);
        Assertions.assertTrue(firstAnnouncement.has("id"), "Announcement does not have 'id'");
        Assertions.assertTrue(firstAnnouncement.has("title"), "Announcement does not have 'title'");
        Assertions.assertTrue(firstAnnouncement.has("content"), "Announcement does not have 'content'");
        Assertions.assertTrue(firstAnnouncement.has("postDate"), "Announcement does not have 'postDate'");

        // Log details of the first announcement for debugging
        System.out.println("First Announcement ID: " + firstAnnouncement.getInt("id"));
        System.out.println("First Announcement Title: " + firstAnnouncement.getString("title"));
    }

    @Test
    @Order(6)
    public void testDeleteAnnouncement() {
        // Send a DELETE request to delete the announcement
        Response response = RestAssured.given()
                .header("Cookie", "JSESSIONID=" + teacherCookie) // Use teacher's session cookie
                .queryParam("announcementId", setupAnnouncementId) // Use the saved announcement ID
                .delete("/announcements");

        // Log the response body for debugging
        String responseBody = response.getBody().asString();
        System.out.println("Response Body: " + responseBody);

        // Assert the response status code
        Assertions.assertEquals(200, response.getStatusCode(), "Failed to delete the announcement");

        // Assert the response message
        Assertions.assertTrue(responseBody.contains("Announcement deleted"),
                "Unexpected response message: " + responseBody);
    }


    @AfterAll
    public void tearDown() {
        // Remove the student from the classroom
        Response removeStudentResponse = RestAssured.given()
                .header("Cookie", "JSESSIONID=" + teacherCookie)
                .queryParam("studentName", "studentUser")
                .queryParam("classroomId", setupClassroomId)
                .post("/classroom/delete-student-by-teacher");
        Assertions.assertTrue(removeStudentResponse.getStatusCode() == 200 || removeStudentResponse.getStatusCode() == 404,
                "Failed to remove student from classroom");

        // Delete classroom
        if (setupClassroomId > 0) {
            Response deleteClassroomResponse = RestAssured.given()
                    .header("Cookie", "JSESSIONID=" + teacherCookie)
                    .queryParam("classroomId", setupClassroomId)
                    .delete("/classroom/delete");
            Assertions.assertTrue(deleteClassroomResponse.getStatusCode() == 200 || deleteClassroomResponse.getStatusCode() == 404,
                    "Failed to delete classroom");
        }

        // Delete teacher user
        if (teacherCookie != null) {
            Response deleteTeacherResponse = RestAssured.given()
                    .header("Cookie", "JSESSIONID=" + teacherCookie)
                    .queryParam("password", "password")
                    .delete("/user/delete");
            Assertions.assertTrue(deleteTeacherResponse.getStatusCode() == 200 || deleteTeacherResponse.getStatusCode() == 404,
                    "Failed to delete teacher user");
        }

        // Delete student user
        if (studentCookie != null) {
            Response deleteStudentResponse = RestAssured.given()
                    .header("Cookie", "JSESSIONID=" + studentCookie)
                    .queryParam("password", "password")
                    .delete("/user/delete");
            Assertions.assertTrue(deleteStudentResponse.getStatusCode() == 200 || deleteStudentResponse.getStatusCode() == 404,
                    "Failed to delete student user");
        }
    }
}
