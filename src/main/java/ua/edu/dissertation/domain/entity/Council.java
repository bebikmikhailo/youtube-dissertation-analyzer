package ua.edu.dissertation.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "councils")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Council {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "university", nullable = false)
    private String university;

    @OneToMany(mappedBy = "council", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Dissertation> dissertations = new ArrayList<>();

    public void addDissertation(Dissertation dissertation) {
        dissertations.add(dissertation);
        dissertation.setCouncil(this);
    }

    public void removeDissertation(Dissertation dissertation) {
        dissertations.remove(dissertation);
        dissertation.setCouncil(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Council council = (Council) o;
        return id != null && Objects.equals(id, council.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
