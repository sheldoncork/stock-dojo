package cs309.stocks.websockets;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@Table(name = "messages")
@Getter
@Setter
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userName;

    @Lob
    @Column(nullable = false)
    private String content;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "sent", nullable = false)
    private Date sent = new Date();

    @Column(nullable = false)
    private Long classroomId; // ID of the classroom the message belongs to

    // Default constructor
    public Message() {
    }

    // Constructor with fields
    public Message(String userName, String content, Long classroomId) {
        this.userName = userName;
        this.content = content;
        this.classroomId = classroomId;
    }
}
