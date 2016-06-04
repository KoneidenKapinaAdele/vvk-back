package fi.solita.adele.timelines.model;

import java.time.LocalDateTime;

public class Range {
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public Range() { }

    public Range(LocalDateTime s, LocalDateTime e) {
        startTime = s;
        endTime = e;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
}
