package com.example.helloworld.api.v1;

import com.example.helloworld.api.dto.GreetingResponse;
import com.example.helloworld.api.dto.GreetingTemplateResponse;
import com.example.helloworld.service.GreetingResult;
import com.example.helloworld.service.GreetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Clock;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
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
    private final Clock clock;
    private final String serviceVersion;

    public GreetingController(GreetingService greetingService,
                              Clock clock,
                              @Value("${info.app.version:unknown}") String serviceVersion) {
        this.greetingService = greetingService;
        this.clock = clock;
        this.serviceVersion = serviceVersion;
    }

    @GetMapping("/hello")
    @Operation(summary = "Say hello", description = "Returns a localized greeting, timestamp and service version")
    @ApiResponse(responseCode = "200", description = "Greeting produced")
    @ApiResponse(responseCode = "400", description = "Invalid name or locale")
    @ApiResponse(responseCode = "404", description = "Unknown locale")
    public GreetingResponse hello(
            @RequestParam(defaultValue = "World")
            @Size(max = 100) @Pattern(regexp = "[\\p{L} .'-]+", message = "name contains invalid characters")
            String name,
            @RequestParam(defaultValue = "en")
            @Pattern(regexp = "[A-Za-z]{2,3}(-[A-Za-z]{2,4})?", message = "locale must be an ISO language tag")
            String locale) {
        GreetingResult result = greetingService.greet(name, locale);
        return new GreetingResponse(result.message(), result.quote(), clock.instant(), serviceVersion);
    }

    @GetMapping
    @Operation(summary = "List greeting templates", description = "Requires authentication")
    @ApiResponse(responseCode = "200", description = "Templates listed")
    public List<GreetingTemplateResponse> list() {
        return greetingService.findAll().stream()
                .map(greeting -> new GreetingTemplateResponse(greeting.getLocale(), greeting.getTemplate()))
                .toList();
    }
}
