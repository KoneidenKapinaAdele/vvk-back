package fi.solita.adele.place;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class PlaceRepository {

    private static final String PLACE = "PLACE";
    private static final RowMapper<Place> placeRowMapper = (rs, rowNum) -> {
        Place place = new Place();
        place.setId(rs.getInt("ID"));
        place.setName(rs.getString("NAME"));
        place.setLatitude(rs.getDouble("LATITUDE"));
        place.setLongitude(rs.getDouble("LONGITUDE"));
        return place;
    };

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Transactional
    public List<Place> allPlaces() {
        String sql = "select * from " + PLACE;
        return jdbcTemplate.query(sql, placeRowMapper);
    }

    @Transactional
    public int addPlace(final CreatePlaceCommand place) {
        final KeyHolder keyHolder = new GeneratedKeyHolder();
        final String sql = "insert into " + PLACE + " (name, latitude, longitude) values (?, ?, ?)";
        final PreparedStatementCreator statementCreator = connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, place.getName());
            ps.setDouble(2, place.getLatitude());
            ps.setDouble(3, place.getLongitude());
            return ps;
        };

        jdbcTemplate.update(statementCreator, keyHolder);
        return keyHolder.getKey().intValue();
    }

    @Transactional
    public Place getPlace(final int id) {
        Object[] args = {id};
        return jdbcTemplate.queryForObject("select * from " + PLACE + " where id = ? ", args, placeRowMapper);
    }

    @Transactional
    public Place updatePlace(int id, CreatePlaceCommand place) {
        final String sql = "update " + PLACE + " set (name, latitude, longitude) values (?, ?, ?) where id = ?";
        Object[] args = {place.getName(), place.getLatitude(), place.getLongitude(), id};
        jdbcTemplate.update(sql, args);
        return getPlace(id);
    }
}
