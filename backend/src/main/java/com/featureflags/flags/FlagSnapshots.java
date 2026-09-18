package com.featureflags.flags;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.featureflags.shared.Requests;

/**
 * Reads a stored configuration snapshot back into a live configuration. A
 * snapshot is written by {@link FlagConfiguration#toSnapshot()}, so restoring an
 * earlier version is a straight round trip rather than a partial replay.
 */
public final class FlagSnapshots {

	public static void apply(FlagConfiguration configuration, Map<String, Object> snapshot) {
		configuration.setEnabled(Boolean.TRUE.equals(Requests.bool(snapshot, "enabled")));
		configuration.setOffVariationId(Requests.string(snapshot, "offVariationId"));
		configuration.setTargets(readTargets(snapshot));
		configuration.setRules(readRules(snapshot));
		configuration.setFallthrough(readServe(Requests.map(snapshot, "fallthrough")));
	}

	/**
	 * Every variation a snapshot refers to, so a restore can be refused when the
	 * flag no longer has one of them.
	 */
	public static Set<String> referencedVariationIds(Map<String, Object> snapshot) {
		Set<String> ids = new LinkedHashSet<>();
		addIfPresent(ids, Requests.string(snapshot, "offVariationId"));
		collectServe(ids, Requests.map(snapshot, "fallthrough"));

		for (Map<String, Object> target : maps(snapshot, "targets")) {
			addIfPresent(ids, Requests.string(target, "variationId"));
		}

		for (Map<String, Object> rule : maps(snapshot, "rules")) {
			collectServe(ids, Requests.map(rule, "serve"));
		}

		return ids;
	}

	private static void collectServe(Set<String> ids, Map<String, Object> serve) {
		if (serve == null) {
			return;
		}

		addIfPresent(ids, Requests.string(serve, "variationId"));
		Map<String, Object> rollout = Requests.map(serve, "rollout");

		if (rollout != null) {
			for (Map<String, Object> weight : maps(rollout, "weights")) {
				addIfPresent(ids, Requests.string(weight, "variationId"));
			}
		}
	}

	private static List<FlagTarget> readTargets(Map<String, Object> snapshot) {
		List<FlagTarget> targets = new ArrayList<>();

		for (Map<String, Object> item : maps(snapshot, "targets")) {
			FlagTarget target = new FlagTarget();
			target.setVariationId(Requests.string(item, "variationId"));
			List<String> keys = Requests.strings(item, "userKeys");
			target.setUserKeys(keys == null ? new ArrayList<>() : new ArrayList<>(keys));
			targets.add(target);
		}

		return targets;
	}

	private static List<TargetRule> readRules(Map<String, Object> snapshot) {
		List<TargetRule> rules = new ArrayList<>();

		for (Map<String, Object> item : maps(snapshot, "rules")) {
			TargetRule rule = new TargetRule();
			rule.setId(Requests.string(item, "id"));
			String description = Requests.string(item, "description");
			rule.setDescription(description == null ? "" : description);
			List<String> segmentIds = Requests.strings(item, "segmentIds");
			rule.setSegmentIds(segmentIds == null ? new ArrayList<>() : new ArrayList<>(segmentIds));
			rule.setClauses(readClauses(item));
			rule.setServe(readServe(Requests.map(item, "serve")));
			rules.add(rule);
		}

		return rules;
	}

	private static List<RuleClause> readClauses(Map<String, Object> rule) {
		List<RuleClause> clauses = new ArrayList<>();

		for (Map<String, Object> item : maps(rule, "clauses")) {
			RuleClause clause = new RuleClause();
			clause.setAttribute(Requests.string(item, "attribute"));
			String op = Requests.string(item, "op");
			clause.setOp(op == null ? "in" : op);
			List<Object> values = Requests.list(item, "values");
			clause.setValues(values == null ? new ArrayList<>() : new ArrayList<>(values));
			clause.setNegate(Boolean.TRUE.equals(Requests.bool(item, "negate")));
			clauses.add(clause);
		}

		return clauses;
	}

	private static Serve readServe(Map<String, Object> source) {
		if (source == null) {
			return new Serve();
		}

		String kind = Requests.string(source, "kind");

		if (Serve.ROLLOUT.equals(kind)) {
			return Serve.ofRollout(readRollout(Requests.map(source, "rollout")));
		}

		return Serve.ofVariation(Requests.string(source, "variationId"));
	}

	private static Rollout readRollout(Map<String, Object> source) {
		Rollout rollout = new Rollout();

		if (source == null) {
			return rollout;
		}

		String bucketBy = Requests.string(source, "bucketBy");
		rollout.setBucketBy(bucketBy == null ? Rollout.DEFAULT_BUCKET_BY : bucketBy);
		List<RolloutWeight> weights = new ArrayList<>();

		for (Map<String, Object> item : maps(source, "weights")) {
			RolloutWeight weight = new RolloutWeight();
			weight.setVariationId(Requests.string(item, "variationId"));
			Integer value = Requests.integer(item, "weight");
			weight.setWeight(value == null ? 0 : value);
			weights.add(weight);
		}

		rollout.setWeights(weights);

		return rollout;
	}

	private static List<Map<String, Object>> maps(Map<String, Object> source, String key) {
		List<Object> items = source == null ? null : Requests.list(source, key);

		if (items == null) {
			return List.of();
		}

		List<Map<String, Object>> results = new ArrayList<>();

		for (Object item : items) {
			Map<String, Object> entry = Requests.asMap(item);

			if (entry != null) {
				results.add(entry);
			}
		}

		return results;
	}

	private static void addIfPresent(Set<String> ids, String value) {
		if (value != null) {
			ids.add(value);
		}
	}

	private FlagSnapshots() {
	}
}
