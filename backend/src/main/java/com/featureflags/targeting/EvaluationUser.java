package com.featureflags.targeting;

import java.util.Map;

/**
 * The audience member an evaluation is performed for, reduced to the key and
 * the flat attribute map that clauses are matched against.
 */
public record EvaluationUser(String key, Map<String, Object> attributes) {

	public Object attribute(String name) {
		return attributes.get(name);
	}
}
