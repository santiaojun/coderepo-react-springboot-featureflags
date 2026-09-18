package com.featureflags.segments;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.featureflags.flags.ClauseValidator;
import com.featureflags.shared.FieldErrors;
import com.featureflags.shared.Messages;
import com.featureflags.shared.Requests;

public final class SegmentValidator {

	private static final Pattern KEY = Pattern.compile("^[a-z0-9][a-z0-9-]{1,49}$");

	private static final String KEY_MESSAGE = "Use 2 to 50 characters: lowercase letters, numbers, and hyphens.";

	private static final int NAME_MAX = 80;

	private static final int DESCRIPTION_MAX = 1000;

	private static final int RULES_MAX = 10;

	private static final int RULE_DESCRIPTION_MAX = 120;

	private static final int USER_KEYS_MAX = 500;

	private static final int USER_KEY_MAX = 80;

	public record CreateInput(String key, String name, String description, List<String> includedUserKeys,
			List<String> excludedUserKeys, List<SegmentRule> rules) {
	}

	public static String validateEnvironmentQuery(Map<String, String> parameters) {
		Map<String, Object> input = Requests.query(parameters);
		FieldErrors errors = new FieldErrors();
		String environmentKey = Requests.string(input, "environment");
		String trimmed = environmentKey == null ? null : environmentKey.trim();

		if (trimmed == null || trimmed.isEmpty()) {
			errors.add("environment", "Select an environment.");
		}

		errors.throwIfAny();

		return trimmed;
	}

	public static CreateInput validateCreate(Map<String, Object> body) {
		Map<String, Object> input = Requests.body(body);
		FieldErrors errors = new FieldErrors();
		String key = readKey(input, errors);
		String name = readName(input, errors);
		String description = readDescription(input, errors);
		List<String> included = readUserKeys(input, "includedUserKeys", errors);
		List<String> excluded = readUserKeys(input, "excludedUserKeys", errors);
		List<SegmentRule> rules = readRules(input, errors);

		if (errors.isEmpty() && included.isEmpty() && excluded.isEmpty() && rules.isEmpty()) {
			errors.addFormError("Add a rule or at least one included member so this segment matches somebody.");
		}

		errors.throwIfAny();

		return new CreateInput(key, name, description, included, excluded, rules);
	}

	public static Map<String, Object> validateUpdate(Map<String, Object> body) {
		Map<String, Object> input = Requests.body(body);
		FieldErrors errors = new FieldErrors();
		Map<String, Object> updates = new LinkedHashMap<>();

		if (input.containsKey("name")) {
			updates.put("name", readName(input, errors));
		}

		if (input.containsKey("description")) {
			updates.put("description", readDescription(input, errors));
		}

		if (input.containsKey("includedUserKeys")) {
			updates.put("includedUserKeys", readUserKeys(input, "includedUserKeys", errors));
		}

		if (input.containsKey("excludedUserKeys")) {
			updates.put("excludedUserKeys", readUserKeys(input, "excludedUserKeys", errors));
		}

		if (input.containsKey("rules")) {
			updates.put("rules", readRules(input, errors));
		}

		if (updates.isEmpty() && errors.isEmpty()) {
			errors.addFormError("Provide at least one field to update.");
		}

		errors.throwIfAny();

		return updates;
	}

	private static String readKey(Map<String, Object> input, FieldErrors errors) {
		String key = Requests.string(input, "key");

		if (key == null) {
			errors.add("key", Messages.MISSING_STRING);

			return null;
		}

		String trimmed = key.trim().toLowerCase();

		if (!KEY.matcher(trimmed).matches()) {
			errors.add("key", KEY_MESSAGE);
		}

		return trimmed;
	}

	private static String readName(Map<String, Object> input, FieldErrors errors) {
		String name = Requests.string(input, "name");

		if (name == null) {
			errors.add("name", Messages.MISSING_STRING);

			return null;
		}

		String trimmed = name.trim();

		if (trimmed.isEmpty()) {
			errors.add("name", Requests.tooSmallString(1));
		} else if (trimmed.length() > NAME_MAX) {
			errors.add("name", Requests.tooBigString(NAME_MAX));
		}

		return trimmed;
	}

	private static String readDescription(Map<String, Object> input, FieldErrors errors) {
		String description = Requests.string(input, "description");

		if (description == null) {
			if (input.containsKey("description")) {
				errors.add("description", Messages.MISSING_STRING);
			}

			return "";
		}

		String trimmed = description.trim();

		if (trimmed.length() > DESCRIPTION_MAX) {
			errors.add("description", Requests.tooBigString(DESCRIPTION_MAX));
		}

		return trimmed;
	}

	private static List<String> readUserKeys(Map<String, Object> input, String field, FieldErrors errors) {
		if (!input.containsKey(field)) {
			return List.of();
		}

		List<String> keys = Requests.strings(input, field);

		if (keys == null) {
			errors.add(field, Messages.expected("array of strings", "undefined"));

			return List.of();
		}

		if (keys.size() > USER_KEYS_MAX) {
			errors.add(field, Messages.tooBigArray(USER_KEYS_MAX));

			return List.of();
		}

		Set<String> unique = new LinkedHashSet<>();

		for (String key : keys) {
			String trimmed = key.trim();

			if (trimmed.isEmpty()) {
				continue;
			}

			if (trimmed.length() > USER_KEY_MAX) {
				errors.add(field, Requests.tooBigString(USER_KEY_MAX));

				break;
			}

			unique.add(trimmed);
		}

		return new ArrayList<>(unique);
	}

	private static List<SegmentRule> readRules(Map<String, Object> input, FieldErrors errors) {
		if (!input.containsKey("rules")) {
			return List.of();
		}

		List<Object> items = Requests.list(input, "rules");

		if (items == null) {
			errors.add("rules", Messages.expected("array", "undefined"));

			return List.of();
		}

		if (items.size() > RULES_MAX) {
			errors.add("rules", Messages.tooBigArray(RULES_MAX));

			return List.of();
		}

		List<SegmentRule> rules = new ArrayList<>();

		for (int index = 0; index < items.size(); index += 1) {
			Map<String, Object> item = Requests.asMap(items.get(index));
			String field = "rules." + index;

			if (item == null) {
				errors.add(field, Messages.expected("object", "undefined"));

				continue;
			}

			SegmentRule rule = new SegmentRule();
			rule.setId("r" + (index + 1));
			rule.setDescription(readRuleDescription(item, field, errors));
			rule.setClauses(ClauseValidator.readClauses(item, field, errors));

			if (rule.getClauses().isEmpty()) {
				errors.add(field + ".clauses", Messages.tooSmallArray(1));
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

	private SegmentValidator() {
	}
}
