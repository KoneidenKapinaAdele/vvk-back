package fi.solita.adele.place.status;

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
public class PlaceStatusController {

    @Resource
    private PlaceStatusRepository placeStatusRepository;

    @RequestMapping(value = "/v1/place/status", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<PlaceStatus> getCurrentStatusForAllPlaces(@RequestParam(value = "at", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Optional<LocalDateTime> atDate) {
        return placeStatusRepository.getCurrentStatusForAllPlaces(atDate);
    }

    @RequestMapping(value = "/v1/place/{id}/status", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    PlaceStatus getCurrentStatusForPlaces(@PathVariable("id") final int placeId,
                                          @RequestParam(value = "at", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Optional<LocalDateTime> atDate) {
        return placeStatusRepository.getCurrentStatusForPlace(placeId, atDate)
                .orElseThrow(() -> new NoCurrentStatusForPlaceException(placeId, atDate));
    }

    @ExceptionHandler(NoCurrentStatusForPlaceException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    String handleNoCurrentStatusForPlaceException(NoCurrentStatusForPlaceException ex) {
        return ex.getMessage();
    }
}
