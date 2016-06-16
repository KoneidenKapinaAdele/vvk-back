package fi.solita.adele.utils;

import com.google.common.collect.Lists;
import fi.solita.adele.event.Event;
import fi.solita.adele.event.EventType;
import fi.solita.adele.event.OccupiedStatusSolver;
import fi.solita.adele.place.status.PlaceStatus;
import fi.solita.adele.timelines.model.Range;
import fi.solita.adele.timelines.model.TimeLine;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static fi.solita.adele.utils.StatisticsUtils.getEventsForPlace;
import static fi.solita.adele.utils.StatisticsUtils.getStartStatusForPlaceId;

public class TimeLineResolver {

    public static final int MAX_OCCUPIED_IF_NO_MOVEMENT = 10;

    public static TimeLine resolveTimeLineForPlace(LocalDateTime starting, LocalDateTime ending, List<PlaceStatus> startStatusForPlaces, List<Event> events, Integer place_id) {
        LocalDateTime now = LocalDateTime.now();
        if (starting.isAfter(now)) {
            return new TimeLine(place_id, Lists.newArrayList());
        }
        boolean doorClosed = getStartStatusForPlaceId(place_id, startStatusForPlaces).orElse(false);
        LocalDateTime doorClosedTime = starting;

        List<Range> ranges = new ArrayList<>();
        Range tempRange = new Range();
        if (doorClosed) {
            tempRange.setStartTime(starting);
        }

        List<Event> eventsForPlace = getEventsForPlace(place_id, events);
        for (Event event : eventsForPlace) {
            if (event.getType().equals(EventType.movement)) {
                boolean movement = OccupiedStatusSolver.isOccupied(event.getType(), event.getValue());
                if (movement && doorClosed && tempRange.getStartTime() == null) {
                    tempRange.setStartTime(doorClosedTime);
                }
            } else {
                if (event.getValue() == 1) {
                    doorClosed = true;
                    doorClosedTime = event.getTime();
                } else {
                    doorClosed = false;
                    if (tempRange.getStartTime() != null) {
                        tempRange.setEndTime(event.getTime());
                        ranges.add(new Range(tempRange.getStartTime(), tempRange.getEndTime()));
                        tempRange = new Range();
                    }
                }
            }
            int nextEvent = eventsForPlace.indexOf(event) + 1;
            if (occupationShouldTimeOut(ending, events, eventsForPlace, event, nextEvent)) {
                if (tempRange.getStartTime() != null) {
                    tempRange.setEndTime(event.getTime().plusMinutes(MAX_OCCUPIED_IF_NO_MOVEMENT));
                    ranges.add(new Range(tempRange.getStartTime(), tempRange.getEndTime()));
                    tempRange = new Range();
                }
            }
        }
        if (tempRange.getStartTime() != null) {
            ranges.add(new Range(tempRange.getStartTime(), ending.isAfter(now) ? now : ending));
        }
        return new TimeLine(place_id, ranges);
    }

    private static boolean occupationShouldTimeOut(LocalDateTime ending, List<Event> events, List<Event> eventsForPlace, Event event, int nextEvent) {
        boolean nextEventIsMaxTimeFromNow = nextEvent < events.size() && eventsForPlace.get(nextEvent)
                                                                                       .getTime()
                                                                                       .isAfter(event.getTime()
                                                                                        .plusMinutes(MAX_OCCUPIED_IF_NO_MOVEMENT));
        boolean latestEventWasOverMaxTimeAgo = nextEvent == events.size() && ending.isAfter(event.getTime().plusMinutes(MAX_OCCUPIED_IF_NO_MOVEMENT));
        return nextEventIsMaxTimeFromNow || latestEventWasOverMaxTimeAgo;
    }
}
