package com.featureflags.audience;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audience")
public class AudienceController {

	private final AudienceService audienceService;

	public AudienceController(AudienceService audienceService) {
		this.audienceService = audienceService;
	}

	@GetMapping("/users")
	public Map<String, Object> listUsers(@RequestParam Map<String, String> parameters) {
		return Map.of("data", audienceService.list(AudienceValidator.validateList(parameters)));
	}

	@GetMapping("/users/{userKey}/flags")
	public Map<String, Object> getUserFlags(@PathVariable String userKey,
			@RequestParam Map<String, String> parameters) {
		String environmentKey = AudienceValidator.validateEnvironmentQuery(parameters);

		return Map.of("data", audienceService.flagsForUser(userKey, environmentKey));
	}
}
