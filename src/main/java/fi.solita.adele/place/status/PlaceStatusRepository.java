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
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class PlaceStatusRepository {

    private static final RowMapper<PlaceStatus> placeStatusRowMapper = (rs, rowNum) -> {
        PlaceStatus status = new PlaceStatus();
        status.setPlace_id(rs.getInt("place_id"));
        status.setOccupied(OccupiedStatusSolver.isOccupied(EventType.valueOf(rs.getString("type")), rs.getDouble("value")));
        status.setLatitude(rs.getDouble("latitude"));
        status.setLongitude(rs.getDouble("longitude"));
        return status;
    };

    @Resource
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Transactional
    public Optional<PlaceStatus> getCurrentStatusForPlace(final int placeId) {
        return getStatusForPlace(Optional.of(new Integer[]{placeId}), Optional.<LocalDateTime>empty());
    }

    @Transactional
    public List<PlaceStatus> getCurrentStatusForAllPlaces() {
        return getCurrentStatusForPlaces(Optional.empty(), Optional.<LocalDateTime>empty());
    }

    @Transactional
    public Map<LocalDateTime, Boolean> geStatusForPlaces(LocalDateTime starting, LocalDateTime ending, Integer[] place_ids, int interval, ChronoUnit unit) {
        Map<LocalDateTime, Boolean> reservedMap = Maps.newHashMap();
        for (LocalDateTime time = starting; time.isBefore(ending); time = time.plus(interval, unit)) {
            Optional<PlaceStatus> placeStatusOptional = getStatusForPlace(Optional.of(place_ids), Optional.of(time));
            final LocalDateTime time1 = time;
            placeStatusOptional.ifPresent(placeStatus -> {
                reservedMap.put(time1, placeStatus.isOccupied());
            });
        }
        return reservedMap;
    }

    private Optional<PlaceStatus> getStatusForPlace(Optional<Integer[]> place_ids, Optional<LocalDateTime> time) {
        return getCurrentStatusForPlaces(place_ids, time).stream().findFirst();
    }

    private List<PlaceStatus> getCurrentStatusForPlaces(final Optional<Integer[]> placeIds, Optional<LocalDateTime> ending) {
        List<String> where = new ArrayList<>();
        final MapSqlParameterSource params = new MapSqlParameterSource();

        where.add(" where type in(:types)");
        params.addValue("types", Arrays.asList(EventType.occupied.toString(), EventType.movement.toString()));

        ending.ifPresent(end -> {
            where.add("time <= :end");
            params.addValue("end", Timestamp.valueOf(end));
        });

        final String where2 = placeIds
                .filter(ids -> ids.length > 0)
                .map(ids -> {
                    params.addValue("place_ids", Arrays.asList(ids));
                    return " where e.place_id in (:place_ids)";
                })
                .orElse("");

        final String sql = "select e.place_id, p.latitude, p.longitude, e.type, e.value " +
                "from event as e " +
                "left join place as p on e.place_id = p.id " +
                "inner join (" +
                "  select place_id, max(time) as latest_time" +
                "  from event" +
                where.stream().collect(Collectors.joining(" AND ")) +
                "  group by place_id" +
                ") as latest_event on latest_event.place_id = e.place_id and latest_event.latest_time = e.time " +
                where2;

        return namedParameterJdbcTemplate.query(sql, params, placeStatusRowMapper);
    }
}
