package cs309.stocks.Announcements;


import com.fasterxml.jackson.annotation.JsonIgnore;
import cs309.stocks.classroom.Classroom;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.sql.Timestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String title;

    @Lob
    private String content;

    private Timestamp postDate;
    private boolean edited;
    private Timestamp editedDate;

    @ManyToOne
    @JoinColumn(name = "classroom_id")
    @JsonIgnore
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Classroom classroom;

    @Transient
    private String classroomName;
}
