package fi.solita.adele.timelines.model;

import fi.solita.adele.timelines.model.Range;

import java.util.List;

public class TimeLine {

    private int placeId;

    private List<Range> ranges;

    public TimeLine(){

    }

    public TimeLine(List<Range> ranges) {
        this.ranges = ranges;
    }

    public TimeLine(int place_id, List<Range> ranges){
        this.placeId = place_id;
        this.ranges = ranges;
    }

    public List<Range> getRanges() {
        return ranges;
    }

    public void setRanges(List<Range> ranges) {
        this.ranges = ranges;
    }

    public int getPlaceId() {
        return placeId;
    }

    public void setPlaceId(int placeId) {
        this.placeId = placeId;
    }
}
