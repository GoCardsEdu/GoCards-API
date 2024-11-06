package pl.gocards.api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping(value = {"/", "/swagger-ui", "/swagger-ui/" })
    public String redirectToSwaggerUi() {
        return "redirect:/swagger-ui/index.html";
    }
}
