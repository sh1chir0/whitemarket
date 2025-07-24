package ua.sh1chiro.Bot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.sh1chiro.Bot.models.Target;
import ua.sh1chiro.Bot.repositories.TargetRepository;

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
public class TargetService {
    private final TargetRepository targetRepository;

    public Target save(Target target){
        return targetRepository.save(target);
    }

    public void delete(Target target){
        targetRepository.delete(target);
    }
    public void deleteById(Long id){
        Target target = targetRepository.findById(id).orElse(null);

        if(target != null)
            targetRepository.delete(target);
    }

    public List<Target> getAllTargets(){
        return targetRepository.findAll();
    }

    public Target getById(Long id){
        return targetRepository.findById(id).orElse(null);
    }

    public List<Target> getByTargetId(String targetId){
        return targetRepository.getByTargetId(targetId);
    }

    public void deleteByTargetId(String targetId){
        targetRepository.deleteByTargetId(targetId);
    }

    public List<Target> getTargetsByName(String name){
        return targetRepository.findTargetsByName(name);
    }

    public Target getByName(String name){
        return targetRepository.findByName(name);
    }
}
