package cs309.stocks.userAnnouncements;

import cs309.stocks.Announcements.Announcement;
import cs309.stocks.Users.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class UserAnnouncements {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement;

    @Column(nullable = false)
    private boolean viewed = false; // Default value is false, indicating not viewed

    // Constructor for easy initialization
    public UserAnnouncements(User user, Announcement announcement) {
        this.user = user;
        this.announcement = announcement;
    }
}
