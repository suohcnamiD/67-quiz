package dev.six_seven_quiz.search.controller;

import dev.six_seven_quiz.search.dto.SearchResponseDto;
import dev.six_seven_quiz.search.service.SearchService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public SearchResponseDto search(
            @RequestParam(name = "q", required = false) String query,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return searchService.search(query, userDetails);
    }
}
