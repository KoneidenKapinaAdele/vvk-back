package fi.solita.adele.usagestats.model;

import fi.solita.adele.event.EventType;
import lombok.AllArgsConstructor;
import lombok.experimental.Builder;

@Builder
@AllArgsConstructor
public class UsageStats {
    private EventType type;
    private double average;

    public UsageStats() {

    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public double getAverage() {
        return average;
    }

    public void setAverage(double average) {
        this.average = average;
    }
}
