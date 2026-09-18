package com.featureflags.flags;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Rollout {

	public static final String DEFAULT_BUCKET_BY = "key";

	/** Rollout weights are whole percentages that must add up to this total. */
	public static final int TOTAL_WEIGHT = 100;

	/** Members are bucketed onto 0 to {@code BUCKET_SCALE - 1}. */
	public static final int BUCKET_SCALE = 100000;

	private String bucketBy = DEFAULT_BUCKET_BY;

	private List<RolloutWeight> weights = new ArrayList<>();

	public Map<String, Object> toMap() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("bucketBy", bucketBy);
		body.put("weights", weights.stream().map(RolloutWeight::toMap).toList());

		return body;
	}

	public String getBucketBy() {
		return bucketBy;
	}

	public void setBucketBy(String bucketBy) {
		this.bucketBy = bucketBy;
	}

	public List<RolloutWeight> getWeights() {
		return weights;
	}

	public void setWeights(List<RolloutWeight> weights) {
		this.weights = weights;
	}
}
