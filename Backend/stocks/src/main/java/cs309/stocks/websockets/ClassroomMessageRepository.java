package cs309.stocks.websockets;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassroomMessageRepository extends JpaRepository<Message, Long> {

    // Retrieve all messages for a specific classroom
    List<Message> findByClassroomId(int classroomId);
}
