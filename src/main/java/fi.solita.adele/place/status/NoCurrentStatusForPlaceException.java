package fi.solita.adele.place.status;

public class NoCurrentStatusForPlaceException extends RuntimeException {

    public NoCurrentStatusForPlaceException(int placeId) {
        super("Current status for place id " + placeId + " cannot be resolved because there is no event data for that place.");
    }
}
