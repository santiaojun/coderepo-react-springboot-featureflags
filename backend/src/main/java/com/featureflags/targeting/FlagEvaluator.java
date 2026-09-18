package com.featureflags.targeting;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import com.featureflags.flags.ClauseMatcher;
import com.featureflags.flags.Flag;
import com.featureflags.flags.FlagConfiguration;
import com.featureflags.flags.FlagTarget;
import com.featureflags.flags.Rollout;
import com.featureflags.flags.RolloutWeight;
import com.featureflags.flags.Serve;
import com.featureflags.flags.TargetRule;
import com.featureflags.flags.Variation;
import com.featureflags.segments.Segment;
import com.featureflags.segments.SegmentMatcher;

/**
 * Decides which variation an audience member receives, and why.
 *
 * <p>Percentage rollouts are bucketed deterministically: the same member always
 * lands on the same point of the 0 to 99999 scale for a given flag and
 * environment, so raising a rollout only ever adds members to the treatment.
 */
public final class FlagEvaluator {

	private static final String HASH_ALGORITHM = "SHA-1";

	private static final int HASH_BYTES = 4;

	public static EvaluationResult evaluate(Flag flag, FlagConfiguration configuration, EvaluationUser user,
			Map<String, Segment> segmentsById) {
		if (!configuration.isEnabled()) {
			return result(flag, configuration.getOffVariationId(), EvaluationResult.OFF, null, null, null, null);
		}

		for (FlagTarget target : configuration.getTargets()) {
			if (target.getUserKeys().contains(user.key())) {
				return result(flag, target.getVariationId(), EvaluationResult.TARGET_MATCH, null, null, null, null);
			}
		}

		for (TargetRule rule : configuration.getRules()) {
			Segment matchedSegment = matchingSegment(rule, user, segmentsById);
			boolean segmentsSatisfied = rule.getSegmentIds().isEmpty() || matchedSegment != null;

			if (!segmentsSatisfied || !ClauseMatcher.matchesAll(rule.getClauses(), user.attributes())) {
				continue;
			}

			String kind = matchedSegment == null ? EvaluationResult.RULE_MATCH : EvaluationResult.SEGMENT_MATCH;
			String segmentKey = matchedSegment == null ? null : matchedSegment.getKey();

			return serve(flag, configuration, rule.getServe(), user, kind, rule.getId(), rule.getDescription(),
					segmentKey);
		}

		return serve(flag, configuration, configuration.getFallthrough(), user, EvaluationResult.FALLTHROUGH, null,
				null, null);
	}

	public static int bucket(String flagKey, String environmentKey, String bucketValue) {
		byte[] digest = digest(flagKey + ":" + environmentKey + ":" + bucketValue);
		long value = 0;

		for (int index = 0; index < HASH_BYTES; index += 1) {
			value = (value << 8) | (digest[index] & 0xFF);
		}

		return (int) (value % Rollout.BUCKET_SCALE);
	}

	private static EvaluationResult serve(Flag flag, FlagConfiguration configuration, Serve serve,
			EvaluationUser user, String kind, String ruleId, String ruleDescription, String segmentKey) {
		if (serve == null) {
			return result(flag, configuration.getOffVariationId(), kind, ruleId, ruleDescription, segmentKey, null);
		}

		if (!serve.isRollout()) {
			return result(flag, serve.getVariationId(), kind, ruleId, ruleDescription, segmentKey, null);
		}

		Rollout rollout = serve.getRollout();
		int bucket = bucket(configuration.getFlagKey(), configuration.getEnvironmentKey(),
				bucketValue(rollout, user));
		String reasonKind = EvaluationResult.FALLTHROUGH.equals(kind) ? EvaluationResult.ROLLOUT : kind;

		return result(flag, variationForBucket(rollout, bucket), reasonKind, ruleId, ruleDescription, segmentKey,
				bucket);
	}

	private static String variationForBucket(Rollout rollout, int bucket) {
		long threshold = 0;
		String last = null;

		for (RolloutWeight weight : rollout.getWeights()) {
			threshold += (long) weight.getWeight() * Rollout.BUCKET_SCALE / Rollout.TOTAL_WEIGHT;
			last = weight.getVariationId();

			if (bucket < threshold) {
				return weight.getVariationId();
			}
		}

		return last;
	}

	private static String bucketValue(Rollout rollout, EvaluationUser user) {
		String bucketBy = rollout.getBucketBy();

		if (bucketBy == null || Rollout.DEFAULT_BUCKET_BY.equals(bucketBy)) {
			return user.key();
		}

		Object value = user.attribute(bucketBy);

		return value == null ? user.key() : String.valueOf(value);
	}

	private static Segment matchingSegment(TargetRule rule, EvaluationUser user, Map<String, Segment> segmentsById) {
		for (String segmentId : rule.getSegmentIds()) {
			Segment segment = segmentsById.get(segmentId);

			if (segment != null && SegmentMatcher.matches(segment, user.key(), user.attributes())) {
				return segment;
			}
		}

		return null;
	}

	private static byte[] digest(String seed) {
		try {
			return MessageDigest.getInstance(HASH_ALGORITHM).digest(seed.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-1 is required for rollout bucketing.", exception);
		}
	}

	private static EvaluationResult result(Flag flag, String variationId, String kind, String ruleId,
			String ruleDescription, String segmentKey, Integer bucket) {
		Variation variation = flag.findVariation(variationId);

		return new EvaluationResult(variationId, variation == null ? null : variation.getName(),
				variation == null ? null : variation.getValue(), kind, ruleId, ruleDescription, segmentKey, bucket);
	}

	private FlagEvaluator() {
	}
}
