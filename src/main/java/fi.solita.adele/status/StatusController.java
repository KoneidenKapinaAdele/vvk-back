package fi.solita.adele.status;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@CrossOrigin
@RestController
public class StatusController {

    @Resource
    private JdbcTemplate jdbcTemplate;

    private static final String SCHEMA_VERSION = "event";

    @RequestMapping(value = "/status", method = RequestMethod.GET)
    String applicationStatus() {
        return "ok, " + eventsCount() + " events";
    }

    @Transactional
    private Integer eventsCount()  {
        return jdbcTemplate.queryForObject("select count(*) from " + SCHEMA_VERSION, Integer.class);
    }
}
