package fi.solita.adele.device;

import fi.solita.adele.event.EventType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class DeviceStatus {
    public int device_id;
    public int place_id;
    public LocalDateTime last_update;
    public EventType type;

    public DeviceStatus(int device_id, int place_id, LocalDateTime last_update, EventType type) {
        this.device_id = device_id;
        this.place_id = place_id;
        this.last_update = last_update;
        this.type = type;
    }
}
