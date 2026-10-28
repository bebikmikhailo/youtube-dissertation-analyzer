package ua.edu.dissertation.client.youtube;

import ua.edu.dissertation.client.youtube.dto.YouTubeVideoDto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class YouTubeDemoDataFactory {

    private YouTubeDemoDataFactory() {
    }

    public static List<YouTubeVideoDto> createDemoVideos() {
        List<YouTubeVideoDto> videos = new ArrayList<>();

        videos.add(YouTubeVideoDto.builder()
                .id("nubip_labenko_p1")
                .title("Захист дисертації Лабенка О.М. (частина 1)")
                .description("Разова спеціалізована вчена рада ДФ 26.004.001. Захист дисертації Лабенка Олександра Миколайовича на тему «Фінансове забезпечення інноваційного розвитку аграрних підприємств» за спеціальністю 072 Фінанси, банківська справа та страхування. НУБіП України.")
                .publishedAt(LocalDate.of(2023, 11, 20))
                .durationSeconds(3720)
                .viewCount(245)
                .likeCount(18)
                .commentCount(4)
                .hasSubtitles(true)
                .channelId("UC_NUBIP_OFFICIAL")
                .channelTitle("НУБіП України")
                .build());

        videos.add(YouTubeVideoDto.builder()
                .id("nubip_labenko_p2")
                .title("Захист дисертації Лабенка О.М. (частина 2)")
                .description("Продовження засідання разової спеціалізованої вченої ради ДФ 26.004.001 захисту дисертації Лабенка Олександра Миколайовича. НУБіП України.")
                .publishedAt(LocalDate.of(2023, 11, 20))
                .durationSeconds(2540)
                .viewCount(165)
                .likeCount(11)
                .commentCount(2)
                .hasSubtitles(true)
                .channelId("UC_NUBIP_OFFICIAL")
                .channelTitle("НУБіП України")
                .build());

        videos.add(YouTubeVideoDto.builder()
                .id("kpi_shevchenko_121")
                .title("Публічний захист дисертації Шевченка Д.В. на здобуття ступеня доктора філософії")
                .description("Спеціалізована вчена рада Д 26.002.01 КПІ ім. Ігоря Сікорського. Здобувач: Шевченко Дмитро Володимирович. Тема: «Методи машинного навчання для виявлення кібератак у хмарних сховищах». Спеціальність: 121 Інженерія програмного забезпечення.")
                .publishedAt(LocalDate.of(2024, 1, 18))
                .durationSeconds(4500)
                .viewCount(410)
                .likeCount(34)
                .commentCount(8)
                .hasSubtitles(false)
                .channelId("UC_KPI_MEDIA")
                .channelTitle("КПІмедіа")
                .build());

        videos.add(YouTubeVideoDto.builder()
                .id("knu_melnyk_051")
                .title("Захист дисертації Мельника А.С. на тему «Моделювання макроекономічної стабільності»")
                .description("Разова вчена рада КНУ імені Тараса Шевченка. Здобувач: Мельник Артем Сергійович. Спеціальність: 051 Економіка.")
                .publishedAt(LocalDate.of(2023, 12, 14))
                .durationSeconds(3900)
                .viewCount(320)
                .likeCount(22)
                .commentCount(5)
                .hasSubtitles(true)
                .channelId("UC_KNU_OFFICIAL")
                .channelTitle("КНУ ім. Тараса Шевченка")
                .build());

        videos.add(YouTubeVideoDto.builder()
                .id("lp_bondarenko_122")
                .title("Захист дисертації Бондаренко О.І. на тему «Архітектура розподілених інтелектуальних систем»")
                .description("Спеціалізована вчена рада Національного університету «Львівська політехніка». Здобувач: Бондаренко Олена Ігорівна. Спеціальність: 122 Комп'ютерні науки.")
                .publishedAt(LocalDate.of(2024, 2, 5))
                .durationSeconds(4100)
                .viewCount(290)
                .likeCount(27)
                .commentCount(3)
                .hasSubtitles(true)
                .channelId("UC_LP_OFFICIAL")
                .channelTitle("Львівська політехніка")
                .build());

        return videos;
    }
}
