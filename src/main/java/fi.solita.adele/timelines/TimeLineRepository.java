package fi.solita.adele.timelines;

import fi.solita.adele.event.Event;
import fi.solita.adele.event.EventRepository;
import fi.solita.adele.event.EventType;
import fi.solita.adele.event.OccupiedStatusSolver;
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
import java.util.List;
import java.util.Optional;

@Repository
public class TimeLineRepository {

    @Resource
    private EventRepository eventRepository;

    @Resource
    private PlaceStatusRepository placeStatusRepository;

    @Transactional
    public List<TimeLine> occupationTimeLine(final Optional<Integer[]> device_ids, final Optional<Integer[]> place_ids, Optional<LocalDateTime> ending, final int minutes){
        LocalDateTime now = ending.orElse(LocalDateTime.now());
        LocalDateTime starting = now.minus(minutes, ChronoUnit.MINUTES);
        final List<PlaceStatus> startStatusForPlaces = placeStatusRepository.getCurrentStatusForAllPlaces(Optional.of( starting));
        final List<Event> eventsForPlaces = eventRepository.all(Optional.of(starting),
                                                                Optional.of(now),
                                                                device_ids,
                                                                place_ids,
                                                                Optional.of(EventType.movement));

        List<TimeLine> rangesPrePlace = new ArrayList<>();
        if (place_ids.isPresent()) {
            for (Integer place_id : place_ids.get()) {
                List<Range> ranges = new ArrayList<>();

                boolean previousOccupiedStatus = StatisticsUtils.getStartStatusForPlaceId(place_id,
                                                                                          startStatusForPlaces).orElse(false);
                LocalDateTime previousStatusTime = starting;
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
                    ranges.add(new Range(previousStatusTime, now));
                } else if (tempRange.getStartTime() != null) {
                    ranges.add(new Range(tempRange.getStartTime(), previousStatusTime));
                }

                rangesPrePlace.add(new TimeLine(place_id, ranges));
            }
        }
        return rangesPrePlace;
    }

}
