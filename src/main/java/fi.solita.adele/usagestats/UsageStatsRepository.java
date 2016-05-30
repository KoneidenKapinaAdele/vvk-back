package fi.solita.adele.usagestats;

import fi.solita.adele.event.EventType;
import fi.solita.adele.place.status.PlaceStatus;
import fi.solita.adele.place.status.PlaceStatusRepository;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

@Repository
public class UsageStatsRepository {

    @Resource
    private PlaceStatusRepository placeStatusRepository;

    public UsageStats getStats(final Optional<LocalDateTime> starting,
                               final Optional<LocalDateTime> ending,
                               final Optional<Integer[]> device_id,
                               final Optional<Integer[]> place_id,
                               final Optional<EventType> type) {

        if (!(starting.isPresent() && ending.isPresent() && place_id.isPresent())) {
            throw new IllegalArgumentException("Must give start and end -times and a device id");
        }

        Map<LocalDateTime, PlaceStatus> statusPerTime = placeStatusRepository.geStatusForPlaces(starting.get(),
                                                                                                ending.get(),
                                                                                                place_id.get(),
                                                                                                5,
                                                                                                ChronoUnit.SECONDS);
        long occupiedEvents = statusPerTime.values().stream().filter(PlaceStatus::isOccupied).count();
        int total = statusPerTime.values().size();

        return UsageStats.builder()
                         .average(((1.0) * occupiedEvents) / total)
                         .type(EventType.occupied)
                         .build();
    }

}
