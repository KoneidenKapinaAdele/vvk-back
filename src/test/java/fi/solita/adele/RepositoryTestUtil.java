package fi.solita.adele;

import fi.solita.adele.event.CreateEventCommand;
import fi.solita.adele.event.EventType;
import fi.solita.adele.place.CreatePlaceCommand;

import java.time.LocalDateTime;
import java.util.Optional;

public class RepositoryTestUtil {
    public static CreatePlaceCommand generatePlace() {
        CreatePlaceCommand place = new CreatePlaceCommand();
        place.setName("foo");
        place.setLatitude(2.33);
        place.setLongitude(3.44);
        return place;
    }

    public static CreateEventCommand generateEvent(double value, int device_id, int place_id, LocalDateTime time, EventType eventType) {
        CreateEventCommand event = new CreateEventCommand();
        event.setValue(value);
        event.setDevice_id(device_id);
        event.setPlace_id(Optional.of(place_id));
        event.setTime(Optional.of(time));
        event.setType(eventType);
        return event;
    }
}
