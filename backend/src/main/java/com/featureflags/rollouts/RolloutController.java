package com.featureflags.rollouts;

import java.util.Map;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.featureflags.history.ChangeRecorder;
import com.featureflags.shared.RequestContext;

@RestController
@RequestMapping("/api/v1/flags/{flagKey}/environments/{environmentKey}/rollout")
public class RolloutController {

	private final RolloutService rolloutService;

	private final RequestContext requestContext;

	public RolloutController(RolloutService rolloutService, RequestContext requestContext) {
		this.rolloutService = rolloutService;
		this.requestContext = requestContext;
	}

	@PatchMapping
	public Map<String, Object> updateRollout(@PathVariable String flagKey, @PathVariable String environmentKey,
			@RequestBody(required = false) Map<String, Object> body) {
		RolloutValidator.RolloutInput input = RolloutValidator.validateUpdate(body);

		return Map.of("data", rolloutService.update(flagKey, environmentKey, input, actor()));
	}

	@PostMapping("/preview")
	public Map<String, Object> previewRollout(@PathVariable String flagKey, @PathVariable String environmentKey,
			@RequestBody(required = false) Map<String, Object> body) {
		RolloutValidator.PreviewInput input = RolloutValidator.validatePreview(body);

		return Map.of("data", rolloutService.preview(flagKey, environmentKey, input));
	}

	private ChangeRecorder.Actor actor() {
		return new ChangeRecorder.Actor(requestContext.profileId(), requestContext.profileName());
	}
}
