package ua.edu.dissertation.dto;

public enum MultiPartExportMode {
    SUMMARY_ONLY(1, "Тільки сумарні показники по дисертаціях"),
    PARTS_ONLY(2, "Показники окремо по кожній частині відео"),
    BOTH(3, "Обидва варіанти (сумарні + вкладені частини)");

    private final int code;
    private final String description;

    MultiPartExportMode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static MultiPartExportMode fromValue(String value) {
        boolean isEmptyValue = value == null || value.isBlank();
        if (isEmptyValue) {
            return SUMMARY_ONLY;
        }

        String normalized = value.trim();
        for (MultiPartExportMode mode : values()) {
            boolean matchesName = mode.name().equalsIgnoreCase(normalized);
            boolean matchesCode = String.valueOf(mode.getCode()).equals(normalized);
            if (matchesName || matchesCode) {
                return mode;
            }
        }
        return SUMMARY_ONLY;
    }
}

