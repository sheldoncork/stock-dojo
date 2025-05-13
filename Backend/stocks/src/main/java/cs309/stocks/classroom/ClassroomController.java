package cs309.stocks.classroom;

import cs309.stocks.Portfolios.Portfolio;
import cs309.stocks.Portfolios.PortfolioRepository;
import cs309.stocks.Users.User;
import cs309.stocks.Users.UserRepository;
import cs309.stocks.api.FinnHub;
import cs309.stocks.userAnnouncements.UserAnnouncementsRepository;
import cs309.stocks.websockets.ClassroomMessageRepository;
import cs309.stocks.websockets.Message;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/classroom")
public class ClassroomController {

    @Autowired
    private ClassroomRepository classroomRepository;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private UserAnnouncementsRepository userAnnouncementsRepository;

    @Autowired
    private ClassroomMessageRepository classroomMessageRepository;


    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Classroom created successfully"))),
            @ApiResponse(responseCode = "404", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Teacher not found")))
    })

    @PostMapping("/create")
    public ResponseEntity<?> createClassroom(
            @RequestParam String name,
            @RequestParam(required = false) Double startingAmount) {

        // Get the current authenticated user's username
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        // Find the teacher by authenticated username
        Optional<User> teacherOpt = Optional.ofNullable(userRepository.findByUsername(currentUsername));
        if (teacherOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Teacher not found");
        }

        User teacher = teacherOpt.get();

        // Generate a unique join code
        String joinCode;
        do {
            joinCode = generateJoinCode();
        } while (classroomRepository.existsByJoinCode(joinCode));

        // Create a new classroom
        Classroom classroom = new Classroom(name, teacher, startingAmount, joinCode);

        //classId
        //joinCode

        // Save the classroom
        classroomRepository.save(classroom);

        // Prepare the JSON response
        Map<String, Object> response = new HashMap<>();
        response.put("classId", classroom.getClassroom_id());
        response.put("joinCode", classroom.getJoinCode());

        return ResponseEntity.ok(response);
    }

    // Method to generate a random 6-digit join code
    private String generateJoinCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);  // Ensure a 6-digit number
        return String.valueOf(code);
    }


    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Classroom deleted successfully"))),
            @ApiResponse(responseCode = "404", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Classroom not found")))
    })


    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteClassroomByTeacherUsername(
            @RequestParam int classroomId
    ) {
        // Get the current authenticated user's username
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        // Find the teacher by the authenticated username
        Optional<User> teacherOpt = Optional.ofNullable(userRepository.findByUsername(currentUsername));

        if (teacherOpt.isPresent()) {
            User teacher = teacherOpt.get();

            // Find the classroom to delete from the teacher's list of classrooms using a loop
            Classroom classroom = null;
            for (Classroom c : teacher.getTeacherClassrooms()) {
                if (c.getClassroom_id() == classroomId) {
                    classroom = c;
                    break;
                }
            }

            if (classroom != null) {
                // Clear the teacher's reference to the classroom
                teacher.getTeacherClassrooms().remove(classroom);
                userRepository.save(teacher);

                // Clear the students' references to the classroom
                List<User> students = classroom.getStudents();
                for (User student : students) {
                    student.getStudentClassrooms().remove(classroom);
                    userRepository.save(student);
                }

                // Delete the classroom
                classroomRepository.delete(classroom);
                return ResponseEntity.ok("Classroom deleted successfully");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Classroom not found for this teacher");
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Teacher not found");
        }
    }


    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Student added to classroom"))),
            @ApiResponse(responseCode = "404", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Teacher not found | Classroom not found for this teacher | Student not found")))
    })


    // Add a student to a classroom by join code
    @PostMapping("/join-classroom")
    public ResponseEntity<?> joinClassroom(
            @RequestParam String joinCode
    ) {
        // Get the current authenticated user's username
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        // Find the student by authenticated username
        Optional<User> studentOpt = Optional.ofNullable(userRepository.findByUsername(currentUsername));
        if (studentOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Student not found");
        }

        User student = studentOpt.get();

        // Find the classroom by join code
        Optional<Classroom> classroomOpt = Optional.ofNullable(classroomRepository.findByJoinCode(joinCode));
        if (classroomOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Classroom not found");
        }

        Classroom classroom = classroomOpt.get();

        // Add the student to the classroom and update the student's classroom reference
        classroom.addStudent(student);

        Portfolio classroomPortfolio = new Portfolio(classroom.getName() + " Portfolio", classroom.getStartingAmount(), student, classroom);
        portfolioRepository.save(classroomPortfolio);

        // Save both the classroom and the student
        classroomRepository.save(classroom);
        userRepository.save(student);

        return ResponseEntity.ok("Student added to classroom");
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Student removed from classroom"))),
            @ApiResponse(responseCode = "404", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Teacher not found | Classroom not found for this teacher | Student not found"))),
            @ApiResponse(responseCode = "400", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Student is not in the classroom"))),
    })

    @PostMapping("/delete-student-by-teacher")
    public ResponseEntity<?> deleteStudentByTeacher(
            @RequestParam String studentName,
            @RequestParam int classroomId
    ) {
        // Get the current authenticated user's username (i.e., the teacher)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        // Find the teacher by authenticated username
        Optional<User> teacherOpt = Optional.ofNullable(userRepository.findByUsername(currentUsername));
        if (teacherOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Teacher not found");
        }

        User teacher = teacherOpt.get();

        // Force loading of classrooms (since it's a lazy-loaded field)
        List<Classroom> teacherClassrooms = teacher.getTeacherClassrooms();
        teacherClassrooms.size(); // Trigger lazy loading

        // Debug: Print out all classroom IDs for this teacher
        System.out.println("Classrooms for teacher: " + teacherClassrooms.stream().map(Classroom::getClassroom_id).toList());

        // Find the classroom to delete the student from using the teacher's classrooms list
        Classroom classroom = teacherClassrooms.stream()
                .filter(c -> c.getClassroom_id() == classroomId)
                .findFirst()
                .orElse(null);

        if (classroom == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Classroom not found for this teacher");
        }

        // Find the student by username
        Optional<User> studentOpt = Optional.ofNullable(userRepository.findByUsername(studentName));
        if (studentOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Student not found");
        }

        User student = studentOpt.get();

        // Remove the student from the classroom's list of students
        if (classroom.getStudents().contains(student)) {
            classroom.removeStudent(student);
            student.getStudentClassrooms().remove(classroom); // Clear the reference to this classroom for the student
            Portfolio portfolio = portfolioRepository.findByUserAndClassroom(student, classroom);
            // Delete the portfolio for this classroom
            portfolioRepository.delete(portfolio);
            //delete the portfolio for the student
            student.deletePortfolio(portfolio);

            // Save both the updated classroom and student
            classroomRepository.save(classroom);
            userRepository.save(student);

            return ResponseEntity.ok("Student removed from classroom");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Student is not in the classroom");
        }
    }

    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    type = "string",
                                    description = "Confirmation message when the student successfully leaves the classroom",
                                    example = "\"Student left the classroom\""
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    type = "string",
                                    description = "Error message when the student or classroom is not found",
                                    example = "\"Student not found | Classroom not found\""
                            )
                    )
            )
    })

    @PostMapping("/leave-classroom")
    public ResponseEntity<?> leaveClassroom(
            @RequestParam int classroomId
    ) {
        // Get the current authenticated user's username
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        // Find the student by authenticated username
        Optional<User> studentOpt = Optional.ofNullable(userRepository.findByUsername(currentUsername));
        if (studentOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Student not found");
        }

        User student = studentOpt.get();

        // Find the classroom in the student's list of classrooms
        Classroom classroom = null;
        for (Classroom c : student.getStudentClassrooms()) {
            if (c.getClassroom_id() == classroomId) {
                classroom = c;
                break;
            }
        }

        if (classroom == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Classroom not found");
        }

        // Remove the student from the classroom's list of students
        classroom.removeStudent(student);

        Portfolio portfolio = portfolioRepository.findByUserAndClassroom(student, classroom);

        // Delete the portfolio for this classroom
        portfolioRepository.delete(portfolio);
        student.deletePortfolio(portfolio);

        // Save both the updated classroom and student
        classroomRepository.save(classroom);
        userRepository.save(student);

        return ResponseEntity.ok("Student left the classroom");
    }


    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    type = "object",
                                    description = "Details about the classroom and its students",
                                    example = "{"
                                            + "\"students\": ["
                                            + "  {"
                                            + "    \"studentId\": 1,"
                                            + "    \"studentUsername\": \"john_doe\","
                                            + "    \"portfolioStatus\": 1234.56,"
                                            + "    \"portfolioId\": 101"
                                            + "  },"
                                            + "  {"
                                            + "    \"studentId\": 2,"
                                            + "    \"studentUsername\": \"jane_smith\","
                                            + "    \"portfolioStatus\": 2345.67"
                                            + "  }"
                                            + "],"
                                            + "\"hasNewAnnouncements\": true"
                                            + "}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    content = @Content(
                            mediaType = "text/plain",
                            schema = @Schema(
                                    type = "string",
                                    description = "Error message if the classroom or user is not found",
                                    example = "Classroom not found | User not found"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    content = @Content(
                            mediaType = "text/plain",
                            schema = @Schema(
                                    type = "string",
                                    description = "Error message if the user does not have permission to view the classroom",
                                    example = "You cannot view this classroom"
                            )
                    )
            )
    })

    @GetMapping("/classInfo")
    public ResponseEntity<?> getClassInfoForTeacher(@RequestParam int classId) {
        // Get the current authenticated user's username
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        // Find the student by authenticated username
        Optional<User> studentOpt = Optional.ofNullable(userRepository.findByUsername(currentUsername));
        if (studentOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        User user = studentOpt.get();

        // Find the classroom by classId
        Optional<Classroom> classroomOpt = classroomRepository.findById(classId);
        if (classroomOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Classroom not found");
        }

        Classroom classroom = classroomOpt.get();

        if (!user.equals(classroom.getTeacher()) && !classroom.getStudents().contains(user)) {
            return new ResponseEntity<>("You cannot view this classroom", HttpStatus.FORBIDDEN);
        }

        // Prepare the list of students with their portfolio status (currently null)
        List<Map<String, Object>> studentInfoList = classroom.getStudents().stream().map(student -> {
            Map<String, Object> studentInfo = new HashMap<>();
            studentInfo.put("studentId", student.getId()); // Add student ID
            studentInfo.put("studentUsername", student.getUsername());

            Portfolio p = portfolioRepository.findByUserAndClassroom(student, classroom);
            double value = 0;
            if (p != null) {
                p.calculateValue(FinnHub.getInstance());
                value = p.getValue();
                value -= classroom.getStartingAmount();
                DecimalFormat df = new DecimalFormat("#.##");
                value = Double.parseDouble(df.format(value));

                if (user.equals(classroom.getTeacher())) {
                    studentInfo.put("portfolioId", p.getId());
                }
            }
            studentInfo.put("portfolioStatus", value);

            return studentInfo;
        }).collect(Collectors.toList());

        // Check if there are any unviewed announcements for this classroom
        boolean hasNewAnnouncements = false;
        if (!user.equals(classroom.getTeacher())) {
            hasNewAnnouncements = userAnnouncementsRepository
                    .existsByUserAndAnnouncementClassroomAndViewedFalse(user, classroom);
        }

        // Add the boolean to the response
        Map<String, Object> response = new HashMap<>();
        response.put("students", studentInfoList);
        response.put("hasNewAnnouncements", hasNewAnnouncements);

        return ResponseEntity.ok(response);
    }


    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    type = "array",
                                    description = "List of classes with relevant details for the user",
                                    example = "[{"
                                            + "\"className\": \"CS101\","
                                            + "\"classId\": 1,"
                                            + "\"teacherName\": \"Prof. Smith\","
                                            + "\"hasNewAnnouncements\": true"
                                            + "},"
                                            + "{"
                                            + "\"className\": \"Math101\","
                                            + "\"classId\": 2,"
                                            + "\"numStudents\": 25,"
                                            + "\"classCode\": \"ABCD1234\","
                                            + "\"hasNewAnnouncements\": false"
                                            + "}]"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    content = @Content(
                            mediaType = "text/plain",
                            schema = @Schema(
                                    type = "string",
                                    description = "Error message when the user is not found",
                                    example = "\"User not found\""
                            )
                    )
            )
    })

    @GetMapping("/classes")
    public ResponseEntity<?> getClassesForUser() {
        // Get the current authenticated user's username
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        // Find the user by authenticated username
        Optional<User> userOpt = Optional.ofNullable(userRepository.findByUsername(currentUsername));
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        User user = userOpt.get();

        // Check if user is a student or a teacher and prepare the list of classes
        List<Map<String, Object>> classesInfoList = new ArrayList<>();

        if (!user.getStudentClassrooms().isEmpty()) {
            // Student role: Gather student-specific data
            classesInfoList.addAll(user.getStudentClassrooms().stream().map(classroom -> {
                Map<String, Object> classInfo = new HashMap<>();
                classInfo.put("className", classroom.getName());
                classInfo.put("classId", classroom.getClassroom_id());
                classInfo.put("teacherName", classroom.getTeacher().getUsername());

                // Check for unviewed announcements for this classroom
                boolean hasNewAnnouncements = userAnnouncementsRepository
                        .existsByUserAndAnnouncementClassroomAndViewedFalse(user, classroom);
                classInfo.put("hasNewAnnouncements", hasNewAnnouncements);

                return classInfo;
            }).toList());
        }

        if (!user.getTeacherClassrooms().isEmpty()) {
            // Teacher role: Gather teacher-specific data
            classesInfoList.addAll(user.getTeacherClassrooms().stream().map(classroom -> {
                Map<String, Object> classInfo = new HashMap<>();
                classInfo.put("className", classroom.getName());
                classInfo.put("classId", classroom.getClassroom_id());
                classInfo.put("numStudents", classroom.getStudents().size());
                classInfo.put("classCode", classroom.getJoinCode());

                // Teachers always have no unviewed announcements
                classInfo.put("hasNewAnnouncements", false);

                return classInfo;
            }).toList());
        }

        // Return the combined list of classes for either role
        return ResponseEntity.ok(classesInfoList);
    }


    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Student added successfully to classroom"))),
            @ApiResponse(responseCode = "404", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Teacher not found | Classroom not found | Student not found"))),
            @ApiResponse(responseCode = "403", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "You do not own this classroom"))),
            @ApiResponse(responseCode = "400", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Student is already in this classroom")))
    })
    @PostMapping("/add-student")
    public ResponseEntity<?> addStudentByTeacher(
            @RequestParam String joinCode,
            @RequestParam String studentName
    ) {
        // Get the current authenticated user's username (i.e., the teacher)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        // Find the teacher by authenticated username
        Optional<User> teacherOpt = Optional.ofNullable(userRepository.findByUsername(currentUsername));
        if (teacherOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Teacher not found");
        }

        User teacher = teacherOpt.get();

        // Find the classroom by join code and verify it belongs to this teacher
        Optional<Classroom> classroomOpt = Optional.ofNullable(classroomRepository.findByJoinCode(joinCode));
        if (classroomOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Classroom not found");
        }

        Classroom classroom = classroomOpt.get();

        // Verify that the teacher owns this classroom
        if (!teacher.getTeacherClassrooms().contains(classroom)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You do not own this classroom");
        }

        // Find the student by username
        Optional<User> studentOpt = Optional.ofNullable(userRepository.findByUsername(studentName));
        if (studentOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Student not found");
        }

        User student = studentOpt.get();

        // Add the student to the classroom
        if (classroom.getStudents().contains(student)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Student is already in this classroom");
        }

        classroom.addStudent(student);

        Portfolio classroomPortfolio = new Portfolio(classroom.getName() + " Portfolio", classroom.getStartingAmount(), student, classroom);
        portfolioRepository.save(classroomPortfolio);

        // Save both the updated classroom and student
        classroomRepository.save(classroom);
        userRepository.save(student);

        return ResponseEntity.ok("Student added successfully to classroom");
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Classroom name updated successfully"))),
            @ApiResponse(responseCode = "404", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Teacher not found | Classroom not found"))),
            @ApiResponse(responseCode = "403", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "You do not own this classroom")))
    })
    @PutMapping("/change-name")
    public ResponseEntity<?> changeClassroomName(
            @RequestParam int classroomId,
            @RequestParam String newName
    ) {
        // Get the current authenticated user's username (i.e., the teacher)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        // Find the teacher by authenticated username
        Optional<User> teacherOpt = Optional.ofNullable(userRepository.findByUsername(currentUsername));
        if (teacherOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Teacher not found");
        }

        User teacher = teacherOpt.get();

        // Find the classroom by classroomId and verify it belongs to this teacher
        Classroom classroom = teacher.getTeacherClassrooms().stream()
                .filter(c -> c.getClassroom_id() == classroomId)
                .findFirst()
                .orElse(null);

        if (classroom == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Classroom not found");
        }

        // Update the classroom name
        classroom.setName(newName);
        classroomRepository.save(classroom);

        return ResponseEntity.ok("Classroom name updated successfully");
    }


    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    type = "array",
                                    example = """
                                            [
                                              {
                                                "content": "Hello, everyone!",
                                                "username": "john_doe",
                                                "time": "2024-12-06T14:34:00Z"
                                              },
                                              {
                                                "content": "Welcome to the class!",
                                                "username": "jane_smith",
                                                "time": "2024-12-06T14:35:00Z"
                                              }
                                            ]"""
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    content = @Content(
                            mediaType = "text/plain",
                            schema = @Schema(type = "string", example = "Classroom not found")
                    )
            )
    })
    @GetMapping("/chatHistory")
    public ResponseEntity<?> getChatHistory(@RequestParam int classId) {
        // Check if the classroom exists
        Optional<Classroom> classroomOpt = classroomRepository.findById(classId);
        if (classroomOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Classroom not found");
        }

        // Fetch messages for the classroom
        List<Message> messages = classroomMessageRepository.findByClassroomId(classId);

        // Transform messages into JSON-friendly objects
        List<Map<String, Object>> response = messages.stream().map(message -> {
            Map<String, Object> messageInfo = new HashMap<>();
            messageInfo.put("content", cleanContent(message.getContent())); // Clean the content
            messageInfo.put("username", message.getUserName());
            messageInfo.put("time", message.getSent());
            return messageInfo;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Cleans the message content by removing trailing \r\n if present.
     */
    private String cleanContent(String content) {
        if (content == null) {
            return "";
        }
        return content.strip(); // Removes leading and trailing whitespace, including \r\n
    }


}

