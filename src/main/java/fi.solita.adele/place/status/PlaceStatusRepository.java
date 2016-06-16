package fi.solita.adele.place.status;

import fi.solita.adele.event.Event;
import fi.solita.adele.event.EventRepository;
import fi.solita.adele.event.EventType;
import fi.solita.adele.event.OccupiedStatusSolver;
import fi.solita.adele.place.Place;
import fi.solita.adele.place.PlaceRepository;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static fi.solita.adele.utils.StatisticsUtils.getEventsForPlace;

@Repository
public class PlaceStatusRepository {

    private static final RowMapper<PlaceStatus> placeStatusRowMapper = (rs, rowNum) -> {
        PlaceStatus status = new PlaceStatus();
        status.setPlace_id(rs.getInt("place_id"));
        status.setOccupied(OccupiedStatusSolver.isOccupied(EventType.valueOf(rs.getString("type")), rs.getDouble("value")));
        status.setLatitude(rs.getDouble("latitude"));
        status.setLongitude(rs.getDouble("longitude"));
        status.setLastEventTime(LocalDateTime.ofInstant(rs.getTimestamp("time").toInstant(), ZoneId.systemDefault()));
        return status;
    };

    @Resource
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Resource
    private EventRepository eventRepository;

    @Resource
    private PlaceRepository placeRepository;

    @Transactional
    public Optional<PlaceStatus> getCurrentStatusForPlace(final int placeId, final Optional<LocalDateTime> atDate) {
        return getStatusForPlaces(placeId, atDate);
    }

    private Optional<PlaceStatus> getStatusForPlaces(int placeId, Optional<LocalDateTime> atDate) {
        return getCurrentStatusForPlaces(Optional.of(new Integer[]{placeId}), atDate).stream().findFirst();
    }

    @Transactional
    public List<PlaceStatus> getCurrentStatusForAllPlaces(final Optional<LocalDateTime> atDate) {
        return getCurrentStatusForPlaces(Optional.empty(), atDate);
    }

    private List<PlaceStatus> getCurrentStatusForPlaces(final Optional<Integer[]> placeIds, final Optional<LocalDateTime> atDate) {
        LocalDateTime now = atDate.isPresent() ? atDate.get() : LocalDateTime.now();
        List<Event> events = eventRepository.all(Optional.of(now.minusHours(1)),
                                                 Optional.of(now),
                                                 Optional.<Integer[]>empty(),
                                                 placeIds,
                                                 Optional.of(new EventType[]{EventType.movement, EventType.closed}));

        List<PlaceStatus> statuses = new ArrayList<>();
        for (Integer place_id : placeIds.orElse(allPlaces())) {
            boolean doorClosed = false;
            boolean occupied = false;
            LocalDateTime lastEventTime = null;
            for (Event event : getEventsForPlace(place_id, events)) {
                if (event.getType() == EventType.closed) {
                    if (event.getValue() == 1) {
                        doorClosed = true;
                        if (occupied) {
                            occupied = false;
                            lastEventTime = event.getTime();
                        }
                    } else {
                        doorClosed = false;
                    }
                } else {
                    if (event.getValue() == 1) {
                        if (doorClosed) {
                            occupied = true;
                            lastEventTime = event.getTime();
                        }
                    }
                }
            }
            PlaceStatus status = new PlaceStatus();
            status.setOccupied(occupied);
            status.setLastEventTime(lastEventTime);
            statuses.add(status);
        }
        return statuses;
    }

    private Integer[] allPlaces() {
        return placeRepository.allPlaces().stream().map(Place::getId).toArray(Integer[]::new);
    }
}
