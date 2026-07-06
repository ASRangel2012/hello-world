package com.example.helloworld.api.v1;

import com.example.helloworld.api.dto.GreetingResponse;
import com.example.helloworld.domain.Greeting;
import com.example.helloworld.service.GreetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/greetings", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@Tag(name = "Greetings", description = "Greeting operations")
public class GreetingController {

    private final GreetingService greetingService;

    public GreetingController(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    @GetMapping("/hello")
    @Operation(summary = "Say hello", description = "Returns a localized greeting, timestamp and service version")
    @ApiResponse(responseCode = "200", description = "Greeting produced")
    @ApiResponse(responseCode = "404", description = "Unknown locale")
    public GreetingResponse hello(
            @RequestParam(defaultValue = "World")
            @Size(max = 100) @Pattern(regexp = "[\\p{L} .'-]+", message = "name contains invalid characters")
            String name,
            @RequestParam(defaultValue = "en")
            @Size(min = 2, max = 8)
            String locale) {
        return greetingService.greet(name, locale);
    }

    @GetMapping
    @Operation(summary = "List greeting templates", description = "Requires authentication")
    public List<Greeting> list() {
        return greetingService.findAll();
    }
}
