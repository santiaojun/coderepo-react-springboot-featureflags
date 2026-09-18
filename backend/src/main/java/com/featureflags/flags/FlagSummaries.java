package com.featureflags.flags;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Human-readable descriptions of flag state. Used for change-record summaries
 * so the audit trail reads as sentences rather than identifiers.
 */
public final class FlagSummaries {

	private static final String UNKNOWN_VARIATION = "an unknown variation";

	public static String variationName(Flag flag, String variationId) {
		Variation variation = flag.findVariation(variationId);

		return variation == null ? UNKNOWN_VARIATION : variation.getName();
	}

	public static String describeServe(Flag flag, Serve serve) {
		if (serve == null) {
			return UNKNOWN_VARIATION;
		}

		if (!serve.isRollout()) {
			return variationName(flag, serve.getVariationId());
		}

		return describeRollout(flag, serve.getRollout());
	}

	public static String describeRollout(Flag flag, Rollout rollout) {
		if (rollout == null || rollout.getWeights().isEmpty()) {
			return "no rollout";
		}

		return rollout.getWeights().stream().filter(weight -> weight.getWeight() > 0)
				.map(weight -> weight.getWeight() + "% " + variationName(flag, weight.getVariationId()))
				.collect(Collectors.joining(", "));
	}

	public static String describeTargets(Flag flag, List<FlagTarget> targets) {
		List<String> parts = new ArrayList<>();

		for (FlagTarget target : targets) {
			if (!target.getUserKeys().isEmpty()) {
				parts.add(target.getUserKeys().size() + " to " + variationName(flag, target.getVariationId()));
			}
		}

		return parts.isEmpty() ? "no individual targets" : String.join(", ", parts);
	}

	public static String describeRuleCount(int count) {
		return count == 1 ? "1 rule" : count + " rules";
	}

	private FlagSummaries() {
	}
}
