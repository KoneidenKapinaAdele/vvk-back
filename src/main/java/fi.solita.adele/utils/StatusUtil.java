package fi.solita.adele.utils;

import fi.solita.adele.event.Event;
import fi.solita.adele.place.status.PlaceStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static fi.solita.adele.event.EventType.closed;
import static fi.solita.adele.event.EventType.movement;
import static fi.solita.adele.event.OccupiedStatusSolver.isOccupied;
import static fi.solita.adele.utils.DoorStatusResolver.isClosed;
import static fi.solita.adele.utils.StatisticsUtils.getEventsForPlace;

public class StatusUtil {
    public static Optional<PlaceStatus> getPlaceStatus(Optional<LocalDateTime> starting, List<Event> events, Integer place_id) {
        List<Event> eventsForPlace = getEventsForPlace(place_id, events);
        if (eventsForPlace.isEmpty()) {
            return Optional.empty();
        }
        boolean doorClosed = false;
        PlaceStatus status = new PlaceStatus();
        status.setLastEventTime(starting.get());
        status.setOccupied(false);

        for (Event event : eventsForPlace) {
            if (event.getType() == closed) {
                doorClosed = isClosed(event.getValue());
                if (status.isOccupied()) {
                    status.setOccupied(false);
                    status.setLastEventTime(event.getTime());
                }
            } else if (event.getType() == movement){
                if (isOccupied(movement, event.getValue())) {
                    if (doorClosed) {
                        status.setOccupied(true);
                        status.setLastEventTime(event.getTime());
                    }
                }
            }
        }
        return Optional.of(status);
    }


}
