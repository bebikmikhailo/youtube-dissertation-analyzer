package ua.edu.dissertation.dto;

public enum ReportFormat {
    EXCEL("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    PDF("pdf", "application/pdf");

    private final String extension;
    private final String contentType;

    ReportFormat(String extension, String contentType) {
        this.extension = extension;
        this.contentType = contentType;
    }

    public String getExtension() {
        return extension;
    }

    public String getContentType() {
        return contentType;
    }

    public static ReportFormat fromValue(String value) {
        boolean isEmptyValue = value == null || value.isBlank();
        if (isEmptyValue) {
            return EXCEL;
        }

        String normalized = value.trim();
        for (ReportFormat format : values()) {
            boolean matchesName = format.name().equalsIgnoreCase(normalized);
            boolean matchesExtension = format.getExtension().equalsIgnoreCase(normalized);
            if (matchesName || matchesExtension) {
                return format;
            }
        }
        return EXCEL;
    }
}

