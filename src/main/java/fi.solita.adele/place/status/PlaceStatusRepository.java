package fi.solita.adele.place.status;

import com.google.common.collect.Maps;
import fi.solita.adele.event.EventType;
import fi.solita.adele.event.OccupiedStatusSolver;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    @Transactional
    public Map<LocalDateTime, PlaceStatus> geStatusForPlaces(LocalDateTime starting, LocalDateTime ending, Integer[] place_ids, int interval, ChronoUnit unit) {
        Map<LocalDateTime, PlaceStatus> reservedMap = Maps.newHashMap();
        for (LocalDateTime time = starting; time.isBefore(ending); time = time.plus(interval, unit)) {
            Optional<PlaceStatus> placeStatusOptional = getStatusForPlaces(place_ids[0], Optional.of(time));
            final LocalDateTime time1 = time;
            placeStatusOptional.ifPresent(placeStatus -> {
                reservedMap.put(time1, placeStatus);
            });
        }
        return reservedMap;
    }

    private List<PlaceStatus> getCurrentStatusForPlaces(final Optional<Integer[]> placeIds, final Optional<LocalDateTime> atDate) {
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("types", Arrays.asList(EventType.occupied.toString(), EventType.movement.toString()));

        final String where = placeIds
                .filter(ids -> ids.length > 0)
                .map(ids -> {
                    params.addValue("place_ids", Arrays.asList(ids));
                    return " where e.place_id in (:place_ids) ";
                })
                .orElse("");

        // Ugly hack to make a delay to no movement -> not occupied
        final String subqueryMovementRestriction =
                " and (not (type = 'movement' and value < 0.1 and time > (localtime - interval '15 seconds'))) ";

        final String subqueryWhere = atDate.map(date -> {
            params.addValue("time", Timestamp.valueOf(date));
            return " and time <= :time ";
        }).orElse("");

        final String sql = "select e.place_id, p.latitude, p.longitude, e.type, e.value, e.time " +
                "from event as e " +
                "left join place as p on e.place_id = p.id " +
                "inner join (" +
                "  select place_id, max(time) as latest_time" +
                "  from event" +
                "  where type in(:types) " +
                subqueryWhere + subqueryMovementRestriction +
                "  group by place_id" +
                ") as latest_event on latest_event.place_id = e.place_id and latest_event.latest_time = e.time " +
                where;

        return namedParameterJdbcTemplate.query(sql, params, placeStatusRowMapper);
    }
}
