package ua.sh1chiro.Bot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.sh1chiro.Bot.models.History;
import ua.sh1chiro.Bot.repositories.HistoryRepository;

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

@Service
@Slf4j
@RequiredArgsConstructor
public class HistoryService {
    private final HistoryRepository historyRepository;

    public History save(History history){
        return historyRepository.save(history);
    }

    public List<History> getALl(){
        return historyRepository.findAll();
    }

    public History getById(Long id){
        return historyRepository.findById(id).orElse(null);
    }

    public void delete(History history){
        historyRepository.delete(history);
    }
}
