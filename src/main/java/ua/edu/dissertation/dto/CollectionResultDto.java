package ua.edu.dissertation.dto;

public record CollectionResultDto(int collectedVideoCount, int mergedDefenseCount) {

    public boolean isEmpty() {
        return collectedVideoCount == 0;
    }
}

