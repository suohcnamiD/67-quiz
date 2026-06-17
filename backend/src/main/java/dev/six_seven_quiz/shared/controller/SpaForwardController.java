package dev.six_seven_quiz.shared.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaForwardController {
    @RequestMapping(value = "/{path:[^.]*}")
    public String forward() {
        return "forward:/index.html";
    }
}
