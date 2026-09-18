package com.featureflags.environments;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/environments")
public class EnvironmentController {

	private final EnvironmentService environmentService;

	public EnvironmentController(EnvironmentService environmentService) {
		this.environmentService = environmentService;
	}

	@GetMapping
	public Map<String, Object> listEnvironments() {
		List<Environment> environments = environmentService.list();

		return Map.of("data", environments.stream().map(Environment::toMap).toList());
	}

	@GetMapping("/{environmentKey}/summary")
	public Map<String, Object> getSummary(@PathVariable String environmentKey) {
		return Map.of("data", environmentService.summary(environmentKey));
	}
}
