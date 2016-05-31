package fi.solita.adele.usagestats;

import fi.solita.adele.event.Event;
import fi.solita.adele.event.EventRepository;
import fi.solita.adele.event.EventType;
import fi.solita.adele.event.OccupiedStatusSolver;
import fi.solita.adele.place.status.PlaceStatus;
import fi.solita.adele.place.status.PlaceStatusRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class UsageStatsRepository {

    @Resource
    private EventRepository eventRepository;

    @Resource
    private PlaceStatusRepository placeStatusRepository;

    public UsageStats getUsageStats(final LocalDateTime starting,
                                    final LocalDateTime ending,
                                    final Optional<Integer[]> device_ids,
                                    final Optional<Integer[]> place_ids) {

        final Optional<EventType> eventType = Optional.of(EventType.movement);
        final List<PlaceStatus> startStatusForPlaces = placeStatusRepository.getCurrentStatusForAllPlaces(Optional.of(starting));
        final List<Event> events = eventRepository.all(Optional.of(starting), Optional.of(ending), device_ids, place_ids, eventType);

        final Map<Boolean, Long> periodSums = new HashMap<>();

        for (int placeId : getPlaceIds(place_ids, startStatusForPlaces)) {
            final Optional<Boolean> startOccupiedStatus = getStartStatusForPlaceId(placeId, startStatusForPlaces);
            final List<Event> eventsForPlace = getEventsForPlace(placeId, events);

            if(eventsForPlace.isEmpty()) {
                // Nothing to calculate
                continue;
            }

            LocalDateTime previousEventTime = starting;
            Optional<Boolean> isPreviousEventOccupied = startOccupiedStatus;

            for (Event event : eventsForPlace) {
                addEventToPeriodToSum(event, previousEventTime, isPreviousEventOccupied, periodSums);
                previousEventTime = event.getTime();
                isPreviousEventOccupied = Optional.of(OccupiedStatusSolver.isOccupied(event.getType(), event.getValue()));
            }

            // Add period after last event to timespan end
            if (isPreviousEventOccupied.isPresent()) {
                addPeriodToSum(isPreviousEventOccupied.get(), previousEventTime, ending, periodSums);
            }
        }


        UsageStats usageStats = new UsageStats();
        usageStats.setAverage(calculateAverage(periodSums));
        usageStats.setType(eventType.get());
        return usageStats;
    }

    private static double calculateAverage(final Map<Boolean, Long> periodSums) {
        final double occupiedMs = periodSums.getOrDefault(Boolean.TRUE, 0L).doubleValue();
        final double freeMs = periodSums.getOrDefault(Boolean.FALSE, 0L).doubleValue();
        final double totalMs = freeMs + occupiedMs;
        return totalMs == 0L ? 0.0 : occupiedMs / totalMs;
    }

    private static Optional<Boolean> getStartStatusForPlaceId(final int place_id, final List<PlaceStatus> startStatusForPlaces) {
        return startStatusForPlaces
                .stream()
                .filter(status -> status.getPlace_id() == place_id)
                .findFirst()
                .map(status -> status.isOccupied());
    }

    private static List<Integer> getPlaceIds(final Optional<Integer[]> place_id, final List<PlaceStatus> startStatusForPlaces) {
        return place_id
                .filter(ids -> ids.length > 0)
                .map(Arrays::asList)
                .orElseGet(() -> startStatusForPlaces
                        .stream()
                        .map(status -> status.getPlace_id())
                        .collect(Collectors.toList()));
    }

    private static List<Event> getEventsForPlace(final int place_id, final List<Event> events) {
        return events.stream()
                .filter(e -> e.getPlace_id() == place_id)
                .sorted((a, b) -> a.getTime().compareTo(b.getTime()))
                .collect(Collectors.toList());
    }

    private static long getMillisecondsBetween(final LocalDateTime a, final LocalDateTime b) {
        return ChronoUnit.MILLIS.between(a, b);
    }

    private static void addEventToPeriodToSum(Event event,
                                              LocalDateTime previousEventTime,
                                              Optional<Boolean> isPreviousEventOccupied,
                                              Map<Boolean, Long> periodSums) {
        isPreviousEventOccupied.ifPresent(isOccupied -> addPeriodToSum(isOccupied, previousEventTime, event.getTime(), periodSums));
    }

    private static void addPeriodToSum(boolean isOccupied,
                                       LocalDateTime starting,
                                       LocalDateTime ending,
                                       Map<Boolean, Long> periodSums) {
        long periodMs = getMillisecondsBetween(starting, ending);
        periodSums.merge(isOccupied, periodMs, (oldValue, newValue) -> oldValue + newValue);
    }
}
