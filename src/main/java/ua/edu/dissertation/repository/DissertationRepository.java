package ua.edu.dissertation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.edu.dissertation.domain.entity.Dissertation;

import java.util.List;

@Repository
public interface DissertationRepository extends JpaRepository<Dissertation, Long> {

    List<Dissertation> findByAuthorFullNameContainingIgnoreCase(String authorFullName);

    List<Dissertation> findBySpecialty(String specialty);

    List<Dissertation> findByCouncilId(Long councilId);
}
