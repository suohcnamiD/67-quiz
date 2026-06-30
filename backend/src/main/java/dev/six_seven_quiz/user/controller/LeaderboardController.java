package dev.six_seven_quiz.user.controller;

import dev.six_seven_quiz.user.dto.LeaderboardPageDto;
import dev.six_seven_quiz.user.service.LeaderboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/leaderboards")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping("/players")
    public LeaderboardPageDto topPlayers(
            @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return leaderboardService.topPlayers(page, userDetails);
    }

    @GetMapping("/authors")
    public LeaderboardPageDto topAuthors(
            @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return leaderboardService.topAuthors(page, userDetails);
    }
}
