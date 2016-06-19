package fi.solita.adele.utils;

import com.google.common.collect.Lists;
import fi.solita.adele.event.Event;
import fi.solita.adele.timelines.model.Range;
import fi.solita.adele.timelines.model.TimeLine;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static fi.solita.adele.event.OccupiedStatusSolver.isOccupied;
import static fi.solita.adele.utils.DoorStatusResolver.isClosed;

public class TimeLineResolver {

    public static final int MAX_OCCUPIED_IF_NO_MOVEMENT = 10;

    public static TimeLine resolveTimeLineForPlace(LocalDateTime starting, LocalDateTime ending, Boolean startStatusForPlace, List<Event> eventsForPlace, Integer place_id) {
        LocalDateTime now = LocalDateTime.now();
        if (starting.isAfter(now)) {
            return new TimeLine(place_id, Lists.newArrayList());
        }
        boolean doorClosed = startStatusForPlace;
        LocalDateTime doorClosedTime = starting;
        Optional<LocalDateTime> rangeStartTime = doorClosed ? Optional.of(doorClosedTime) : Optional.empty();

        List<Range> ranges = new ArrayList<>();
        for (Event event : eventsForPlace) {
            switch (event.getType()) {
                case movement:
                    boolean occupied = doorClosed && isOccupied(event.getType(), event.getValue());
                    if (occupied && !rangeStartTime.isPresent()) {
                        rangeStartTime = Optional.of(doorClosedTime);
                    }
                    break;
                case closed:
                    doorClosed = isClosed(event.getValue());
                    if (doorClosed) {
                        doorClosedTime = event.getTime();
                    } else {
                        if (rangeStartTime.isPresent()) {
                            ranges.add(new Range(rangeStartTime.get(), event.getTime()));
                            rangeStartTime = Optional.empty();
                        }
                    }
                    break;
            }
            if (occupationShouldTimeOut(ending, eventsForPlace, event) && rangeStartTime.isPresent()) {
                ranges.add(new Range(rangeStartTime.get(), event.getTime().plusMinutes(MAX_OCCUPIED_IF_NO_MOVEMENT)));
                rangeStartTime = Optional.empty();
            }
        }
        if (rangeStartTime.isPresent()) {
            ranges.add(new Range(rangeStartTime.get(), ending.isAfter(now) ? now : ending));
        }
        return new TimeLine(place_id, ranges);
    }

    private static boolean occupationShouldTimeOut(LocalDateTime ending, List<Event> eventsForPlace, Event event) {
        int nextEvent = eventsForPlace.indexOf(event) + 1;
        LocalDateTime shouldTimeOut = event.getTime().plusMinutes(MAX_OCCUPIED_IF_NO_MOVEMENT);
        boolean nextEventIsMaxTimeFromNow = nextEvent < eventsForPlace.size() && eventsForPlace.get(nextEvent)
                                                                                       .getTime()
                                                                                       .isAfter(shouldTimeOut);
        boolean latestEventWasOverMaxTimeAgo = nextEvent == eventsForPlace.size() && ending.isAfter(shouldTimeOut);
        return nextEventIsMaxTimeFromNow || latestEventWasOverMaxTimeAgo;
    }
}
