package com.featureflags.segments;

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
@RequestMapping("/api/v1/segments")
public class SegmentController {

	private final SegmentService segmentService;

	private final RequestContext requestContext;

	public SegmentController(SegmentService segmentService, RequestContext requestContext) {
		this.segmentService = segmentService;
		this.requestContext = requestContext;
	}

	@GetMapping
	public Map<String, Object> listSegments(@RequestParam Map<String, String> parameters) {
		String environmentKey = SegmentValidator.validateEnvironmentQuery(parameters);

		return Map.of("data", segmentService.list(environmentKey));
	}

	@GetMapping("/{segmentKey}/members")
	public Map<String, Object> getSegmentMembers(@PathVariable String segmentKey,
			@RequestParam Map<String, String> parameters) {
		String environmentKey = SegmentValidator.validateEnvironmentQuery(parameters);

		return Map.of("data", segmentService.members(segmentKey, environmentKey));
	}

	@PostMapping
	public ResponseEntity<Map<String, Object>> createSegment(@RequestParam Map<String, String> parameters,
			@RequestBody(required = false) Map<String, Object> body) {
		String environmentKey = SegmentValidator.validateEnvironmentQuery(parameters);
		SegmentValidator.CreateInput input = SegmentValidator.validateCreate(body);

		return ResponseEntity.status(201).body(Map.of("data", segmentService.create(environmentKey, input, actor())));
	}

	@PatchMapping("/{segmentKey}")
	public Map<String, Object> updateSegment(@PathVariable String segmentKey,
			@RequestParam Map<String, String> parameters, @RequestBody(required = false) Map<String, Object> body) {
		String environmentKey = SegmentValidator.validateEnvironmentQuery(parameters);
		Map<String, Object> updates = SegmentValidator.validateUpdate(body);

		return Map.of("data", segmentService.update(segmentKey, environmentKey, updates, actor()));
	}

	@DeleteMapping("/{segmentKey}")
	public ResponseEntity<Void> deleteSegment(@PathVariable String segmentKey,
			@RequestParam Map<String, String> parameters) {
		String environmentKey = SegmentValidator.validateEnvironmentQuery(parameters);
		segmentService.remove(segmentKey, environmentKey);

		return ResponseEntity.noContent().build();
	}

	private ChangeRecorder.Actor actor() {
		return new ChangeRecorder.Actor(requestContext.profileId(), requestContext.profileName());
	}
}
