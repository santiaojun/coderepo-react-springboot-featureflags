package com.featureflags.targeting;

import java.util.LinkedHashMap;
import java.util.Map;

public record EvaluationResult(String variationId, String variationName, Object value, String reasonKind,
		String ruleId, String ruleDescription, String segmentKey, Integer bucket) {

	public static final String OFF = "off";

	public static final String TARGET_MATCH = "targetMatch";

	public static final String RULE_MATCH = "ruleMatch";

	public static final String SEGMENT_MATCH = "segmentMatch";

	public static final String ROLLOUT = "rollout";

	public static final String FALLTHROUGH = "fallthrough";

	public Map<String, Object> toMap() {
		Map<String, Object> reason = new LinkedHashMap<>();
		reason.put("kind", reasonKind);
		reason.put("ruleId", ruleId);
		reason.put("ruleDescription", ruleDescription);
		reason.put("segmentKey", segmentKey);
		reason.put("bucket", bucket);
		reason.put("percentage", bucket == null ? null : bucket / 1000.0);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("variationId", variationId);
		body.put("variationName", variationName);
		body.put("value", value);
		body.put("reason", reason);

		return body;
	}
}
