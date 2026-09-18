package com.featureflags.segments;

import java.util.Map;

import com.featureflags.flags.ClauseMatcher;

/**
 * Decides whether an audience member belongs to a segment. Explicit includes
 * and excludes win over rules, and excludes win over includes.
 */
public final class SegmentMatcher {

	public static boolean matches(Segment segment, String userKey, Map<String, Object> attributes) {
		if (segment.getExcludedUserKeys().contains(userKey)) {
			return false;
		}

		if (segment.getIncludedUserKeys().contains(userKey)) {
			return true;
		}

		for (SegmentRule rule : segment.getRules()) {
			if (!rule.getClauses().isEmpty() && ClauseMatcher.matchesAll(rule.getClauses(), attributes)) {
				return true;
			}
		}

		return false;
	}

	private SegmentMatcher() {
	}
}
