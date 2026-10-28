package ua.edu.dissertation.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "dissertations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dissertation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "author_full_name", nullable = false)
    private String authorFullName;

    @Column(name = "topic", nullable = false, length = 1000)
    private String topic;

    @Column(name = "specialty")
    private String specialty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "council_id")
    private Council council;

    @OneToMany(mappedBy = "dissertation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DefenseVideo> videos = new ArrayList<>();

    public void addVideo(DefenseVideo video) {
        videos.add(video);
        video.setDissertation(this);
    }

    public void removeVideo(DefenseVideo video) {
        videos.remove(video);
        video.setDissertation(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Dissertation that = (Dissertation) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
