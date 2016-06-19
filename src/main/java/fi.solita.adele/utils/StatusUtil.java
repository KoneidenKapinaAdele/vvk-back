package fi.solita.adele.utils;

import fi.solita.adele.event.Event;
import fi.solita.adele.place.status.PlaceStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static fi.solita.adele.event.EventType.movement;
import static fi.solita.adele.event.OccupiedStatusSolver.isOccupied;
import static fi.solita.adele.utils.DoorStatusResolver.isClosed;

public class StatusUtil {
    public static Optional<PlaceStatus> getPlaceStatus(LocalDateTime starting, List<Event> events) {
        if (events.isEmpty()) {
            return Optional.empty();
        }
        boolean doorClosed = false;
        PlaceStatus status = new PlaceStatus();
        status.setOccupied(false, starting);
        for (Event event : events) {
            switch (event.getType()) {
                case closed:
                    doorClosed = isClosed(event.getValue());
                    if (status.isOccupied()) {
                        status.setOccupied(false, event.getTime());
                    }
                    break;
                case movement:
                    if (isOccupied(movement, event.getValue())) {
                        if (doorClosed) {
                            status.setOccupied(true, event.getTime());
                        }
                    }
                    break;
            }
        }
        return Optional.of(status);

    }
}
