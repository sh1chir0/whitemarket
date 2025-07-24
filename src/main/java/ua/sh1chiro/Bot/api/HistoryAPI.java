package ua.sh1chiro.Bot.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.sh1chiro.Bot.models.History;
import ua.sh1chiro.Bot.services.HistoryService;

import java.util.List;

/**
 * Created by Sh1chiro on 13.05.2025.
 * <p>
 * When I wrote this code, only god and
 * I knew how it worked.
 * Now, only god knows it!
 *
 * @author Sh1chiro
 */

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryAPI {
    private final HistoryService historyService;

    @GetMapping("/get-all")
    public List<History> getAll(){
        return historyService.getALl();
    }

    @GetMapping("/delete/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id){
        History history = historyService.getById(id);
        if(history != null)
            historyService.delete(history);

        return ResponseEntity.ok("ok");
    }

}
