package fi.solita.adele.place.status;

import com.google.common.collect.Lists;
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
import java.util.List;
import java.util.Optional;

import static fi.solita.adele.event.EventType.closed;
import static fi.solita.adele.event.EventType.movement;
import static fi.solita.adele.utils.StatisticsUtils.getEventsForPlace;
import static fi.solita.adele.utils.StatusUtil.getPlaceStatus;
import static java.util.stream.Collectors.toList;

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
    public static final int MAX_OCCUPATION_WITH_NO_MOVEMENT = 10;

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
        Optional<LocalDateTime> starting = Optional.of(now.minusHours(1));
        List<Event> events = eventRepository.all(starting,
                                                 Optional.of(now),
                                                 Optional.<Integer[]>empty(),
                                                 placeIds,
                                                 Optional.of(new EventType[]{movement, closed}));

        return Lists.newArrayList(placeIds.orElse(allPlaces()))
                    .stream()
                    .map(place -> getStatusForPlace(now, starting.get(), events, place))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(toList());
    }

    private Optional<PlaceStatus> getStatusForPlace(LocalDateTime now, LocalDateTime starting, List<Event> events, Integer place_id) {
        Optional<PlaceStatus> status = getPlaceStatus(starting, getEventsForPlace(place_id, events));
        status.ifPresent(s -> setFreeIfLongTimeWithNoEvents(now, s));
        status.ifPresent(s -> setPlaceInformation(place_id, s));
        return status;
    }

    private void setFreeIfLongTimeWithNoEvents(LocalDateTime now, PlaceStatus status) {
        if (status.getLastEventTime().isBefore(now.minusMinutes(MAX_OCCUPATION_WITH_NO_MOVEMENT))) {
            status.setOccupied(false);
        }
    }

    private void setPlaceInformation(Integer place_id, PlaceStatus status) {
        status.setPlace_id(place_id);
        Place place = placeRepository.getPlace(place_id);
        status.setLatitude(place.getLatitude());
        status.setLongitude(place.getLongitude());
    }

    private Integer[] allPlaces() {
        return placeRepository.allPlaces().stream().map(Place::getId).toArray(Integer[]::new);
    }
}
