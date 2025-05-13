package cs309.stocks.websockets;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cs309.stocks.Users.User;
import cs309.stocks.Users.UserRepository;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ServerEndpoint(value = "/ws/classroom")
public class ClassroomSocket {

    // Maps classroom IDs to their respective sessions (clients)
    private static final Map<String, Map<Session, String>> classroomSessions = new ConcurrentHashMap<>();
    private static final Map<Session, User> sessionUserMap = new ConcurrentHashMap<>();

    private static UserRepository userRepository;
    private static BCryptPasswordEncoder encoder;
    private static ClassroomMessageRepository messageRepository;

    @Autowired
    public void setUserRepository(UserRepository repo) {
        userRepository = repo;
    }

    @Autowired
    public void setEncoder(BCryptPasswordEncoder e) {
        encoder = e;
    }

    @Autowired
    public void setMessageRepository(ClassroomMessageRepository repo) {
        messageRepository = repo;
    }

    @OnOpen
    public void onOpen(Session session) throws IOException {
        // Retrieve classroomId and username from the query parameters
        Map<String, String> params = getQueryParams(session);
        WebSocketAuthenticator auth = new WebSocketAuthenticator(userRepository, encoder);
        User user = auth.authenticate(params.get("username"), params.get("password"));
        String classroomId = params.get("classroomId");

        if (user == null) {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Unauthorized"));
            return;
        }
        // Check if both parameters are provided
        if (classroomId == null) {
            session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "classroomId is required"));
            return;
        }

        sessionUserMap.put(session, user);

        // Add session to the classroom
        classroomSessions.computeIfAbsent(classroomId, k -> new ConcurrentHashMap<>()).put(session, user.getUsername());
    }

    @OnMessage
    public void onMessage(Session session, String messageContent) throws IOException {
        String classroomId = getClassroomIdFromSession(session);
        User user = sessionUserMap.get(session);

        if (user == null || classroomId == null) {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Unauthorized or invalid classroom"));
            return;
        }

        String displayMessage = messageContent;
        // Check if messageContent is in the JSON format and extract "content" field if necessary
        if (messageContent.startsWith("{") && messageContent.endsWith("}")) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, String> jsonMap = objectMapper.readValue(messageContent, new TypeReference<Map<String, String>>() {
                });
                if (jsonMap.containsKey("content")) {
                    messageContent = jsonMap.get("content").trim(); // Use the "content" field and trim whitespace
                }
            } catch (Exception e) {
                e.printStackTrace(); // Log any parsing error
            }
        }

        // Save the message to the database
        Message message = new Message(user.getUsername(), messageContent, Long.parseLong(classroomId));
        messageRepository.save(message);

        // Broadcast the message to everyone in the classroom
        broadcastToClassroom(classroomId, user.getUsername() + ": " + displayMessage);
    }

    @OnClose
    public void onClose(Session session) {
        String classroomId = getClassroomIdFromSession(session);
        classroomSessions.get(classroomId).remove(session);

        // Clean up empty classrooms
        if (classroomSessions.get(classroomId).isEmpty()) {
            classroomSessions.remove(classroomId);
        }

        sessionUserMap.remove(session);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        throwable.printStackTrace();
    }

    /**
     * Extracts query parameters from the session's query string.
     */
    private Map<String, String> getQueryParams(Session session) {
        String query = session.getQueryString();
        Map<String, String> params = new ConcurrentHashMap<>();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] parts = param.split("=");
                if (parts.length > 1) {
                    params.put(parts[0], parts[1]);
                }
            }
        }
        return params;
    }

    /**
     * Retrieves the classroomId associated with the session.
     */
    private String getClassroomIdFromSession(Session session) {
        Map<String, String> params = getQueryParams(session);
        return params.get("classroomId");
    }

    /**
     * Broadcasts a message to all users in a specific classroom.
     *
     * @param classroomId The ID of the classroom to broadcast to.
     * @param message     The message to be broadcast.
     */
    private void broadcastToClassroom(String classroomId, String message) {
        classroomSessions.getOrDefault(classroomId, Map.of()).forEach((session, user) -> {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
