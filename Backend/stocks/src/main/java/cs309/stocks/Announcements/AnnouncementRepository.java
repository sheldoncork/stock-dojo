package cs309.stocks.Announcements;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementRepository extends JpaRepository<Announcement, Integer> {
    Announcement findById(int id);
}
