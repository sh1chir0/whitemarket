package ua.sh1chiro.Bot.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Created by Sh1chiro on 18.06.2025.
 * <p>
 * When I wrote this code, only god and
 * I knew how it worked.
 * Now, only god knows it!
 *
 * @author Sh1chiro
 */

@RestController
@RequestMapping("/api")
public class DeviceInfoController {

    private final Path logDirectory = Paths.get("device-logs");

    @PostMapping("/device-info")
    public ResponseEntity<String> receiveDeviceInfo(@RequestBody Map<String, Object> info) {
        try {
            if (!Files.exists(logDirectory)) {
                Files.createDirectories(logDirectory);
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String filename = "device-info_" + timestamp + ".txt";
            Path filePath = logDirectory.resolve(filename);

            String json = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(info);

            Files.write(filePath, json.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);

            return ResponseEntity.ok("я" + filename);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("я.");
        }
    }
}
