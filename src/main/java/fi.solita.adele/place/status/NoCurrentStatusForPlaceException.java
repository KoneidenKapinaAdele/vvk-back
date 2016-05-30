package fi.solita.adele.place.status;

import java.time.LocalDateTime;
import java.util.Optional;

public class NoCurrentStatusForPlaceException extends RuntimeException {

    public NoCurrentStatusForPlaceException(final int placeId, final Optional<LocalDateTime> atDate) {
        super("Status for place id " + placeId + " cannot be resolved" + atDate.map(date -> " at " + date).orElse(""));
    }
}
