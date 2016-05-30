package fi.solita.adele;

import java.util.concurrent.atomic.AtomicInteger;

public class DeviceTestUtil {
    private static final AtomicInteger DEVICE_ID = new AtomicInteger(0);

    public static int getNewDeviceId() {
        return DEVICE_ID.incrementAndGet();
    }
}
