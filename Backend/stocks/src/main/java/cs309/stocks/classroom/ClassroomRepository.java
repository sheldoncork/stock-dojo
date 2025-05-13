package cs309.stocks.classroom;

import org.springframework.data.jpa.repository.JpaRepository;


public interface ClassroomRepository extends JpaRepository<Classroom, Integer> {
    Classroom findByJoinCode(String joinCode);

    // Method to check if a join code already exists
    boolean existsByJoinCode(String joinCode);
}