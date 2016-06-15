package fi.solita.adele.timelines.model;

import fi.solita.adele.App;
import fi.solita.adele.DeviceTestUtil;
import fi.solita.adele.EventTestUtil;
import fi.solita.adele.PlaceTestUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static fi.solita.adele.EventTestUtil.FREE;
import static fi.solita.adele.EventTestUtil.OCCUPIED;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = App.class)
@WebAppConfiguration
@IntegrationTest({"server.port:0"})
public class TimeLineControllerTest {
    private static final RestTemplate restTemplate = new RestTemplate();

    @Value("${local.server.port}")
    int port;

    private PlaceTestUtil placeTestUtil;
    private EventTestUtil eventTestUtil;

    private int placeId;
    private int deviceId;

    @Before
    public void setup() {
        placeTestUtil = new PlaceTestUtil(port);
        eventTestUtil = new EventTestUtil(port);

        placeId = placeTestUtil.addPlace();
        deviceId = DeviceTestUtil.getNewDeviceId();
    }

    @Test
    public void should_calculate_correct_time_line_with_one_usage() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.minus(10, MINUTES);
        LocalDateTime endTime = now.minus(5, MINUTES);
        eventTestUtil.addEvent(deviceId, placeId, startTime, OCCUPIED);
        eventTestUtil.addEvent(deviceId, placeId, now.minus(7, MINUTES), OCCUPIED);
        eventTestUtil.addEvent(deviceId, placeId, endTime, FREE);

        List<TimeLine> timeLines = getRanges(Optional.empty(), Optional.of(new Integer[]{placeId}), now, 60);

        assertEquals(timeLines.size(), 1);
        assertEquals(timeLines.get(0).getPlaceId(), placeId);
        assertEquals(timeLines.get(0).getRanges().size(), 1);
        assertEquals(timeLines.get(0).getRanges().get(0).getStartTime(), startTime);
        assertEquals(timeLines.get(0).getRanges().get(0).getEndTime(), endTime);
    }

    @Test
    public void should_handle_several_free_events() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.minus(10, MINUTES);
        LocalDateTime endTime = now.minus(5, MINUTES);
        eventTestUtil.addEvent(deviceId, placeId, startTime, OCCUPIED);
        eventTestUtil.addEvent(deviceId, placeId, now.minus(7, MINUTES), OCCUPIED);
        eventTestUtil.addEvent(deviceId, placeId, endTime, FREE);
        eventTestUtil.addEvent(deviceId, placeId, endTime.plusMinutes(2), FREE);
        eventTestUtil.addEvent(deviceId, placeId, endTime.plusMinutes(4), FREE);

        List<TimeLine> timeLines = getRanges(Optional.empty(), Optional.of(new Integer[]{placeId}), now, 60);

        assertEquals(timeLines.size(), 1);
        assertEquals(timeLines.get(0).getPlaceId(), placeId);
        assertEquals(timeLines.get(0).getRanges().size(), 1);
        assertEquals(timeLines.get(0).getRanges().get(0).getStartTime(), startTime);
        assertEquals(timeLines.get(0).getRanges().get(0).getEndTime(), endTime);
    }

    @Test
    public void should_calculate_correct_time_line_with_two_usages() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.minus(30, MINUTES);
        LocalDateTime endTime = now.minus(20, MINUTES);
        eventTestUtil.addEvent(deviceId, placeId, startTime, OCCUPIED);
        eventTestUtil.addEvent(deviceId, placeId, now.minus(23, MINUTES), OCCUPIED);
        eventTestUtil.addEvent(deviceId, placeId, endTime, FREE);
        LocalDateTime startTime2 = now.minus(10, MINUTES);
        LocalDateTime endTime2 = now.minus(5, MINUTES);
        eventTestUtil.addEvent(deviceId, placeId, startTime2, OCCUPIED);
        eventTestUtil.addEvent(deviceId, placeId, now.minus(7, MINUTES), OCCUPIED);
        eventTestUtil.addEvent(deviceId, placeId, endTime2, FREE);

        List<TimeLine> timeLines = getRanges(Optional.empty(), Optional.of(new Integer[]{placeId}), now, 60);

        assertEquals(timeLines.size(), 1);
        assertEquals(timeLines.get(0).getPlaceId(), placeId);
        assertEquals(timeLines.get(0).getRanges().size(), 2);
        assertEquals(timeLines.get(0).getRanges().get(0).getStartTime(), startTime);
        assertEquals(timeLines.get(0).getRanges().get(0).getEndTime(), endTime);
        assertEquals(timeLines.get(0).getRanges().get(1).getStartTime(), startTime2);
        assertEquals(timeLines.get(0).getRanges().get(1).getEndTime(), endTime2);
    }

    @Test
    public void should_calculate_correct_time_line_with_usage_not_ending_in_time_frame() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.minus(10, MINUTES);
        eventTestUtil.addEvent(deviceId, placeId, startTime, OCCUPIED);
        eventTestUtil.addEvent(deviceId, placeId, now.minus(7, MINUTES), OCCUPIED);
        eventTestUtil.addEvent(deviceId, placeId, now.minus(4, MINUTES), OCCUPIED);

        List<TimeLine> timeLines = getRanges(Optional.empty(), Optional.of(new Integer[]{placeId}), now, 60);

        assertEquals(timeLines.size(), 1);
        assertEquals(timeLines.get(0).getPlaceId(), placeId);
        assertEquals(timeLines.get(0).getRanges().size(), 1);
        assertEquals(timeLines.get(0).getRanges().get(0).getStartTime(), startTime);
        assertEquals(timeLines.get(0).getRanges().get(0).getEndTime(), now);
    }

    @Test
    public void should_calculate_correct_time_line_with_usage_starting_before_time_frame() {
        int timeFrame = 60;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = now.minus(5, MINUTES);
        eventTestUtil.addEvent(deviceId, placeId, now.minus(timeFrame + 5, MINUTES), OCCUPIED);
        eventTestUtil.addEvent(deviceId, placeId, endTime, FREE);

        List<TimeLine> timeLines = getRanges(Optional.empty(), Optional.of(new Integer[]{placeId}), now, timeFrame);

        assertEquals(timeLines.size(), 1);
        assertEquals(timeLines.get(0).getPlaceId(), placeId);
        assertEquals(timeLines.get(0).getRanges().size(), 1);
        assertEquals(timeLines.get(0).getRanges().get(0).getStartTime(), now.minus(timeFrame, MINUTES));
        assertEquals(timeLines.get(0).getRanges().get(0).getEndTime(), endTime);
    }

    @Test
    public void should_calculate_correct_time_line_with_no_usage() {
        LocalDateTime now = LocalDateTime.now();
        int timeFrame = 60;

        List<TimeLine> timeLines = getRanges(Optional.empty(), Optional.of(new Integer[]{placeId}), now, timeFrame);

        assertEquals(timeLines.size(), 1);
        assertEquals(timeLines.get(0).getPlaceId(), placeId);
        assertEquals(timeLines.get(0).getRanges().size(), 0);
    }

    @Test
    public void should_calculate_correct_time_line_with_time_frame_fitting_inside_the_event() {
        LocalDateTime now = LocalDateTime.now();
        int deviceId = DeviceTestUtil.getNewDeviceId();
        int timeFrame = 60;
        eventTestUtil.addEvent(deviceId, placeId, now.minus(timeFrame + 5, MINUTES), OCCUPIED);
        eventTestUtil.addEvent(deviceId, placeId, now.minus(timeFrame - 5, MINUTES), OCCUPIED);
        eventTestUtil.addEvent(deviceId, placeId, now.minus(timeFrame - 10, MINUTES), OCCUPIED);
        eventTestUtil.addEvent(deviceId, placeId, now.minus(timeFrame - 15, MINUTES), OCCUPIED);

        List<TimeLine> timeLines = getRanges(Optional.empty(), Optional.of(new Integer[]{placeId}), now, timeFrame);

        assertEquals(timeLines.size(), 1);
        assertEquals(timeLines.get(0).getPlaceId(), placeId);
        assertEquals(timeLines.get(0).getRanges().size(), 1);
        assertEquals(timeLines.get(0).getRanges().get(0).getStartTime(), now.minus(timeFrame, MINUTES));
        assertEquals(timeLines.get(0).getRanges().get(0).getEndTime(), now);
    }

    private List<TimeLine> getRanges(Optional<Object> device_id, Optional<Integer[]> place_id, LocalDateTime ending, int minutes) {
        UriComponentsBuilder uri = UriComponentsBuilder.fromUriString(url("/v1/query/timeline"));

        uri.queryParam("ending", ending);
        uri.queryParam("minutes", minutes);
        device_id.map(Arrays::asList).orElse(new ArrayList<>()).forEach(v -> uri.queryParam("device_id", v));
        place_id.map(Arrays::asList).orElse(new ArrayList<>()).forEach(v -> uri.queryParam("place_id", v));

        ResponseEntity<List<TimeLine>> rateResponse = restTemplate.exchange(uri.build().toUriString(),
                                                                            HttpMethod.GET,
                                                                            null,
                                                                            new ParameterizedTypeReference<List<TimeLine>>() {});
        return rateResponse.getBody();
    }

    private String url(String suffix) {
        return "http://localhost:" + port + suffix;
    }

}