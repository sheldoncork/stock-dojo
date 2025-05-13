package cs309.stocks.Announcements;

import cs309.stocks.Users.User;
import cs309.stocks.Users.UserRepository;
import cs309.stocks.Users.UserRole;
import cs309.stocks.classroom.Classroom;
import cs309.stocks.classroom.ClassroomRepository;
import cs309.stocks.docs.AnnouncementDocs;
import cs309.stocks.userAnnouncements.UserAnnouncements;
import cs309.stocks.userAnnouncements.UserAnnouncementsRepository;
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

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/announcements")
public class AnnouncementController {
    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClassroomRepository classroomRepository;

    @Autowired
    private UserAnnouncementsRepository userAnnouncementsRepository;

    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = AnnouncementDocs.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Announcement created"))),
            @ApiResponse(responseCode = "404", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Classroom not found"))),
            @ApiResponse(responseCode = "403", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "You are not the teacher of this classroom"))),
    })
    @PostMapping
    public ResponseEntity<String> postAnnouncement(@RequestParam int classroomId, @RequestBody Announcement announcement) {
        User user = getCurrentUser();
        Optional<Classroom> classroomOptional = classroomRepository.findById(classroomId);

        if (classroomOptional.isEmpty()) {
            return new ResponseEntity<>("Classroom not found", HttpStatus.NOT_FOUND);
        }

        Classroom classroom = classroomOptional.get();

        if (!user.equals(classroom.getTeacher())) {
            return new ResponseEntity<>("You are not the teacher of this classroom", HttpStatus.FORBIDDEN);
        }

        // Save the announcement
        announcement.setClassroom(classroom);
        announcement.setPostDate(Timestamp.from(Instant.now()));
        announcement.setEdited(false);
        announcementRepository.save(announcement);

        // Populate the UserAnnouncements table
        List<User> students = classroom.getStudents();
        for (User student : students) {
            UserAnnouncements userAnnouncement = new UserAnnouncements(student, announcement);
            userAnnouncementsRepository.save(userAnnouncement);
        }

        return new ResponseEntity<>("Announcement created", HttpStatus.OK);
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Announcement.class))),
            @ApiResponse(responseCode = "404", content = @Content(mediaType = "application/json", schema = @Schema(example = "null"))),
            @ApiResponse(responseCode = "403", content = @Content(mediaType = "application/json", schema = @Schema(example = "null")))
    })
    @GetMapping
    public ResponseEntity<Announcement> getAnnouncement(@RequestParam int announcementId) {
        User user = getCurrentUser();

        // Check if the user has sufficient permissions
        if (user.getRole() == UserRole.STANDARD) {
            return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
        }

        // Fetch the announcement
        Announcement announcement = announcementRepository.findById(announcementId);
        if (announcement == null) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }

        Classroom classroom = announcement.getClassroom();

        // Check if the user is authorized to view the announcement
        if (!user.equals(classroom.getTeacher()) && !classroom.getStudents().contains(user)) {
            return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
        }

        // Update the viewed status for this user and announcement
        UserAnnouncements userAnnouncement = userAnnouncementsRepository
                .findByUserAndAnnouncement(user, announcement)
                .stream()
                .findFirst()
                .orElse(null);

        if (userAnnouncement != null && !userAnnouncement.isViewed()) {
            userAnnouncement.setViewed(true);
            userAnnouncementsRepository.save(userAnnouncement);
        }

        // Add classroom name to the response
        announcement.setClassroomName(classroom.getName());

        return new ResponseEntity<>(announcement, HttpStatus.OK);
    }


    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    type = "array",
                                    description = "List of announcements with additional 'notViewed' property",
                                    example = "[{"
                                            + "\"id\": 1,"
                                            + "\"title\": \"Upcoming Project Deadline\","
                                            + "\"content\": \"Submit your project by the end of the week.\","
                                            + "\"postDate\": \"2024-12-09T12:00:00Z\","
                                            + "\"edited\": false,"
                                            + "\"editedDate\": null,"
                                            + "\"classroomName\": \"Math 201\","
                                            + "\"notViewed\": true"
                                            + "}]"
                            )
                    )
            ),
            @ApiResponse(responseCode = "403", content = @Content(mediaType = "application/json", schema = @Schema(example = "null")))
    })

    @GetMapping("/all")
    public ResponseEntity<List<Map<String, Object>>> getAllAnnouncements() {
        User user = getCurrentUser();

        if (user.getRole() == UserRole.STANDARD) {
            return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
        }

        List<Classroom> classrooms;
        if (user.getRole() == UserRole.TEACHER) {
            classrooms = user.getTeacherClassrooms();
        } else {
            classrooms = user.getStudentClassrooms();
        }

        List<Map<String, Object>> responseList = new ArrayList<>();

        for (Classroom classroom : classrooms) {
            for (Announcement a : classroom.getAnnouncements()) {
                // Create a map to include Announcement fields and hasViewed status
                Map<String, Object> announcementMap = new HashMap<>();
                announcementMap.put("id", a.getId());
                announcementMap.put("title", a.getTitle());
                announcementMap.put("content", a.getContent());
                announcementMap.put("postDate", a.getPostDate());
                announcementMap.put("edited", a.isEdited());
                announcementMap.put("editedDate", a.getEditedDate());
                announcementMap.put("classroomName", classroom.getName());

                // Determine hasViewed status for students
                boolean notViewed = userAnnouncementsRepository
                        .findByUserAndAnnouncement(user, a)
                        .stream() // Stream the list
                        .findFirst() // Get the first matching entry if it exists
                        .map(UserAnnouncements::isViewed) // Map to the viewed status
                        .orElse(false); // Default to false if no match is found

                announcementMap.put("notViewed", !notViewed); // Invert for unviewed announcements

                responseList.add(announcementMap);
            }
        }

        return new ResponseEntity<>(responseList, HttpStatus.OK);
    }


    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    type = "array",
                                    description = "List of announcements with additional 'notViewed' property",
                                    example = "[{"
                                            + "\"id\": 1,"
                                            + "\"title\": \"Midterm Review Session\","
                                            + "\"content\": \"Join us for a detailed review before the midterms.\","
                                            + "\"postDate\": \"2024-12-10T10:30:00Z\","
                                            + "\"edited\": false,"
                                            + "\"editedDate\": null,"
                                            + "\"classroomName\": \"CS101\","
                                            + "\"notViewed\": true"
                                            + "}]"
                            )
                    )
            ),
            @ApiResponse(responseCode = "404", content = @Content(mediaType = "application/json", schema = @Schema(example = "null"))),
            @ApiResponse(responseCode = "403", content = @Content(mediaType = "application/json", schema = @Schema(example = "null")))
    })
    @GetMapping("/classroom")
    public ResponseEntity<List<Map<String, Object>>> getClassroomAnnouncements(@RequestParam int classroomId) {
        User user = getCurrentUser();

        if (user.getRole() == UserRole.STANDARD) {
            return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
        }

        Optional<Classroom> c = classroomRepository.findById(classroomId);
        if (c.isEmpty()) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
        Classroom classroom = c.get();
        if (!user.equals(classroom.getTeacher()) && !classroom.getStudents().contains(user)) {
            return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
        }

        List<Map<String, Object>> responseList = new ArrayList<>();
        List<Announcement> announcements = classroom.getAnnouncements();

        for (Announcement a : announcements) {
            // Create a map to include Announcement fields and hasViewed status
            Map<String, Object> announcementMap = new HashMap<>();
            announcementMap.put("id", a.getId());
            announcementMap.put("title", a.getTitle());
            announcementMap.put("content", a.getContent());
            announcementMap.put("postDate", a.getPostDate());
            announcementMap.put("edited", a.isEdited());
            announcementMap.put("editedDate", a.getEditedDate());
            announcementMap.put("classroomName", classroom.getName());

            // Determine hasViewed status for students
            boolean notViewed = userAnnouncementsRepository
                    .findByUserAndAnnouncement(user, a)
                    .stream() // Stream the list
                    .findFirst() // Get the first matching entry if it exists
                    .map(UserAnnouncements::isViewed) // Map to the viewed status
                    .orElse(false); // Default to false if no match is found

            announcementMap.put("notViewed", !notViewed); // Invert for unviewed announcements

            responseList.add(announcementMap);
        }

        return new ResponseEntity<>(responseList, HttpStatus.OK);
    }


    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Announcement deleted"))),
            @ApiResponse(responseCode = "404", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Announcement not found"))),
            @ApiResponse(responseCode = "403", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "You cannot delete this announcement")))
    })
    @DeleteMapping
    public ResponseEntity<String> deleteAnnouncement(@RequestParam int announcementId) {
        User user = getCurrentUser();

        // Fetch the announcement
        Announcement announcement = announcementRepository.findById(announcementId);
        if (announcement == null) {
            return new ResponseEntity<>("Announcement not found", HttpStatus.NOT_FOUND);
        }

        Classroom classroom = announcement.getClassroom();

        // Check if the user is authorized to delete the announcement
        if (!user.equals(classroom.getTeacher())) {
            return new ResponseEntity<>("You cannot delete this announcement", HttpStatus.FORBIDDEN);
        }

        // Delete associated UserAnnouncements records
        List<UserAnnouncements> userAnnouncements = userAnnouncementsRepository.findByAnnouncement(announcement);
        userAnnouncementsRepository.deleteAll(userAnnouncements);

        // Delete the announcement itself
        announcementRepository.delete(announcement);

        return new ResponseEntity<>("Announcement deleted", HttpStatus.OK);
    }


    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = AnnouncementDocs.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Announcement updated successfully"))),
            @ApiResponse(responseCode = "404", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Announcement not found"))),
            @ApiResponse(responseCode = "403", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "You cannot edit this announcement"))),
    })
    @PutMapping
    public ResponseEntity<String> editAnnouncement(@RequestBody Announcement announcement) {
        User user = getCurrentUser();
        Announcement oldAnnouncement = announcementRepository.findById(announcement.getId());

        if (oldAnnouncement == null) {
            return new ResponseEntity<>("Announcement not found", HttpStatus.NOT_FOUND);
        }

        if (!user.equals(oldAnnouncement.getClassroom().getTeacher())) {
            return new ResponseEntity<>("You cannot edit this announcement", HttpStatus.FORBIDDEN);
        }

        // Update contents
        oldAnnouncement.setTitle(announcement.getTitle());
        oldAnnouncement.setContent(announcement.getContent());

        // Set edit date
        oldAnnouncement.setEdited(true);
        oldAnnouncement.setEditedDate(Timestamp.from(Instant.now()));
        announcementRepository.save(oldAnnouncement);

        // Reset 'viewed' to false for all associated UserAnnouncements
        List<UserAnnouncements> userAnnouncements = userAnnouncementsRepository.findByAnnouncement(oldAnnouncement);
        userAnnouncements.forEach(userAnnouncement -> userAnnouncement.setViewed(false));
        userAnnouncementsRepository.saveAll(userAnnouncements);

        return new ResponseEntity<>("Announcement updated successfully", HttpStatus.OK);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username);
    }
}
