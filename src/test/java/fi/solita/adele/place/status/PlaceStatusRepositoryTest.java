package fi.solita.adele.place.status;

import fi.solita.adele.App;
import fi.solita.adele.event.EventRepository;
import fi.solita.adele.event.EventType;
import fi.solita.adele.place.CreatePlaceCommand;
import fi.solita.adele.place.PlaceRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static fi.solita.adele.RepositoryTestUtil.generateEvent;
import static fi.solita.adele.RepositoryTestUtil.generatePlace;
import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = App.class)
@WebAppConfiguration
public class PlaceStatusRepositoryTest {

    @Resource PlaceStatusRepository placeStatusRepository;
    @Resource EventRepository eventRepository;
    @Resource PlaceRepository placeRepository;

    private static final int DEVICE_ID = 1;
    private static final int PLACE_ID = 1;
    private static final EventType EVENT_TYPE = EventType.movement;

    private static final double MOVING = 1.0;
    private static final double STILL = 0.0;
    private static final LocalDateTime START_TIME = LocalDateTime.now();

    @Before public void setup() {
        CreatePlaceCommand place = generatePlace();
        placeRepository.addPlace(place);
    }

    @Test
    public void should_get_status_for_places_based_on_latest_movement_event() throws Exception {
        eventRepository.addEvent(generateEvent(STILL, DEVICE_ID, PLACE_ID, START_TIME, EVENT_TYPE));
        eventRepository.addEvent(generateEvent(MOVING, DEVICE_ID, PLACE_ID, START_TIME.plusSeconds(10), EVENT_TYPE));
        eventRepository.addEvent(generateEvent(STILL, DEVICE_ID, PLACE_ID, START_TIME.plusSeconds(20), EVENT_TYPE));
        eventRepository.addEvent(generateEvent(MOVING, DEVICE_ID, PLACE_ID, START_TIME.plusSeconds(30), EVENT_TYPE));
        int interval = 5;

        Map<LocalDateTime, PlaceStatus> timeStatusMap = placeStatusRepository.geStatusForPlaces(START_TIME,
                                                                                                      START_TIME.plusSeconds(30),
                                                                                                      new Integer[]{PLACE_ID},
                                                                                                      interval,
                                                                                                      ChronoUnit.SECONDS);

        assertEquals(timeStatusMap.size(), 6);
        assertEquals(timeStatusMap.get(START_TIME.plusSeconds(5)).isOccupied(), false);
        assertEquals(timeStatusMap.get(START_TIME.plusSeconds(10)).isOccupied(), true);
        assertEquals(timeStatusMap.get(START_TIME.plusSeconds(15)).isOccupied(), true);
        assertEquals(timeStatusMap.get(START_TIME.plusSeconds(25)).isOccupied(), false);
    }
}