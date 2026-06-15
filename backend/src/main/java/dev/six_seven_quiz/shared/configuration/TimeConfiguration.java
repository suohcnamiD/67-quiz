package dev.six_seven_quiz.shared.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;

@Configuration
public class TimeConfiguration {

    @Bean
    public ZoneId timeZoneId() {
        return ZoneId.of("Europe/Berlin");
    }
}
