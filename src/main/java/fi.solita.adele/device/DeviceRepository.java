package fi.solita.adele.device;

import com.google.common.collect.ImmutableList;
import fi.solita.adele.event.EventType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Repository
public class DeviceRepository {

    @Resource
    private JdbcTemplate jdbcTemplate;

    private static LocalDateTime timestampToLocalDate(Timestamp source) {
        return LocalDateTime.ofInstant(source.toInstant(), ZoneId.systemDefault());
    }

    private final RowMapper<DeviceStatus> deviceStatusMap = (row, rowNum) ->
            new DeviceStatus(row.getInt("device_id"), row.getInt("place_id"),
                    timestampToLocalDate(row.getTimestamp("time")),
                    EventType.valueOf(row.getString("type")));

    public List<DeviceStatus> getDeviceStatus() {
        final String sql = "SELECT * FROM event, " +
                "(SELECT device_id, MAX(time) AS last FROM event GROUP BY device_id) AS last " +
                "WHERE event.device_id = last.device_id AND event.time = last.last";
        return jdbcTemplate.query(sql, deviceStatusMap);
    }
}
