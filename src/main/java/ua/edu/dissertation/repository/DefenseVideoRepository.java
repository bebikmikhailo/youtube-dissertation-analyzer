package ua.edu.dissertation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import ua.edu.dissertation.domain.entity.DefenseVideo;

import java.util.List;
import java.util.Optional;

@Repository
public interface DefenseVideoRepository extends JpaRepository<DefenseVideo, Long>, JpaSpecificationExecutor<DefenseVideo> {

    Optional<DefenseVideo> findByYoutubeVideoId(String youtubeVideoId);

    boolean existsByYoutubeVideoId(String youtubeVideoId);

    List<DefenseVideo> findByDissertationId(Long dissertationId);

    List<DefenseVideo> findByChannelId(String channelId);
}
