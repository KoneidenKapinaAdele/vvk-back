package fi.solita.adele.device;

import com.google.common.collect.ImmutableList;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/v1/device")
public class DeviceStatusController {

    @Resource private DeviceRepository repo;

    @RequestMapping(value = "/status", method = RequestMethod.GET)
    @ResponseBody
    public List<DeviceStatus> getStatusForAllDevices() {
        return repo.getDeviceStatus();
    }
}
