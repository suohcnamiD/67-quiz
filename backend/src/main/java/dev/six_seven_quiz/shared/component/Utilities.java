package dev.six_seven_quiz.shared.component;

import dev.six_seven_quiz.shared.dto.Failure;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Component
public class Utilities {

    private final ObjectMapper objectMapper;

    public Utilities(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void writeResponseEntityToResponse(HttpServletResponse response, ResponseEntity<?> responseEntity) throws IOException {

        response.setStatus(responseEntity.getStatusCode().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String responseBodyString;
        try {
            responseBodyString = objectMapper.writeValueAsString(responseEntity.getBody());
        } catch (JacksonException e) {
            response.setStatus(INTERNAL_SERVER_ERROR.value());
            responseBodyString = objectMapper.writeValueAsString(Failure.status(INTERNAL_SERVER_ERROR));
        }
        response.getWriter().write(responseBodyString);
    }
}

