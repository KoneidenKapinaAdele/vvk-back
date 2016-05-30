package fi.solita.adele.place.status;

import fi.solita.adele.event.EventType;
import fi.solita.adele.event.OccupiedStatusSolver;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
        return getCurrentStatusForPlaces(Optional.of(new Integer[] {placeId})).stream().findFirst();
    }

    @Transactional
    public List<PlaceStatus> getCurrentStatusForAllPlaces() {
        return getCurrentStatusForPlaces(Optional.empty());
    }

    private List<PlaceStatus> getCurrentStatusForPlaces(final Optional<Integer[]> placeIds) {
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("types", Arrays.asList(EventType.occupied.toString(), EventType.movement.toString()));

        final String where = placeIds
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
                "  where type in(:types)" +
                "  group by place_id" +
                ") as latest_event on latest_event.place_id = e.place_id and latest_event.latest_time = e.time " +
                where;

        return namedParameterJdbcTemplate.query(sql, params, placeStatusRowMapper);
    }
}
