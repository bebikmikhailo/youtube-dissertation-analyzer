package ua.edu.dissertation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.edu.dissertation.domain.entity.Channel;

import java.util.Optional;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, String> {

    Optional<Channel> findByName(String name);
}
