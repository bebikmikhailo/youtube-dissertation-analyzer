package ua.edu.dissertation.service;

import org.springframework.stereotype.Service;
import ua.edu.dissertation.client.youtube.dto.YouTubeVideoDto;
import ua.edu.dissertation.domain.entity.DefenseVideo;
import ua.edu.dissertation.dto.ParsedDissertationMetadata;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DissertationMetadataParser {

    private static final Pattern STRUCTURED_AUTHOR_PATTERN = Pattern.compile(
            "(?:Здобувач|Дисертант|Автор)[:\\s]+([А-ЯІЇЄҐ][а-яіїєґ\\']+(?:\\s+[А-ЯІЇЄҐ][а-яіїєґ\\']+){1,2})",
            Pattern.UNICODE_CHARACTER_CLASS
    );

    private static final Pattern GENITIVE_AUTHOR_PATTERN = Pattern.compile(
            "(?:захист(?:у)?\\s+дисертації|дисертаці(?:я|ї))\\s+([А-ЯІЇЄҐ][а-яіїєґ\\']+(?:\\s+[А-ЯІЇЄҐ][а-яіїєґ\\']+){1,2})",
            Pattern.UNICODE_CHARACTER_CLASS | Pattern.CASE_INSENSITIVE
    );

    private static final Pattern INITIALS_AUTHOR_PATTERN = Pattern.compile(
            "(?:захист(?:у)?\\s+дисертації|дисертаці(?:я|ї))\\s+([А-ЯІЇЄҐ][а-яіїєґ\\']+\\s+[А-ЯІЇЄҐ]\\.[\\s]*[А-ЯІЇЄҐ]\\.?)",
            Pattern.UNICODE_CHARACTER_CLASS | Pattern.CASE_INSENSITIVE
    );

    private static final Pattern TOPIC_AFTER_KEYWORD_QUOTED = Pattern.compile(
            "(?:на\\s+тему|Тема:?)\\s*[«\"“]([^»\"”]+)[»\"”]",
            Pattern.UNICODE_CHARACTER_CLASS | Pattern.CASE_INSENSITIVE
    );

    private static final Pattern TOPIC_AFTER_KEYWORD_PLAIN = Pattern.compile(
            "(?:на\\s+тему|Тема:?)\\s*([^.\\n\\r]{10,})(?=\\s*(?:за\\s+спеціальністю|спеціальність|спеціальності|\\.|\\n|$))",
            Pattern.UNICODE_CHARACTER_CLASS | Pattern.CASE_INSENSITIVE
    );

    private static final Pattern QUOTED_CANDIDATE_TOPIC = Pattern.compile(
            "[«\"“]([^»\"”]{15,})[»\"”]",
            Pattern.UNICODE_CHARACTER_CLASS
    );

    private static final Pattern SPECIALTY_WITH_CODE_AND_NAME = Pattern.compile(
            "(?:спеціальністю|спеціальність|спеціальності|спец\\.?)\\s*:?\\s*(\\d{3})\\s+([^.\\n\\r,]+)",
            Pattern.UNICODE_CHARACTER_CLASS | Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SPECIALTY_CODE_ONLY = Pattern.compile(
            "(?:спеціальністю|спеціальність|спеціальності|спец\\.?)\\s*:?\\s*(\\d{3})\\b",
            Pattern.UNICODE_CHARACTER_CLASS | Pattern.CASE_INSENSITIVE
    );

    private static final Pattern COUNCIL_CIPHER_PATTERN = Pattern.compile(
            "\\b([ДD][ФF]?\\s*\\d{2}\\.\\d{3}\\.\\d{2,3})\\b",
            Pattern.UNICODE_CHARACTER_CLASS
    );

    private static final Pattern COUNCIL_FULL_NAME_PATTERN = Pattern.compile(
            "((?:разова\\s+)?(?:спеціалізована\\s+)?вчена\\s+рада\\s+[ДD][ФF]?\\s*\\d{2}\\.\\d{3}\\.\\d{2,3})",
            Pattern.UNICODE_CHARACTER_CLASS | Pattern.CASE_INSENSITIVE
    );

    private static final Map<String, String> COMMON_FIRST_NAMES_GENITIVE = Map.ofEntries(
            Map.entry("Олександра", "Олександр"),
            Map.entry("Дмитра", "Дмитро"),
            Map.entry("Артема", "Артем"),
            Map.entry("Івана", "Іван"),
            Map.entry("Сергія", "Сергій"),
            Map.entry("Андрія", "Андрій"),
            Map.entry("Миколи", "Микола"),
            Map.entry("Олексія", "Олексій"),
            Map.entry("Юрія", "Юрій"),
            Map.entry("Олега", "Олег"),
            Map.entry("Василя", "Василь"),
            Map.entry("Михайла", "Михайло"),
            Map.entry("Володимира", "Володимир"),
            Map.entry("Павла", "Павло"),
            Map.entry("Богдана", "Богдан"),
            Map.entry("Романа", "Роман"),
            Map.entry("Тараса", "Тарас"),
            Map.entry("Віктора", "Віктор"),
            Map.entry("Віталія", "Віталій"),
            Map.entry("Євгена", "Євген"),
            Map.entry("Максима", "Максим"),
            Map.entry("Ярослава", "Ярослав"),
            Map.entry("Олени", "Олена"),
            Map.entry("Марії", "Марія"),
            Map.entry("Тетяни", "Тетяна"),
            Map.entry("Наталії", "Наталія"),
            Map.entry("Ольги", "Ольга"),
            Map.entry("Ірини", "Ірина"),
            Map.entry("Анни", "Анна"),
            Map.entry("Юлії", "Юлія"),
            Map.entry("Оксани", "Оксана"),
            Map.entry("Катерини", "Катерина"),
            Map.entry("Світлани", "Світлана"),
            Map.entry("Вікторії", "Вікторія")
    );

    public ParsedDissertationMetadata parse(String title, String description) {
        String safeTitle = title != null ? title : "";
        String safeDescription = description != null ? description : "";

        String author = extractAuthor(safeTitle, safeDescription);
        String topic = extractTopic(safeTitle, safeDescription);
        String specialtyCode = extractSpecialtyCode(safeTitle, safeDescription);
        String specialtyName = extractSpecialtyName(safeTitle, safeDescription);
        String council = extractCouncil(safeTitle, safeDescription);
        String university = extractUniversity(safeTitle, safeDescription);

        String specialtyFull = formatFullSpecialty(specialtyCode, specialtyName);

        return ParsedDissertationMetadata.builder()
                .authorFullName(author)
                .topic(topic)
                .specialtyCode(specialtyCode)
                .specialtyName(specialtyName)
                .specialty(specialtyFull)
                .council(council)
                .university(university)
                .build();
    }

    public ParsedDissertationMetadata parse(DefenseVideo video) {
        if (video == null) {
            return ParsedDissertationMetadata.builder().build();
        }
        return parse(video.getTitle(), video.getDescription());
    }

    public ParsedDissertationMetadata parse(YouTubeVideoDto videoDto) {
        if (videoDto == null) {
            return ParsedDissertationMetadata.builder().build();
        }
        return parse(videoDto.getTitle(), videoDto.getDescription());
    }

    public String extractAuthor(String title, String description) {
        String safeTitle = title != null ? title : "";
        String safeDescription = description != null ? description : "";

        Matcher structuredMatcher = STRUCTURED_AUTHOR_PATTERN.matcher(safeDescription);
        boolean hasStructuredAuthor = structuredMatcher.find();
        if (hasStructuredAuthor) {
            return cleanWhitespace(structuredMatcher.group(1));
        }

        Matcher descriptionGenitiveMatcher = GENITIVE_AUTHOR_PATTERN.matcher(safeDescription);
        boolean hasDescriptionGenitive = descriptionGenitiveMatcher.find();
        if (hasDescriptionGenitive) {
            String extractedRaw = descriptionGenitiveMatcher.group(1);
            return normalizeAuthorNameToNominative(extractedRaw);
        }

        Matcher titleGenitiveMatcher = GENITIVE_AUTHOR_PATTERN.matcher(safeTitle);
        boolean hasTitleGenitive = titleGenitiveMatcher.find();
        if (hasTitleGenitive) {
            String extractedRaw = titleGenitiveMatcher.group(1);
            return normalizeAuthorNameToNominative(extractedRaw);
        }

        Matcher titleInitialsMatcher = INITIALS_AUTHOR_PATTERN.matcher(safeTitle);
        boolean hasTitleInitials = titleInitialsMatcher.find();
        if (hasTitleInitials) {
            String extractedRaw = titleInitialsMatcher.group(1);
            return normalizeAuthorNameToNominative(extractedRaw);
        }

        Matcher descriptionInitialsMatcher = INITIALS_AUTHOR_PATTERN.matcher(safeDescription);
        boolean hasDescriptionInitials = descriptionInitialsMatcher.find();
        if (hasDescriptionInitials) {
            String extractedRaw = descriptionInitialsMatcher.group(1);
            return normalizeAuthorNameToNominative(extractedRaw);
        }

        return "";
    }

    public String extractTopic(String title, String description) {
        String safeTitle = title != null ? title : "";
        String safeDescription = description != null ? description : "";

        Matcher titleQuotedTopicMatcher = TOPIC_AFTER_KEYWORD_QUOTED.matcher(safeTitle);
        boolean hasTitleQuotedTopic = titleQuotedTopicMatcher.find();
        if (hasTitleQuotedTopic) {
            return cleanWhitespace(titleQuotedTopicMatcher.group(1));
        }

        Matcher descriptionQuotedTopicMatcher = TOPIC_AFTER_KEYWORD_QUOTED.matcher(safeDescription);
        boolean hasDescriptionQuotedTopic = descriptionQuotedTopicMatcher.find();
        if (hasDescriptionQuotedTopic) {
            return cleanWhitespace(descriptionQuotedTopicMatcher.group(1));
        }

        Matcher titlePlainTopicMatcher = TOPIC_AFTER_KEYWORD_PLAIN.matcher(safeTitle);
        boolean hasTitlePlainTopic = titlePlainTopicMatcher.find();
        if (hasTitlePlainTopic) {
            return cleanWhitespace(titlePlainTopicMatcher.group(1));
        }

        Matcher descriptionPlainTopicMatcher = TOPIC_AFTER_KEYWORD_PLAIN.matcher(safeDescription);
        boolean hasDescriptionPlainTopic = descriptionPlainTopicMatcher.find();
        if (hasDescriptionPlainTopic) {
            return cleanWhitespace(descriptionPlainTopicMatcher.group(1));
        }

        Matcher titleCandidateMatcher = QUOTED_CANDIDATE_TOPIC.matcher(safeTitle);
        while (titleCandidateMatcher.find()) {
            String candidate = titleCandidateMatcher.group(1);
            boolean isNotUniversityOrCouncil = !isUniversityOrCouncilName(candidate);
            if (isNotUniversityOrCouncil) {
                return cleanWhitespace(candidate);
            }
        }

        Matcher descriptionCandidateMatcher = QUOTED_CANDIDATE_TOPIC.matcher(safeDescription);
        while (descriptionCandidateMatcher.find()) {
            String candidate = descriptionCandidateMatcher.group(1);
            boolean isNotUniversityOrCouncil = !isUniversityOrCouncilName(candidate);
            if (isNotUniversityOrCouncil) {
                return cleanWhitespace(candidate);
            }
        }

        return "";
    }

    public String extractSpecialtyCode(String title, String description) {
        String combined = (title != null ? title : "") + " " + (description != null ? description : "");

        Matcher matcherWithName = SPECIALTY_WITH_CODE_AND_NAME.matcher(combined);
        boolean hasCodeWithName = matcherWithName.find();
        if (hasCodeWithName) {
            return matcherWithName.group(1);
        }

        Matcher matcherCodeOnly = SPECIALTY_CODE_ONLY.matcher(combined);
        boolean hasCodeOnly = matcherCodeOnly.find();
        if (hasCodeOnly) {
            return matcherCodeOnly.group(1);
        }

        return "";
    }

    public String extractSpecialtyName(String title, String description) {
        String combined = (title != null ? title : "") + " " + (description != null ? description : "");

        Matcher matcherWithName = SPECIALTY_WITH_CODE_AND_NAME.matcher(combined);
        boolean hasCodeWithName = matcherWithName.find();
        if (hasCodeWithName) {
            return cleanWhitespace(matcherWithName.group(2));
        }

        return "";
    }

    public String extractSpecialty(String title, String description) {
        String code = extractSpecialtyCode(title, description);
        String name = extractSpecialtyName(title, description);
        return formatFullSpecialty(code, name);
    }

    public String extractCouncil(String title, String description) {
        String safeDescription = description != null ? description : "";
        String safeTitle = title != null ? title : "";

        Matcher fullNameMatcher = COUNCIL_FULL_NAME_PATTERN.matcher(safeDescription);
        boolean hasFullName = fullNameMatcher.find();
        if (hasFullName) {
            return cleanWhitespace(fullNameMatcher.group(1));
        }

        Matcher descriptionCipherMatcher = COUNCIL_CIPHER_PATTERN.matcher(safeDescription);
        boolean hasDescriptionCipher = descriptionCipherMatcher.find();
        if (hasDescriptionCipher) {
            return cleanWhitespace(descriptionCipherMatcher.group(1));
        }

        Matcher titleCipherMatcher = COUNCIL_CIPHER_PATTERN.matcher(safeTitle);
        boolean hasTitleCipher = titleCipherMatcher.find();
        if (hasTitleCipher) {
            return cleanWhitespace(titleCipherMatcher.group(1));
        }

        return "";
    }

    public String extractUniversity(String title, String description) {
        String combined = (title != null ? title : "") + " " + (description != null ? description : "");

        boolean isKpi = combined.contains("КПІ") || combined.contains("Сікорського");
        if (isKpi) {
            return "КПІ ім. Ігоря Сікорського";
        }

        boolean isNubip = combined.contains("НУБіП") || combined.contains("біоресурсів і природокористування");
        if (isNubip) {
            return "НУБіП України";
        }

        boolean isKnu = combined.contains("КНУ") || combined.contains("імені Тараса Шевченка");
        if (isKnu) {
            return "КНУ імені Тараса Шевченка";
        }

        boolean isLp = combined.contains("Львівська політехніка") || combined.contains("Львівської політехніки");
        if (isLp) {
            return "Національний університет «Львівська політехніка»";
        }

        return "";
    }

    public String normalizeAuthorNameToNominative(String rawAuthor) {
        boolean isInputBlank = rawAuthor == null || rawAuthor.isBlank();
        if (isInputBlank) {
            return "";
        }

        String cleaned = cleanWhitespace(rawAuthor);
        String[] words = cleaned.split("\\s+");

        boolean isThreeWordName = words.length == 3;
        if (isThreeWordName) {
            String surname = declineSurname(words[0]);
            String firstName = declineFirstName(words[1], words[2]);
            String patronymic = declinePatronymic(words[2]);
            return surname + " " + firstName + " " + patronymic;
        }

        boolean isInitialsName = words.length == 2;
        if (isInitialsName) {
            String surname = declineSurname(words[0]);
            return surname + " " + words[1];
        }

        return cleaned;
    }

    private String declineSurname(String surname) {
        boolean endsWithEnka = surname.endsWith("енка");
        if (endsWithEnka) {
            return surname.substring(0, surname.length() - 1) + "о";
        }

        boolean endsWithSkykh = surname.endsWith("ського") || surname.endsWith("цького") || surname.endsWith("зького");
        if (endsWithSkykh) {
            return surname.substring(0, surname.length() - 5) + "ий";
        }

        boolean endsWithOva = surname.endsWith("ова") || surname.endsWith("єва") || surname.endsWith("ева");
        if (endsWithOva) {
            return surname.substring(0, surname.length() - 1);
        }

        boolean endsWithYka = surname.endsWith("ика") || surname.endsWith("ака") || surname.endsWith("ука") || surname.endsWith("юка");
        if (endsWithYka) {
            return surname.substring(0, surname.length() - 1);
        }

        boolean endsWithIna = surname.endsWith("ина") || surname.endsWith("іна") || surname.endsWith("їна");
        if (endsWithIna) {
            return surname.substring(0, surname.length() - 1);
        }

        return surname;
    }

    private String declineFirstName(String firstName, String patronymic) {
        boolean isKnownName = COMMON_FIRST_NAMES_GENITIVE.containsKey(firstName);
        if (isKnownName) {
            return COMMON_FIRST_NAMES_GENITIVE.get(firstName);
        }

        boolean isMalePatronymic = patronymic.endsWith("овича") || patronymic.endsWith("йовича")
                || patronymic.endsWith("ович") || patronymic.endsWith("йович");

        if (isMalePatronymic) {
            boolean endsWithIya = firstName.endsWith("ія");
            if (endsWithIya) {
                return firstName.substring(0, firstName.length() - 2) + "ій";
            }
            boolean endsWithYa = firstName.endsWith("я");
            if (endsWithYa) {
                return firstName.substring(0, firstName.length() - 1) + "ь";
            }
            boolean endsWithTra = firstName.endsWith("тра") || firstName.endsWith("йла") || firstName.endsWith("вла");
            if (endsWithTra) {
                return firstName.substring(0, firstName.length() - 1) + "о";
            }
            boolean endsWithA = firstName.endsWith("а");
            if (endsWithA) {
                return firstName.substring(0, firstName.length() - 1);
            }
        }

        boolean isFemalePatronymic = patronymic.endsWith("івни") || patronymic.endsWith("евни")
                || patronymic.endsWith("євни") || patronymic.endsWith("івна");
        if (isFemalePatronymic) {
            boolean endsWithY = firstName.endsWith("и");
            if (endsWithY) {
                return firstName.substring(0, firstName.length() - 1) + "а";
            }
        }

        return firstName;
    }

    private String declinePatronymic(String patronymic) {
        boolean endsWithOvycha = patronymic.endsWith("овича") || patronymic.endsWith("йовича");
        if (endsWithOvycha) {
            return patronymic.substring(0, patronymic.length() - 1);
        }

        boolean endsWithIvny = patronymic.endsWith("івни") || patronymic.endsWith("евни") || patronymic.endsWith("євни");
        if (endsWithIvny) {
            return patronymic.substring(0, patronymic.length() - 1) + "а";
        }

        return patronymic;
    }

    private boolean isUniversityOrCouncilName(String text) {
        boolean hasUniKeyword = text.contains("університет") || text.contains("інститут")
                || text.contains("академі") || text.contains("політехнік")
                || text.contains("КПІ") || text.contains("НУБіП") || text.contains("КНУ");
        boolean hasCouncilKeyword = text.contains("рада") || text.contains("ДФ") || text.contains("Д 26.");
        return hasUniKeyword || hasCouncilKeyword;
    }

    private String formatFullSpecialty(String code, String name) {
        boolean hasCode = code != null && !code.isBlank();
        boolean hasName = name != null && !name.isBlank();

        if (hasCode && hasName) {
            return code + " " + name;
        }
        if (hasCode) {
            return code;
        }
        if (hasName) {
            return name;
        }
        return "";
    }

    private String cleanWhitespace(String input) {
        if (input == null) {
            return "";
        }
        return input.trim().replaceAll("\\s+", " ");
    }
}

