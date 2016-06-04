package fi.solita.adele.usagestats;

import fi.solita.adele.event.Event;
import fi.solita.adele.event.EventRepository;
import fi.solita.adele.event.EventType;
import fi.solita.adele.event.OccupiedStatusSolver;
import fi.solita.adele.place.status.PlaceStatus;
import fi.solita.adele.place.status.PlaceStatusRepository;
import fi.solita.adele.usagestats.model.UsageStats;
import fi.solita.adele.utils.StatisticsUtils;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

        for (int placeId : StatisticsUtils.getPlaceIds(place_ids, startStatusForPlaces)) {
            final Optional<Boolean> startOccupiedStatus = StatisticsUtils.getStartStatusForPlaceId(placeId,
                                                                                                   startStatusForPlaces);
            final List<Event> eventsForPlace = StatisticsUtils.getEventsForPlace(placeId, events);

            if(eventsForPlace.isEmpty()) {
                // Nothing to calculate
                continue;
            }

            LocalDateTime previousEventTime = starting;
            Optional<Boolean> isPreviousEventOccupied = startOccupiedStatus;

            for (Event event : eventsForPlace) {
                StatisticsUtils.addEventToPeriodToSum(event, previousEventTime, isPreviousEventOccupied, periodSums);
                previousEventTime = event.getTime();
                isPreviousEventOccupied = Optional.of(OccupiedStatusSolver.isOccupied(event.getType(), event.getValue()));
            }

            // Add period after last event to timespan end
            if (isPreviousEventOccupied.isPresent()) {
                StatisticsUtils.addPeriodToSum(isPreviousEventOccupied.get(), previousEventTime, ending, periodSums);
            }
        }


        UsageStats usageStats = new UsageStats();
        usageStats.setAverage(StatisticsUtils.calculateAverage(periodSums));
        usageStats.setType(eventType.get());
        return usageStats;
    }
}
