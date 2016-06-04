package fi.solita.adele.timelines;

import fi.solita.adele.timelines.model.TimeLine;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@CrossOrigin
@RestController
public class TimeLineController {

    @Resource TimeLineRepository timeLineRepository;

    @RequestMapping(value = "/v1/query/timeline", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK) List<TimeLine> timeLine(@RequestParam(value = "minutes", required = true) Integer minutes,
                                                           @RequestParam(value = "ending", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime ending,
                                                           @RequestParam(value = "place_id", required = false) Integer[] place_ids,
                                                           @RequestParam(value = "device_id", required = false) Integer[] device_ids) {
        return timeLineRepository.occupationTimeLine(Optional.ofNullable(device_ids), Optional.ofNullable(place_ids), Optional.of(ending), minutes);
    }
}
