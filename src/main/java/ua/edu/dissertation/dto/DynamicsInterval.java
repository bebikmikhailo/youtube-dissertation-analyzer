package ua.edu.dissertation.dto;

public enum DynamicsInterval {
    DAYS("Дні"),
    WEEKS("Тижні"),
    MONTHS("Місяці");

    private final String title;

    DynamicsInterval(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}

