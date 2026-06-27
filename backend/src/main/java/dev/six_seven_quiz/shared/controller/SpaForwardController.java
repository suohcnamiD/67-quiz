package dev.six_seven_quiz.shared.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Controller
public class SpaForwardController {

    private static final String DEV_FALLBACK_HTML = """
            <!doctype html><html><head><meta charset="utf-8"><title>67 Quiz — backend</title>
            <style>body{font-family:system-ui;margin:4rem auto;max-width:40rem;color:#222}</style>
            </head><body>
            <h1>67 Quiz backend</h1>
            <p>This is the API server. The frontend isn't bundled in this build.</p>
            <p>Run the Vite dev server (<code>cd frontend &amp;&amp; npm run dev</code>)
            and open <a href="http://localhost:5173/">http://localhost:5173/</a>.</p>
            </body></html>
            """;

    @RequestMapping({
            "/{path:[^.]*}",
            "/{p1:[^.]*}/{p2:[^.]*}",
            "/{p1:[^.]*}/{p2:[^.]*}/{p3:[^.]*}",
            "/{p1:[^.]*}/{p2:[^.]*}/{p3:[^.]*}/{p4:[^.]*}"
    })
    public ResponseEntity<String> serveSpa() {
        Resource index = new ClassPathResource("static/index.html");
        if (index.exists()) {
            try (var in = index.getInputStream()) {
                String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .body(body);
            } catch (IOException e) {
                // fall through to dev fallback
            }
        }
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(DEV_FALLBACK_HTML);
    }
}
