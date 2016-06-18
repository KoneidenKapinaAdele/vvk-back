package fi.solita.adele.utils;

public class DoorStatusResolver {

    public static final int DOOR_OPEN = 0;

    public static boolean isClosed(double value) {
        return value > DOOR_OPEN;
    }
}
