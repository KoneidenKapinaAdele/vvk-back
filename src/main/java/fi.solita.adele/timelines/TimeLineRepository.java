package fi.solita.adele.timelines;

import fi.solita.adele.event.Event;
import fi.solita.adele.event.EventRepository;
import fi.solita.adele.event.EventType;
import fi.solita.adele.event.OccupiedStatusSolver;
import fi.solita.adele.place.Place;
import fi.solita.adele.place.PlaceRepository;
import fi.solita.adele.place.status.PlaceStatus;
import fi.solita.adele.place.status.PlaceStatusRepository;
import fi.solita.adele.timelines.model.Range;
import fi.solita.adele.timelines.model.TimeLine;
import fi.solita.adele.utils.StatisticsUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Repository
public class TimeLineRepository {

    @Resource
    private EventRepository eventRepository;

    @Resource
    private PlaceStatusRepository placeStatusRepository;

    @Resource
    private PlaceRepository placeRepository;

    @Transactional
    public List<TimeLine> occupationTimeLine(final Optional<Integer[]> device_ids, final Optional<Integer[]> place_ids, Optional<LocalDateTime> now, final int minutes){
        LocalDateTime ending = now.orElse(LocalDateTime.now());
        LocalDateTime starting = ending.minus(minutes, ChronoUnit.MINUTES);
        return solveTimeLinesForPlaces(place_ids.orElse(allPlaces()), device_ids, ending, starting);
    }

    private Integer[] allPlaces() {
        return (Integer[]) placeRepository.allPlaces()
                                          .stream()
                                          .map(Place::getId)
                                          .collect(toList())
                                          .toArray();
    }

    private List<TimeLine> solveTimeLinesForPlaces(Integer[] place_ids, Optional<Integer[]> device_ids, LocalDateTime ending, LocalDateTime starting) {
        final List<PlaceStatus> startStatusesForPlaces = placeStatusRepository.getCurrentStatusForAllPlaces(Optional.of( starting));
        final List<Event> allEvents = eventRepository.all(Optional.of(starting),
                                                                Optional.of(ending),
                                                                device_ids,
                                                                Optional.of(place_ids),
                                                                Optional.of(EventType.movement));
        return Arrays.asList(place_ids)
                     .stream()
                     .map(place_id -> resolveTimeLineForPlace(ending,
                                                              starting,
                                                              startStatusesForPlaces,
                                                              allEvents,
                                                              place_id))
                     .collect(Collectors.toList());
    }

    private TimeLine resolveTimeLineForPlace(LocalDateTime ending, LocalDateTime starting, List<PlaceStatus> startStatusForPlaces, List<Event> eventsForPlaces, Integer place_id) {
        boolean previousOccupiedStatus = StatisticsUtils.getStartStatusForPlaceId(place_id, startStatusForPlaces).orElse(false);
        LocalDateTime previousStatusTime = starting;
        List<Range> ranges = new ArrayList<>();
        Range tempRange = new Range();
        for (Event event : StatisticsUtils.getEventsForPlace(place_id, eventsForPlaces)) {
            if (previousOccupiedStatus) {
                tempRange = new Range();
                tempRange.setStartTime(previousStatusTime);
            } else if (tempRange.getStartTime() != null) {
                tempRange.setEndTime(previousStatusTime);
                ranges.add(new Range(tempRange.getStartTime(), tempRange.getEndTime()));
            }
            previousOccupiedStatus = (OccupiedStatusSolver.isOccupied(event.getType(), event.getValue()));
            previousStatusTime = event.getTime();
        }
        if (previousOccupiedStatus) {
            ranges.add(new Range(previousStatusTime, ending));
        } else if (tempRange.getStartTime() != null) {
            ranges.add(new Range(tempRange.getStartTime(), previousStatusTime));
        }
        return new TimeLine(place_id, ranges);
    }
}
