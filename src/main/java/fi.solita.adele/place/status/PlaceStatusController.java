package fi.solita.adele.place.status;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@CrossOrigin
@RestController
public class PlaceStatusController {

    @Resource
    private PlaceStatusRepository placeStatusRepository;

    @RequestMapping(value = "/v1/status/current", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<PlaceStatus> getCurrentStatusForAllPlaces() {
        return placeStatusRepository.getCurrentStatusForAllPlaces();
    }

    @RequestMapping(value = "/v1/place/{id}/status/current", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    PlaceStatus getCurrentStatusForPlaces(@PathVariable("id") final int placeId) {
        return placeStatusRepository.getCurrentStatusForPlace(placeId)
                .orElseThrow(() -> new NoCurrentStatusForPlaceException(placeId));
    }

    @ExceptionHandler(NoCurrentStatusForPlaceException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    String handleNoCurrentStatusForPlaceException(NoCurrentStatusForPlaceException ex) {
        return ex.getMessage();
    }
}
