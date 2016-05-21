package fi.solita.adele.event;

import fi.solita.adele.App;
import fi.solita.adele.place.Place;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = App.class)
@WebAppConfiguration
@IntegrationTest({"server.port:0"})
public class EventControllerTest {
    @Value("${local.server.port}")
    int port;

    RestTemplate restTemplate = new RestTemplate();

    private String url(String suffix) {
        return "http://localhost:" + port + suffix;
    }

    private List<Event> getAllEvents() {
        return Arrays.asList(restTemplate.getForObject(url("/v1/event"), Event[].class));
    }

    private int addPlace(Place place) {
        ResponseEntity<Integer> result = restTemplate.postForEntity(url("/v1/place"), place, Integer.class);
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        return result.getBody();
    }

    private void addEvent(Event event) {
        ResponseEntity<Integer> result = restTemplate.postForEntity(url("/v1/event"), event, Integer.class);
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
    }

    private boolean eventsEqualIgnoreId(Event e1, Event e2) {
        return Objects.equals(e1.getDevice_id(), e2.getDevice_id()) &&
                Objects.equals(e1.getPlace_id(), e2.getPlace_id()) &&
                Objects.equals(e1.getTime(), e2.getTime()) &&
                Objects.equals(e1.getType(), e2.getType()) &&
                Objects.equals(e1.getValue(), e2.getValue());
    }

    @Test
    public void should_add_new_event() {
        Place place = new Place();
        place.setName("Paikka 2");
        place.setLatitude(875.99856);
        place.setLongitude(984.98449);
        int placeId = addPlace(place);

        Event event = new Event();
        event.setDevice_id(100);
        event.setPlace_id(placeId);
        event.setTime(LocalDateTime.now());
        event.setType("liiketunnistin");
        event.setValue(1.0);

        addEvent(event);
        assertTrue(getAllEvents().stream().anyMatch(e -> eventsEqualIgnoreId(e, event)));

    }
}