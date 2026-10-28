package ua.edu.dissertation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.edu.dissertation.domain.entity.StatisticsSnapshot;

import java.util.List;

@Repository
public interface StatisticsSnapshotRepository extends JpaRepository<StatisticsSnapshot, Long> {

    List<StatisticsSnapshot> findByVideoIdOrderByCollectedAtAsc(Long videoId);

    List<StatisticsSnapshot> findByVideoYoutubeVideoIdOrderByCollectedAtAsc(String youtubeVideoId);
}
