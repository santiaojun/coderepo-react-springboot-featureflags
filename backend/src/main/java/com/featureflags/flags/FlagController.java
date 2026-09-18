package com.featureflags.flags;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.featureflags.history.ChangeRecorder;
import com.featureflags.shared.RequestContext;

@RestController
@RequestMapping("/api/v1/flags")
public class FlagController {

	private final FlagService flagService;

	private final RequestContext requestContext;

	public FlagController(FlagService flagService, RequestContext requestContext) {
		this.flagService = flagService;
		this.requestContext = requestContext;
	}

	@GetMapping
	public Map<String, Object> listFlags(@RequestParam Map<String, String> parameters) {
		return Map.of("data", flagService.list(FlagValidator.validateList(parameters)));
	}

	@GetMapping("/{flagKey}")
	public Map<String, Object> getFlag(@PathVariable String flagKey,
			@RequestParam Map<String, String> parameters) {
		String environmentKey = FlagValidator.validateEnvironmentQuery(parameters);

		return Map.of("data", flagService.getByKey(flagKey, environmentKey));
	}

	@PostMapping
	public ResponseEntity<Map<String, Object>> createFlag(@RequestBody(required = false) Map<String, Object> body) {
		Map<String, Object> flag = flagService.create(FlagValidator.validateCreate(body), actor());

		return ResponseEntity.status(201).body(Map.of("data", flag));
	}

	@PatchMapping("/{flagKey}")
	public Map<String, Object> updateFlag(@PathVariable String flagKey,
			@RequestBody(required = false) Map<String, Object> body) {
		return Map.of("data", flagService.update(flagKey, FlagValidator.validateUpdate(body), actor()));
	}

	@PostMapping("/{flagKey}/retire")
	public Map<String, Object> retireFlag(@PathVariable String flagKey) {
		return Map.of("data", flagService.retire(flagKey, actor()));
	}

	@PostMapping("/{flagKey}/restore")
	public Map<String, Object> restoreFlag(@PathVariable String flagKey) {
		return Map.of("data", flagService.restore(flagKey, actor()));
	}

	@DeleteMapping("/{flagKey}")
	public ResponseEntity<Void> deleteFlag(@PathVariable String flagKey) {
		flagService.remove(flagKey);

		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{flagKey}/environments/{environmentKey}/toggle")
	public Map<String, Object> toggleFlag(@PathVariable String flagKey, @PathVariable String environmentKey,
			@RequestBody(required = false) Map<String, Object> body) {
		FlagValidator.ToggleInput input = FlagValidator.validateToggle(body);

		return Map.of("data", flagService.toggle(flagKey, environmentKey, input, actor()));
	}

	private ChangeRecorder.Actor actor() {
		return new ChangeRecorder.Actor(requestContext.profileId(), requestContext.profileName());
	}
}
