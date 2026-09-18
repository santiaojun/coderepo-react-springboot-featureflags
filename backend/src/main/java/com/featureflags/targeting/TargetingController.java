package com.featureflags.targeting;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.featureflags.history.ChangeRecorder;
import com.featureflags.shared.RequestContext;

@RestController
@RequestMapping("/api/v1/flags/{flagKey}/environments/{environmentKey}")
public class TargetingController {

	private final TargetingService targetingService;

	private final RequestContext requestContext;

	public TargetingController(TargetingService targetingService, RequestContext requestContext) {
		this.targetingService = targetingService;
		this.requestContext = requestContext;
	}

	@PutMapping("/targeting")
	public Map<String, Object> updateTargeting(@PathVariable String flagKey, @PathVariable String environmentKey,
			@RequestBody(required = false) Map<String, Object> body) {
		TargetingValidator.TargetingInput input = TargetingValidator.validateTargeting(body);

		return Map.of("data", targetingService.updateTargeting(flagKey, environmentKey, input, actor()));
	}

	@PostMapping("/evaluate")
	public Map<String, Object> evaluate(@PathVariable String flagKey, @PathVariable String environmentKey,
			@RequestBody(required = false) Map<String, Object> body) {
		TargetingValidator.EvaluateInput input = TargetingValidator.validateEvaluate(body);

		return Map.of("data", targetingService.evaluate(flagKey, environmentKey, input));
	}

	@GetMapping("/distribution")
	public Map<String, Object> getDistribution(@PathVariable String flagKey, @PathVariable String environmentKey) {
		return Map.of("data", targetingService.distributionFor(flagKey, environmentKey));
	}

	@PostMapping("/restore")
	public Map<String, Object> restore(@PathVariable String flagKey, @PathVariable String environmentKey,
			@RequestBody(required = false) Map<String, Object> body) {
		TargetingValidator.RestoreInput input = TargetingValidator.validateRestore(body);

		return Map.of("data", targetingService.restore(flagKey, environmentKey, input, actor()));
	}

	private ChangeRecorder.Actor actor() {
		return new ChangeRecorder.Actor(requestContext.profileId(), requestContext.profileName());
	}
}
