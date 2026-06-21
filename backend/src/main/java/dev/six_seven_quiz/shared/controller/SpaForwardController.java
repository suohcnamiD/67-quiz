package dev.six_seven_quiz.shared.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaForwardController {
    // Match any path that doesn't contain a dot (i.e. not a static asset) and isn't under /api
    @RequestMapping(value = { "/{path:[^.]*}", "/{path1:[^.]*}/{path2:[^.]*}", "/{path1:[^.]*}/{path2:[^.]*}/{path3:[^.]*}" })
    public String forward() {
        return "forward:/index.html";
    }
}
