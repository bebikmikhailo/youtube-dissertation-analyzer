package ua.edu.dissertation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ua.edu.dissertation.domain.entity.Channel;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, String> {

    Optional<Channel> findByName(String name);

    @Query("select c.name from Channel c order by c.name")
    List<String> findAllNames();
}
