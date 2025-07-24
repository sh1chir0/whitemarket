package ua.sh1chiro.Bot.conrtollers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Created by Sh1chiro on 30.04.2025.
 * <p>
 * When I wrote this code, only god and
 * I knew how it worked.
 * Now, only god knows it!
 *
 * @author Sh1chiro
 */

@Controller
public class IndexController {

    @GetMapping("/")
    public String index(){
        return "index";
    }
}
