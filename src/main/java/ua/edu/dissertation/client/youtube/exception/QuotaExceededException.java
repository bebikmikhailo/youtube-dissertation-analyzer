package ua.edu.dissertation.client.youtube.exception;

public class QuotaExceededException extends YouTubeApiException {

    public QuotaExceededException(String message) {
        super(message);
    }

    public QuotaExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
