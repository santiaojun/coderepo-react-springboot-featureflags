package com.featureflags.flags;

import java.util.LinkedHashMap;
import java.util.Map;

public class Serve {

	public static final String VARIATION = "variation";

	public static final String ROLLOUT = "rollout";

	private String kind = VARIATION;

	private String variationId;

	private Rollout rollout;

	public static Serve ofVariation(String variationId) {
		Serve serve = new Serve();
		serve.setKind(VARIATION);
		serve.setVariationId(variationId);

		return serve;
	}

	public static Serve ofRollout(Rollout rollout) {
		Serve serve = new Serve();
		serve.setKind(ROLLOUT);
		serve.setRollout(rollout);

		return serve;
	}

	public boolean isRollout() {
		return ROLLOUT.equals(kind) && rollout != null && !rollout.getWeights().isEmpty();
	}

	public Map<String, Object> toMap() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("kind", kind);
		body.put("variationId", variationId);
		body.put("rollout", rollout == null ? null : rollout.toMap());

		return body;
	}

	public String getKind() {
		return kind;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

	public String getVariationId() {
		return variationId;
	}

	public void setVariationId(String variationId) {
		this.variationId = variationId;
	}

	public Rollout getRollout() {
		return rollout;
	}

	public void setRollout(Rollout rollout) {
		this.rollout = rollout;
	}
}
