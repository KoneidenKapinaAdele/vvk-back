package fi.solita.adele.utils;

import fi.solita.adele.event.Event;
import fi.solita.adele.place.status.PlaceStatus;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.Boolean.FALSE;

public final class StatisticsUtils {
    public static double calculateAverage(final Map<Boolean, Long> periodSums) {
    final double occupiedMs = periodSums.getOrDefault(Boolean.TRUE, 0L).doubleValue();
    final double freeMs = periodSums.getOrDefault(FALSE, 0L).doubleValue();
    final double totalMs = freeMs + occupiedMs;
    return totalMs == 0L ? 0.0 : occupiedMs / totalMs;
}

    public static Optional<Boolean> getStartStatusForPlaceId(final int place_id, final List<PlaceStatus> startStatusForPlaces) {
    return startStatusForPlaces
            .stream()
            .filter(status -> status.getPlace_id() == place_id)
            .findFirst()
            .map(status -> status.isOccupied());
}

    public static List<Integer> getPlaceIds(final Optional<Integer[]> place_id, final List<PlaceStatus> startStatusForPlaces) {
    return place_id
            .filter(ids -> ids.length > 0)
            .map(Arrays::asList)
            .orElseGet(() -> startStatusForPlaces.stream()
                                                 .map(status -> status.getPlace_id())
                                                 .collect(Collectors.toList()));
}

    public static List<Event> getEventsForPlace(final int place_id, final List<Event> events) {
    return events.stream()
            .filter(e -> e.getPlace_id() == place_id)
            .sorted((a, b) -> a.getTime().compareTo(b.getTime()))
            .collect(Collectors.toList());
}

    private static long getMillisecondsBetween(final LocalDateTime a, final LocalDateTime b) {
        return ChronoUnit.MILLIS.between(a, b);
    }

    public static void addEventToPeriodToSum(Event event, LocalDateTime previousEventTime, Optional<Boolean> isPreviousEventOccupied, Map<Boolean, Long> periodSums) {
        isPreviousEventOccupied.ifPresent(isOccupied -> addPeriodToSum(isOccupied, previousEventTime, event.getTime(), periodSums));
    }

    public static void addPeriodToSum(boolean isOccupied, LocalDateTime starting, LocalDateTime ending, Map<Boolean, Long> periodSums) {
        long periodMs = getMillisecondsBetween(starting, ending);
        periodSums.merge(isOccupied, periodMs, (oldValue, newValue) -> oldValue + newValue);
    }

    private StatisticsUtils(){ }
}
