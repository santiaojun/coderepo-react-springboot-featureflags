package com.featureflags.flags;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FlagTarget {

	private String variationId;

	private List<String> userKeys = new ArrayList<>();

	public Map<String, Object> toMap() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("variationId", variationId);
		body.put("userKeys", new ArrayList<>(userKeys));

		return body;
	}

	public String getVariationId() {
		return variationId;
	}

	public void setVariationId(String variationId) {
		this.variationId = variationId;
	}

	public List<String> getUserKeys() {
		return userKeys;
	}

	public void setUserKeys(List<String> userKeys) {
		this.userKeys = userKeys;
	}
}
