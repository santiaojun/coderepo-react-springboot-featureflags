package com.featureflags.flags;

import java.util.LinkedHashMap;
import java.util.Map;

public class RolloutWeight {

	private String variationId;

	private int weight;

	public Map<String, Object> toMap() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("variationId", variationId);
		body.put("weight", weight);

		return body;
	}

	public String getVariationId() {
		return variationId;
	}

	public void setVariationId(String variationId) {
		this.variationId = variationId;
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}
}
