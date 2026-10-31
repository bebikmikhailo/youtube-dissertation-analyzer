package ua.edu.dissertation.service;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ua.edu.dissertation.domain.entity.Channel;
import ua.edu.dissertation.domain.entity.Council;
import ua.edu.dissertation.domain.entity.DefenseVideo;
import ua.edu.dissertation.domain.entity.Dissertation;
import ua.edu.dissertation.dto.VideoFilterCriteria;
import ua.edu.dissertation.dto.VideoFilterCriteria.SortField;
import ua.edu.dissertation.repository.DefenseVideoRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class VideoQueryService {

    private static final char LIKE_ESCAPE_CHAR = '\\';

    private final DefenseVideoRepository defenseVideoRepository;

    public VideoQueryService(DefenseVideoRepository defenseVideoRepository) {
        this.defenseVideoRepository = defenseVideoRepository;
    }

    public List<DefenseVideo> findVideos(VideoFilterCriteria criteria) {
        VideoFilterCriteria effectiveCriteria = criteria != null ? criteria : new VideoFilterCriteria();
        Sort sorting = sort(effectiveCriteria.getSortField(), effectiveCriteria.getSortDirection());
        return defenseVideoRepository.findAll(filter(effectiveCriteria), sorting);
    }

    public Page<DefenseVideo> findVideos(VideoFilterCriteria criteria, int page, int size) {
        VideoFilterCriteria effectiveCriteria = criteria != null ? criteria : new VideoFilterCriteria();
        Sort sorting = sort(effectiveCriteria.getSortField(), effectiveCriteria.getSortDirection());
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.max(size, 1), sorting);
        return defenseVideoRepository.findAll(filter(effectiveCriteria), pageRequest);
    }

    public Specification<DefenseVideo> filter(VideoFilterCriteria criteria) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (criteria == null) {
                return builder.conjunction();
            }

            boolean hasChannelFilter = StringUtils.hasText(criteria.getChannelName());
            boolean hasCouncilFilter = StringUtils.hasText(criteria.getCouncilOrUniversity());
            boolean hasSpecialtyFilter = StringUtils.hasText(criteria.getSpecialty());
            boolean hasSearchText = StringUtils.hasText(criteria.getSearchText());

            if (hasChannelFilter) {
                Join<DefenseVideo, Channel> channel = root.join("channel", JoinType.LEFT);
                predicates.add(containsIgnoreCase(builder, channel.get("name"), criteria.getChannelName()));
            }

            boolean needsDissertationJoin = hasCouncilFilter || hasSpecialtyFilter || hasSearchText;
            Join<DefenseVideo, Dissertation> dissertation = needsDissertationJoin
                    ? root.join("dissertation", JoinType.LEFT)
                    : null;

            if (hasCouncilFilter) {
                Join<Dissertation, Council> council = dissertation.join("council", JoinType.LEFT);
                String councilQuery = criteria.getCouncilOrUniversity();
                predicates.add(builder.or(
                        containsIgnoreCase(builder, council.get("name"), councilQuery),
                        containsIgnoreCase(builder, council.get("university"), councilQuery)
                ));
            }

            if (hasSpecialtyFilter) {
                predicates.add(containsIgnoreCase(builder, dissertation.get("specialty"), criteria.getSpecialty()));
            }

            if (criteria.getPublishedFrom() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("publishedAt"), criteria.getPublishedFrom()));
            }
            if (criteria.getPublishedTo() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("publishedAt"), criteria.getPublishedTo()));
            }

            if (criteria.getMinViews() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("viewCount"), criteria.getMinViews()));
            }
            if (criteria.getMaxViews() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("viewCount"), criteria.getMaxViews()));
            }

            if (hasSearchText) {
                String searchText = criteria.getSearchText();
                predicates.add(builder.or(
                        containsIgnoreCase(builder, root.get("title"), searchText),
                        containsIgnoreCase(builder, root.get("description"), searchText),
                        containsIgnoreCase(builder, dissertation.get("authorFullName"), searchText),
                        containsIgnoreCase(builder, dissertation.get("topic"), searchText)
                ));
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public Sort sort(SortField field, Sort.Direction direction) {
        SortField effectiveField = field != null ? field : SortField.PUBLISHED_AT;
        Sort.Direction effectiveDirection = direction != null ? direction : Sort.Direction.DESC;
        // Сортування за id гарантує стабільний порядок записів з однаковими значеннями.
        return Sort.by(effectiveDirection, effectiveField.getPropertyName())
                .and(Sort.by(Sort.Direction.ASC, "id"));
    }

    private Predicate containsIgnoreCase(CriteriaBuilder builder, Expression<String> field, String value) {
        String pattern = "%" + escapeLikeWildcards(value.trim().toLowerCase(Locale.ROOT)) + "%";
        return builder.like(builder.lower(field), pattern, LIKE_ESCAPE_CHAR);
    }

    private String escapeLikeWildcards(String value) {
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}

