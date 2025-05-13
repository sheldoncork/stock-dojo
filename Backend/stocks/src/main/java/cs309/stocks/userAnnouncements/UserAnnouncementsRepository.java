package cs309.stocks.userAnnouncements;

import cs309.stocks.Announcements.Announcement;
import cs309.stocks.Users.User;
import cs309.stocks.classroom.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAnnouncementsRepository extends JpaRepository<UserAnnouncements, Integer> {
    // Retrieve all users associated with a specific announcement
    List<UserAnnouncements> findByAnnouncement(Announcement announcement);

    // Retrieve a specific UserAnnouncements record by user and announcement
    List<UserAnnouncements> findByUserAndAnnouncement(User user, Announcement announcement);

    boolean existsByUserAndAnnouncementClassroomAndViewedFalse(User user, Classroom classroom);
}
