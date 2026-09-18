package com.featureflags.targeting;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.featureflags.flags.ClauseValidator;
import com.featureflags.flags.FlagTarget;
import com.featureflags.flags.FlagValidator;
import com.featureflags.flags.Serve;
import com.featureflags.flags.TargetRule;
import com.featureflags.rollouts.RolloutValidator;
import com.featureflags.shared.FieldErrors;
import com.featureflags.shared.Messages;
import com.featureflags.shared.Requests;

public final class TargetingValidator {

	private static final List<String> SERVE_KINDS = List.of(Serve.VARIATION, Serve.ROLLOUT);

	private static final Pattern OBJECT_ID = Pattern.compile("^[a-fA-F\\d]{24}$");

	private static final Pattern VARIATION_ID = Pattern.compile("^v[0-9]{1,3}$");

	private static final int RULES_MAX = 20;

	private static final int SEGMENTS_PER_RULE_MAX = 5;

	private static final int TARGETS_MAX = 8;

	private static final int USER_KEYS_MAX = 500;

	private static final int RULE_DESCRIPTION_MAX = 120;

	private static final int USER_KEY_MAX = 80;

	public record TargetingInput(String offVariationId, List<FlagTarget> targets, List<TargetRule> rules,
			Serve fallthrough, Integer version, String comment) {
	}

	public record EvaluateInput(String userKey) {
	}

	public record RestoreInput(int version, String comment) {
	}

	public static TargetingInput validateTargeting(Map<String, Object> body) {
		Map<String, Object> input = Requests.body(body);
		FieldErrors errors = new FieldErrors();
		String offVariationId = readVariationId(input, "offVariationId", errors);
		List<FlagTarget> targets = readTargets(input, errors);
		List<TargetRule> rules = readRules(input, errors);
		Serve fallthrough = readServe(Requests.map(input, "fallthrough"), "fallthrough", errors);
		Integer version = FlagValidator.readVersion(input, errors);
		String comment = FlagValidator.readComment(input, errors);

		errors.throwIfAny();

		return new TargetingInput(offVariationId, targets, rules, fallthrough, version, comment);
	}

	public static EvaluateInput validateEvaluate(Map<String, Object> body) {
		Map<String, Object> input = Requests.body(body);
		FieldErrors errors = new FieldErrors();
		String userKey = Requests.string(input, "userKey");

		if (userKey == null) {
			errors.add("userKey", Messages.MISSING_STRING);
		} else if (userKey.trim().isEmpty()) {
			errors.add("userKey", Requests.tooSmallString(1));
		} else if (userKey.trim().length() > USER_KEY_MAX) {
			errors.add("userKey", Requests.tooBigString(USER_KEY_MAX));
		}

		errors.throwIfAny();

		return new EvaluateInput(userKey.trim());
	}

	public static RestoreInput validateRestore(Map<String, Object> body) {
		Map<String, Object> input = Requests.body(body);
		FieldErrors errors = new FieldErrors();
		Integer version = Requests.integer(input, "version");

		if (version == null) {
			errors.add("version", Messages.MISSING_NUMBER);
		} else if (version < 1) {
			errors.add("version", Messages.tooSmallNumber(1));
		}

		String comment = FlagValidator.readComment(input, errors);

		errors.throwIfAny();

		return new RestoreInput(version, comment);
	}

	private static List<FlagTarget> readTargets(Map<String, Object> input, FieldErrors errors) {
		List<Object> items = Requests.list(input, "targets");

		if (items == null) {
			if (input.containsKey("targets")) {
				errors.add("targets", Messages.expected("array", "undefined"));
			}

			return List.of();
		}

		if (items.size() > TARGETS_MAX) {
			errors.add("targets", Messages.tooBigArray(TARGETS_MAX));

			return List.of();
		}

		List<FlagTarget> targets = new ArrayList<>();
		Set<String> seenVariations = new LinkedHashSet<>();

		for (int index = 0; index < items.size(); index += 1) {
			Map<String, Object> item = Requests.asMap(items.get(index));
			String field = "targets." + index;

			if (item == null) {
				errors.add(field, Messages.expected("object", "undefined"));

				continue;
			}

			String variationId = readVariationId(item, "variationId", errors, field + ".variationId");

			if (variationId != null && !seenVariations.add(variationId)) {
				errors.add(field + ".variationId", "Each variation can appear once in individual targeting.");
			}

			FlagTarget target = new FlagTarget();
			target.setVariationId(variationId);
			target.setUserKeys(readUserKeys(item, field, errors));
			targets.add(target);
		}

		return targets;
	}

	private static List<String> readUserKeys(Map<String, Object> item, String field, FieldErrors errors) {
		List<String> keys = Requests.strings(item, "userKeys");

		if (keys == null) {
			errors.add(field + ".userKeys", Messages.expected("array of strings", "undefined"));

			return List.of();
		}

		if (keys.size() > USER_KEYS_MAX) {
			errors.add(field + ".userKeys", Messages.tooBigArray(USER_KEYS_MAX));

			return List.of();
		}

		Set<String> unique = new LinkedHashSet<>();

		for (String key : keys) {
			String trimmed = key.trim();

			if (trimmed.isEmpty()) {
				continue;
			}

			if (trimmed.length() > USER_KEY_MAX) {
				errors.add(field + ".userKeys", Requests.tooBigString(USER_KEY_MAX));

				break;
			}

			unique.add(trimmed);
		}

		return new ArrayList<>(unique);
	}

	private static List<TargetRule> readRules(Map<String, Object> input, FieldErrors errors) {
		List<Object> items = Requests.list(input, "rules");

		if (items == null) {
			if (input.containsKey("rules")) {
				errors.add("rules", Messages.expected("array", "undefined"));
			}

			return List.of();
		}

		if (items.size() > RULES_MAX) {
			errors.add("rules", Messages.tooBigArray(RULES_MAX));

			return List.of();
		}

		List<TargetRule> rules = new ArrayList<>();

		for (int index = 0; index < items.size(); index += 1) {
			Map<String, Object> item = Requests.asMap(items.get(index));
			String field = "rules." + index;

			if (item == null) {
				errors.add(field, Messages.expected("object", "undefined"));

				continue;
			}

			TargetRule rule = new TargetRule();
			rule.setId("r" + (index + 1));
			rule.setDescription(readRuleDescription(item, field, errors));
			rule.setSegmentIds(readSegmentIds(item, field, errors));
			rule.setClauses(ClauseValidator.readClauses(item, field, errors));
			rule.setServe(readServe(Requests.map(item, "serve"), field + ".serve", errors));

			if (rule.getSegmentIds().isEmpty() && rule.getClauses().isEmpty()) {
				errors.add(field, "Add a segment or a condition to this rule.");
			}

			rules.add(rule);
		}

		return rules;
	}

	private static String readRuleDescription(Map<String, Object> item, String field, FieldErrors errors) {
		String description = Requests.string(item, "description");

		if (description == null) {
			return "";
		}

		String trimmed = description.trim();

		if (trimmed.length() > RULE_DESCRIPTION_MAX) {
			errors.add(field + ".description", Requests.tooBigString(RULE_DESCRIPTION_MAX));
		}

		return trimmed;
	}

	private static List<String> readSegmentIds(Map<String, Object> item, String field, FieldErrors errors) {
		List<String> ids = Requests.strings(item, "segmentIds");

		if (ids == null) {
			if (item.containsKey("segmentIds")) {
				errors.add(field + ".segmentIds", Messages.expected("array of strings", "undefined"));
			}

			return List.of();
		}

		if (ids.size() > SEGMENTS_PER_RULE_MAX) {
			errors.add(field + ".segmentIds", Messages.tooBigArray(SEGMENTS_PER_RULE_MAX));

			return List.of();
		}

		Set<String> unique = new LinkedHashSet<>();

		for (String id : ids) {
			if (!OBJECT_ID.matcher(id).matches()) {
				errors.add(field + ".segmentIds", "Select a valid segment.");

				return List.of();
			}

			unique.add(id);
		}

		return new ArrayList<>(unique);
	}

	private static Serve readServe(Map<String, Object> input, String field, FieldErrors errors) {
		if (input == null) {
			errors.add(field, Messages.expected("object", "undefined"));

			return new Serve();
		}

		String kind = Requests.string(input, "kind");
		String trimmedKind = kind == null ? Serve.VARIATION : kind.trim();

		if (!SERVE_KINDS.contains(trimmedKind)) {
			errors.add(field + ".kind", Messages.invalidOption(SERVE_KINDS));

			return new Serve();
		}

		if (Serve.ROLLOUT.equals(trimmedKind)) {
			return Serve.ofRollout(RolloutValidator.readRollout(input, field, errors));
		}

		return Serve.ofVariation(readVariationId(input, "variationId", errors, field + ".variationId"));
	}


	private static String readVariationId(Map<String, Object> input, String key, FieldErrors errors) {
		return readVariationId(input, key, errors, key);
	}

	private static String readVariationId(Map<String, Object> input, String key, FieldErrors errors, String field) {
		String variationId = Requests.string(input, key);

		if (variationId == null) {
			errors.add(field, Messages.MISSING_STRING);

			return null;
		}

		String trimmed = variationId.trim();

		if (!VARIATION_ID.matcher(trimmed).matches()) {
			errors.add(field, "Select a valid variation.");

			return null;
		}

		return trimmed;
	}

	private TargetingValidator() {
	}
}
