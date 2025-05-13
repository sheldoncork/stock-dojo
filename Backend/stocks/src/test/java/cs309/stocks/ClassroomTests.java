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
public class ClassroomTests {

    private static String teacherCookie;
    private static String studentCookie;
    private static int setupClassroomId;
    private static String setupJoinCode;
    private static String setupClassroomName;
    private static int additionalClassroomId;
    private static String additionalClassroomName;
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

        // Teacher creates an initial classroom
        Response classroomResponse = RestAssured.given()
                .header("Cookie", "JSESSIONID=" + teacherCookie)
                .queryParam("name", "Initial Classroom")
                .queryParam("startingAmount", 1000.0)
                .post("/classroom/create");
        Assertions.assertEquals(200, classroomResponse.getStatusCode(), "Failed to create initial classroom");

        JSONObject classroomData = new JSONObject(classroomResponse.getBody().asString());
        Assertions.assertTrue(classroomData.has("classId"), "Response does not contain 'classId'");
        setupClassroomId = classroomData.getInt("classId");
        setupJoinCode = classroomData.getString("joinCode");
        setupClassroomName = "Initial Classroom"; // Use the known name
    }

    @BeforeEach
    public void setPort() {
        RestAssured.port = port;
    }

    @Test
    @Order(1)
    public void testCreateAdditionalClassroom() throws JSONException {
        // Create a new classroom
        String classroomName = "Additional Classroom";
        Response response = RestAssured.given()
                .header("Cookie", "JSESSIONID=" + teacherCookie)
                .queryParam("name", classroomName)
                .queryParam("startingAmount", 2000.0)
                .post("/classroom/create");

        // Log the response for debugging
        System.out.println("Response Body: " + response.getBody().asString());

        // Assert the classroom creation was successful
        Assertions.assertEquals(200, response.getStatusCode(), "Failed to create additional classroom");

        JSONObject responseBody = new JSONObject(response.getBody().asString());
        Assertions.assertTrue(responseBody.has("classId"), "Response does not contain 'classId'");
        Assertions.assertTrue(responseBody.has("joinCode"), "Response does not contain 'joinCode'");

        // Store the classroom information for later use
        additionalClassroomId = responseBody.getInt("classId");
        String additionalClassroomJoinCode = responseBody.getString("joinCode");
        additionalClassroomName = classroomName;

        // Log the extracted data for debugging
        System.out.println("Created additional classroom with ID: " + additionalClassroomId);
        System.out.println("Join Code: " + additionalClassroomJoinCode);
        System.out.println("Classroom Name: " + additionalClassroomName);
    }

    @Test
    @Order(2)
    public void testChangeClassroomName() {
        // Change the classroom name
        String newClassroomName = "Updated Classroom Name";
        Response response = RestAssured.given()
                .header("Cookie", "JSESSIONID=" + teacherCookie)
                .queryParam("classroomId", additionalClassroomId)
                .queryParam("newName", newClassroomName)
                .put("/classroom/change-name");

        // Log the response for debugging
        System.out.println("Response Body: " + response.getBody().asString());

        // Assert the name change was successful
        Assertions.assertEquals(200, response.getStatusCode(), "Failed to change classroom name");
        Assertions.assertTrue(response.getBody().asString().contains("Classroom name updated successfully"),
                "Unexpected response message: " + response.getBody().asString());

        // Update the stored classroom name
        additionalClassroomName = newClassroomName;

        // Log the updated classroom name for debugging
        System.out.println("Updated classroom name to: " + additionalClassroomName);
    }

    @Test
    @Order(3)
    public void testDeleteClassroom() {
        // Delete the additional classroom created in Test 1
        Response response = RestAssured.given()
                .header("Cookie", "JSESSIONID=" + teacherCookie)
                .queryParam("classroomId", additionalClassroomId)
                .delete("/classroom/delete");

        // Log the response for debugging
        System.out.println("Response Body: " + response.getBody().asString());

        // Assert the deletion was successful
        Assertions.assertEquals(200, response.getStatusCode(), "Failed to delete classroom");
        Assertions.assertTrue(response.getBody().asString().contains("Classroom deleted successfully"),
                "Unexpected response message: " + response.getBody().asString());

        // Invalidate the classroom ID to avoid duplicate deletions during teardown
        additionalClassroomId = -1;
    }

    @Test
    @Order(4)
    public void testAddStudentToClassroom() {
        // Send a POST request to add the student to the classroom created in setup
        Response response = RestAssured.given()
                .header("Cookie", "JSESSIONID=" + teacherCookie)
                .queryParam("joinCode", setupJoinCode) // Use the joinCode from setup
                .queryParam("studentName", "studentUser") // The student created in setup
                .post("/classroom/add-student");

        // Log the response body for debugging
        System.out.println("Response Body: " + response.getBody().asString());

        // Assert the response status code
        Assertions.assertEquals(200, response.getStatusCode(), "Failed to add student to classroom");

        // Assert the response message
        Assertions.assertTrue(response.getBody().asString().contains("Student added successfully"),
                "Unexpected response message: " + response.getBody().asString());
    }

    @Test
    @Order(5)
    public void testDeleteStudentByTeacher() {
        // Send a POST request to delete the student by the teacher
        Response response = RestAssured.given()
                .header("Cookie", "JSESSIONID=" + teacherCookie) // Use teacher's cookie
                .queryParam("studentName", "studentUser")       // The student to be removed
                .queryParam("classroomId", setupClassroomId)     // The classroom ID
                .post("/classroom/delete-student-by-teacher");

        // Log the response body for debugging
        System.out.println("Response Body: " + response.getBody().asString());

        // Assert the response status code
        Assertions.assertEquals(200, response.getStatusCode(), "Failed to delete student from classroom");

        // Assert the response message
        Assertions.assertTrue(response.getBody().asString().contains("Student removed from classroom"),
                "Unexpected response message: " + response.getBody().asString());
    }

    @Test
    @Order(6)
    public void testStudentJoinClassroom() {
        // Send a POST request for the student to join the classroom using the setup join code
        Response response = RestAssured.given()
                .header("Cookie", "JSESSIONID=" + studentCookie) // Use the student's cookie
                .queryParam("joinCode", setupJoinCode)          // Use the join code from setup
                .post("/classroom/join-classroom");

        // Log the response body for debugging
        System.out.println("Response Body: " + response.getBody().asString());

        // Assert the response status code
        Assertions.assertEquals(200, response.getStatusCode(), "Failed to join classroom");

        // Assert the response message
        Assertions.assertTrue(response.getBody().asString().contains("Student added to classroom"),
                "Unexpected response message: " + response.getBody().asString());
    }

    @Test
    @Order(7)
    public void testGetStudentClasses() {
        // Send a GET request to retrieve the list of classes for the student
        Response response = RestAssured.given()
                .header("Cookie", "JSESSIONID=" + studentCookie) // Use the student's cookie
                .get("/classroom/classes");

        // Log the response body for debugging
        System.out.println("Response Body: " + response.getBody().asString());

        // Assert the response status code
        Assertions.assertEquals(200, response.getStatusCode(), "Failed to retrieve classes");

        // Parse the response and verify it contains the classroom from setup
        Assertions.assertTrue(response.getBody().asString().contains(setupClassroomName),
                "Response does not contain the expected classroom name: " + setupClassroomName);
        Assertions.assertTrue(response.getBody().asString().contains(String.valueOf(setupClassroomId)),
                "Response does not contain the expected classroom ID: " + setupClassroomId);
    }

    @Test
    @Order(8)
    public void testClassInfo() {
        // Send a GET request to fetch class info
        Response response = RestAssured.given()
                .header("Cookie", "JSESSIONID=" + studentCookie) // Use the student's cookie
                .queryParam("classId", setupClassroomId) // Use the classroom ID from setup
                .get("/classroom/classInfo");

        // Log the response body for debugging
        String responseBody = response.getBody().asString();
        System.out.println("Response Body: " + responseBody);

        // Assert the response status code
        Assertions.assertEquals(200, response.getStatusCode(), "Failed to fetch class info");

        try {
            // Attempt to parse the response as JSON
            JSONObject jsonResponse = new JSONObject(responseBody);

            // Assert that the response contains `hasNewAnnouncements`
            Assertions.assertTrue(jsonResponse.has("hasNewAnnouncements"),
                    "Response does not contain 'hasNewAnnouncements'");
        } catch (JSONException e) {
            // If parsing fails, throw a meaningful error
            Assertions.fail("Response is not valid JSON: " + responseBody);
        }
    }


    @Test
    @Order(9)
    public void testChatHistory() {
        // Send a GET request to fetch chat history
        Response response = RestAssured.given()
                .header("Cookie", "JSESSIONID=" + studentCookie) // Use the student's cookie
                .queryParam("classId", setupClassroomId) // Use the classroom ID from setup
                .get("/classroom/chatHistory");

        // Log the response body for debugging
        System.out.println("Response Body: " + response.getBody().asString());

        // Assert the response status code
        Assertions.assertEquals(200, response.getStatusCode(), "Failed to fetch chat history");
    }


    @Test
    @Order(10)
    public void testLeaveClassroom() {
        // Send a POST request to leave the classroom
        Response response = RestAssured.given()
                .header("Cookie", "JSESSIONID=" + studentCookie) // Use the student's cookie
                .queryParam("classroomId", setupClassroomId) // Pass the classroom ID from setup
                .post("/classroom/leave-classroom");

        // Log the response body for debugging
        System.out.println("Response Body: " + response.getBody().asString());

        // Assert the response status code
        Assertions.assertEquals(200, response.getStatusCode(), "Failed to leave classroom");

        // Assert the response message contains confirmation
        Assertions.assertTrue(response.getBody().asString().contains("Student left the classroom"),
                "Unexpected response message: " + response.getBody().asString());
    }

    @AfterAll
    public void tearDown() {
        // Delete additional classroom created in Test 1
        if (additionalClassroomId > 0) {
            Response deleteClassroomResponse = RestAssured.given()
                    .header("Cookie", "JSESSIONID=" + teacherCookie)
                    .queryParam("classroomId", additionalClassroomId)
                    .delete("/classroom/delete");
            Assertions.assertTrue(deleteClassroomResponse.getStatusCode() == 200 || deleteClassroomResponse.getStatusCode() == 404,
                    "Failed to delete additional classroom");
        }

        // Delete initial classroom created in setUp
        if (setupClassroomId > 0) {
            Response deleteClassroomResponse = RestAssured.given()
                    .header("Cookie", "JSESSIONID=" + teacherCookie)
                    .queryParam("classroomId", setupClassroomId)
                    .delete("/classroom/delete");
            Assertions.assertTrue(deleteClassroomResponse.getStatusCode() == 200 || deleteClassroomResponse.getStatusCode() == 404,
                    "Failed to delete initial classroom");
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
