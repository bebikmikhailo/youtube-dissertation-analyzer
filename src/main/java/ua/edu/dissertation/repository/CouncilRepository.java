package ua.edu.dissertation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.edu.dissertation.domain.entity.Council;

import java.util.List;
import java.util.Optional;

@Repository
public interface CouncilRepository extends JpaRepository<Council, Long> {

    Optional<Council> findByName(String name);

    List<Council> findByUniversity(String university);
}
