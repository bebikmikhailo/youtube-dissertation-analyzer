package ua.edu.dissertation.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "statistics_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatisticsSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    @Column(name = "views", nullable = false)
    private Integer views;

    @Column(name = "likes", nullable = false)
    private Integer likes;

    @Column(name = "comments", nullable = false)
    private Integer comments;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "defense_video_id", nullable = false)
    private DefenseVideo video;

    public DefenseVideo getDefenseVideo() {
        return video;
    }

    public void setDefenseVideo(DefenseVideo defenseVideo) {
        this.video = defenseVideo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StatisticsSnapshot that = (StatisticsSnapshot) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
