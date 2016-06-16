package fi.solita.adele.place.status;

import fi.solita.adele.App;
import fi.solita.adele.DeviceTestUtil;
import fi.solita.adele.EventTestUtil;
import fi.solita.adele.PlaceTestUtil;
import fi.solita.adele.event.EventType;
import fi.solita.adele.place.Place;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static fi.solita.adele.EventTestUtil.FREE;
import static fi.solita.adele.EventTestUtil.OCCUPIED;
import static fi.solita.adele.PlaceTestUtil.LOCATION_COMPARISON_DELTA;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = App.class)
@WebAppConfiguration
@IntegrationTest({"server.port:0"})
public class PlaceStatusControllerTest {
    private static final RestTemplate restTemplate = new RestTemplate();

    @Value("${local.server.port}")
    int port;

    private PlaceTestUtil placeTestUtil;
    private EventTestUtil eventTestUtil;

    @Before
    public void setup() {
        placeTestUtil = new PlaceTestUtil(port);
        eventTestUtil = new EventTestUtil(port);
    }

    private String url(String suffix) {
        return "http://localhost:" + port + suffix;
    }

    private List<PlaceStatus> getCurrentStatusForAllPlaces(Optional<LocalDateTime> atDate) {
        String at = atDate.map(date -> "?at=" + date.toString()).orElse("");
        ResponseEntity<PlaceStatus[]> result = restTemplate.getForEntity(url("/v1/place/status" + at), PlaceStatus[].class);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        return Arrays.asList(result.getBody());
    }

    private PlaceStatus getCurrentStatusForPlace(int placeId, Optional<LocalDateTime> atDate) {
        String at = atDate.map(date -> "?at=" + date.toString()).orElse("");
        ResponseEntity<PlaceStatus> result = restTemplate.getForEntity(url("/v1/place/" + placeId + "/status" + at), PlaceStatus.class);
        return result.getBody();
    }

    @Test
    public void should_be_occupied_if_movement_after_closing_door() {
        int deviceId = DeviceTestUtil.getNewDeviceId();
        final int placeId1 = placeTestUtil.addPlace();
        eventTestUtil.addEvent(deviceId, placeId1, LocalDateTime.now().minusMinutes(3), EventType.closed.toString(), OCCUPIED);
        eventTestUtil.addEvent(deviceId,
                               placeId1,
                               LocalDateTime.now().minusMinutes(1),
                               EventType.movement.toString(),
                               OCCUPIED);

        PlaceStatus placeStatus = getCurrentStatusForPlace(placeId1, Optional.empty());

        assertNotNull(placeStatus);
        assertTrue(placeStatus.isOccupied());
    }

    @Test
    public void should_set_free_after_opening_door() {
        int deviceId = DeviceTestUtil.getNewDeviceId();
        final int placeId1 = placeTestUtil.addPlace();
        eventTestUtil.addEvent(deviceId, placeId1, LocalDateTime.now().minusMinutes(3), EventType.closed.toString(),
                               OCCUPIED);
        eventTestUtil.addEvent(deviceId,
                               placeId1,
                               LocalDateTime.now().minusMinutes(2),
                               EventType.movement.toString(),
                               OCCUPIED);
        eventTestUtil.addEvent(deviceId,
                               placeId1,
                               LocalDateTime.now().minusMinutes(1),
                               EventType.closed.toString(),
                               OCCUPIED);

        PlaceStatus placeStatus = getCurrentStatusForPlace(placeId1, Optional.empty());

        assertNotNull(placeStatus);
        assertFalse(placeStatus.isOccupied());
    }


    @Ignore
    @Test
    public void should_list_current_state_for_all_places() {
        int deviceId = DeviceTestUtil.getNewDeviceId();

        final int placeId1 = placeTestUtil.addPlace();
        final int placeId2 = placeTestUtil.addPlace();
        final int placeId3 = placeTestUtil.addPlace();

        final Place place1 = placeTestUtil.getPlace(placeId1);
        final Place place2 = placeTestUtil.getPlace(placeId2);
        final Place place3 = placeTestUtil.getPlace(placeId3);

        eventTestUtil.addEvent(deviceId, placeId1, LocalDateTime.now().minusDays(3), FREE);
        eventTestUtil.addEvent(deviceId, placeId1, LocalDateTime.now().minusDays(2), FREE);
        eventTestUtil.addEvent(deviceId, placeId1, LocalDateTime.now().minusDays(1), OCCUPIED);

        eventTestUtil.addEvent(deviceId, placeId2, LocalDateTime.now().minusDays(3), OCCUPIED);
        eventTestUtil.addEvent(deviceId, placeId2, LocalDateTime.now().minusDays(2), OCCUPIED);
        eventTestUtil.addEvent(deviceId, placeId2, LocalDateTime.now().minusDays(1), FREE);

        List<PlaceStatus> result = getCurrentStatusForAllPlaces(Optional.empty());
        assertNotNull(result);

        Optional<PlaceStatus> place1Status = result.stream().filter(status -> status.getPlace_id() == placeId1).findFirst();
        assertTrue(place1Status.isPresent());
        assertTrue(place1Status.get().isOccupied());
        assertEquals(place1.getLongitude(), place1Status.get().getLongitude(), LOCATION_COMPARISON_DELTA);
        assertEquals(place1.getLatitude(), place1Status.get().getLatitude(), LOCATION_COMPARISON_DELTA);

        Optional<PlaceStatus> place2Status = result.stream().filter(status -> status.getPlace_id() == placeId2).findFirst();
        assertTrue(place2Status.isPresent());
        assertFalse(place2Status.get().isOccupied());
        assertEquals(place2.getLongitude(), place2Status.get().getLongitude(), LOCATION_COMPARISON_DELTA);
        assertEquals(place2.getLatitude(), place2Status.get().getLatitude(), LOCATION_COMPARISON_DELTA);

        assertFalse(result.stream().anyMatch(status -> status.getPlace_id() == placeId3));
    }

    @Ignore
    @Test
    public void should_list_state_for_all_places_at_specific_date() {
        int deviceId = DeviceTestUtil.getNewDeviceId();

        final int placeId1 = placeTestUtil.addPlace();
        final int placeId2 = placeTestUtil.addPlace();
        final int placeId3 = placeTestUtil.addPlace();

        final Place place1 = placeTestUtil.getPlace(placeId1);
        final Place place2 = placeTestUtil.getPlace(placeId2);
        final Place place3 = placeTestUtil.getPlace(placeId3);

        eventTestUtil.addEvent(deviceId, placeId1, LocalDateTime.now().minusDays(3), FREE);
        eventTestUtil.addEvent(deviceId, placeId1, LocalDateTime.now().minusDays(2), FREE);
        eventTestUtil.addEvent(deviceId, placeId1, LocalDateTime.now().minusDays(1), OCCUPIED);

        eventTestUtil.addEvent(deviceId, placeId2, LocalDateTime.now().minusDays(3), OCCUPIED);
        eventTestUtil.addEvent(deviceId, placeId2, LocalDateTime.now().minusDays(2), OCCUPIED);
        eventTestUtil.addEvent(deviceId, placeId2, LocalDateTime.now().minusDays(1), FREE);

        List<PlaceStatus> result = getCurrentStatusForAllPlaces(Optional.of(LocalDateTime.now().minusDays(2)));
        assertNotNull(result);

        Optional<PlaceStatus> place1Status = result.stream().filter(status -> status.getPlace_id() == placeId1).findFirst();
        assertTrue(place1Status.isPresent());
        assertFalse(place1Status.get().isOccupied());
        assertEquals(place1.getLongitude(), place1Status.get().getLongitude(), LOCATION_COMPARISON_DELTA);
        assertEquals(place1.getLatitude(), place1Status.get().getLatitude(), LOCATION_COMPARISON_DELTA);

        Optional<PlaceStatus> place2Status = result.stream().filter(status -> status.getPlace_id() == placeId2).findFirst();
        assertTrue(place2Status.isPresent());
        assertTrue(place2Status.get().isOccupied());
        assertEquals(place2.getLongitude(), place2Status.get().getLongitude(), LOCATION_COMPARISON_DELTA);
        assertEquals(place2.getLatitude(), place2Status.get().getLatitude(), LOCATION_COMPARISON_DELTA);

        assertFalse(result.stream().anyMatch(status -> status.getPlace_id() == placeId3));
    }

    @Ignore
    @Test
    public void should_get_current_state_for_place() {
        int deviceId = DeviceTestUtil.getNewDeviceId();
        final int placeId1 = placeTestUtil.addPlace();
        final Place place1 = placeTestUtil.getPlace(placeId1);

        eventTestUtil.addEvent(deviceId, placeId1, LocalDateTime.now().minusDays(3), FREE);
        eventTestUtil.addEvent(deviceId, placeId1, LocalDateTime.now().minusDays(2), FREE);
        eventTestUtil.addEvent(deviceId, placeId1, LocalDateTime.now().minusDays(1), OCCUPIED);

        PlaceStatus result = getCurrentStatusForPlace(placeId1, Optional.empty());
        assertNotNull(result);
        assertTrue(result.isOccupied());
        assertEquals(place1.getLongitude(), result.getLongitude(), LOCATION_COMPARISON_DELTA);
        assertEquals(place1.getLatitude(), result.getLatitude(), LOCATION_COMPARISON_DELTA);
    }

    @Ignore
    @Test
    public void should_get_state_for_place_at_specific_date() {
        int deviceId = DeviceTestUtil.getNewDeviceId();
        final int placeId1 = placeTestUtil.addPlace();
        final Place place1 = placeTestUtil.getPlace(placeId1);

        eventTestUtil.addEvent(deviceId, placeId1, LocalDateTime.now().minusDays(3), FREE);
        eventTestUtil.addEvent(deviceId, placeId1, LocalDateTime.now().minusDays(2), FREE);
        eventTestUtil.addEvent(deviceId, placeId1, LocalDateTime.now().minusDays(1), OCCUPIED);

        PlaceStatus result = getCurrentStatusForPlace(placeId1, Optional.of(LocalDateTime.now().minusDays(2)));
        assertNotNull(result);
        assertFalse(result.isOccupied());
        assertEquals(place1.getLongitude(), result.getLongitude(), LOCATION_COMPARISON_DELTA);
        assertEquals(place1.getLatitude(), result.getLatitude(), LOCATION_COMPARISON_DELTA);
    }

    @Ignore
    @Test
    public void should_throw_error_for_place_with_no_events() {
        final int placeId1 = placeTestUtil.addPlace();
        try {
            getCurrentStatusForPlace(placeId1, Optional.empty());
        } catch (HttpClientErrorException ex) {
            assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
            assertEquals(new NoCurrentStatusForPlaceException(placeId1, Optional.empty()).getMessage(), ex.getResponseBodyAsString());
        }
    }
}