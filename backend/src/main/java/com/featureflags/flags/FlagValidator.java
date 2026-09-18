package com.featureflags.flags;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.featureflags.shared.FieldErrors;
import com.featureflags.shared.Messages;
import com.featureflags.shared.Requests;

public final class FlagValidator {

	public static final int COMMENT_MAX = 280;

	private static final Pattern KEY = Pattern.compile("^[a-z0-9][a-z0-9-]{1,49}$");

	private static final Pattern OBJECT_ID = Pattern.compile("^[a-fA-F\\d]{24}$");

	private static final Pattern TAG = Pattern.compile("^[a-z0-9][a-z0-9-]{0,29}$");

	private static final String KEY_MESSAGE = "Use 2 to 50 characters: lowercase letters, numbers, and hyphens.";

	private static final String TAG_MESSAGE = "Use lowercase letters, numbers, and hyphens, up to 30 characters.";

	private static final int NAME_MAX = 80;

	private static final int DESCRIPTION_MAX = 1000;

	private static final int VARIATION_NAME_MAX = 60;

	private static final int VARIATION_DESCRIPTION_MAX = 280;

	private static final int VARIATION_VALUE_MAX = 200;

	private static final int VARIATIONS_MIN = 2;

	private static final int VARIATIONS_MAX = 8;

	private static final int TAGS_MAX = 8;

	private static final List<String> KINDS = List.of(Flag.BOOLEAN, Flag.MULTIVARIATE);

	private static final List<String> STATUS_FILTERS = List.of(Flag.ACTIVE, Flag.RETIRED, "all");

	private static final List<String> STATE_FILTERS = List.of("all", "on", "off", "rollout", "targeted");

	public record VariationInput(String name, Object value, String description) {
	}

	public record CreateInput(String key, String name, String description, String kind, List<String> tags,
			boolean temporary, String maintainerId, List<VariationInput> variations, int defaultOnIndex,
			int defaultOffIndex) {
	}

	public record ListQuery(String environmentKey, String search, String tag, String status, String state) {
	}

	public record ToggleInput(boolean enabled, Integer version, String comment) {
	}

	public static ListQuery validateList(Map<String, String> parameters) {
		Map<String, Object> input = Requests.query(parameters);
		FieldErrors errors = new FieldErrors();
		String environmentKey = requireEnvironment(input, errors);
		String search = trimmedOrNull(Requests.string(input, "search"));
		String tag = trimmedOrNull(Requests.string(input, "tag"));
		String status = readOption(input, "status", STATUS_FILTERS, Flag.ACTIVE, errors);
		String state = readOption(input, "state", STATE_FILTERS, "all", errors);

		errors.throwIfAny();

		return new ListQuery(environmentKey, search, tag, "all".equals(status) ? null : status, state);
	}

	public static String validateEnvironmentQuery(Map<String, String> parameters) {
		Map<String, Object> input = Requests.query(parameters);
		FieldErrors errors = new FieldErrors();
		String environmentKey = requireEnvironment(input, errors);

		errors.throwIfAny();

		return environmentKey;
	}

	public static CreateInput validateCreate(Map<String, Object> body) {
		Map<String, Object> input = Requests.body(body);
		FieldErrors errors = new FieldErrors();
		String key = readKey(input, errors);
		String name = readName(input, errors);
		String description = readDescription(input, errors);
		String kind = readOption(input, "kind", KINDS, Flag.BOOLEAN, errors);
		List<String> tags = readTags(input, errors);
		Boolean temporary = Requests.bool(input, "temporary");
		String maintainerId = readMaintainerId(input, errors);
		List<VariationInput> variations = readVariations(input, kind, errors);
		int onIndex = readVariationIndex(input, "defaultOnVariationIndex", 0, variations.size(), errors);
		int offIndex = readVariationIndex(input, "defaultOffVariationIndex",
				variations.isEmpty() ? 0 : variations.size() - 1, variations.size(), errors);

		errors.throwIfAny();

		return new CreateInput(key, name, description, kind, tags, temporary == null || temporary, maintainerId,
				variations, onIndex, offIndex);
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

		if (input.containsKey("tags")) {
			updates.put("tags", readTags(input, errors));
		}

		if (input.containsKey("temporary")) {
			Boolean temporary = Requests.bool(input, "temporary");

			if (temporary == null) {
				errors.add("temporary", Messages.expected("boolean", "undefined"));
			} else {
				updates.put("temporary", temporary);
			}
		}

		if (input.containsKey("maintainerId")) {
			updates.put("maintainerId", readMaintainerId(input, errors));
		}

		if (input.containsKey("variationNames")) {
			updates.put("variationNames", readVariationNames(input, errors));
		}

		if (updates.isEmpty() && errors.isEmpty()) {
			errors.addFormError("Provide at least one field to update.");
		}

		errors.throwIfAny();

		return updates;
	}

	public static ToggleInput validateToggle(Map<String, Object> body) {
		Map<String, Object> input = Requests.body(body);
		FieldErrors errors = new FieldErrors();
		Boolean enabled = Requests.bool(input, "enabled");

		if (enabled == null) {
			errors.add("enabled", Messages.expected("boolean", "undefined"));
		}

		Integer version = readVersion(input, errors);
		String comment = readComment(input, errors);

		errors.throwIfAny();

		return new ToggleInput(Boolean.TRUE.equals(enabled), version, comment);
	}

	public static Integer readVersion(Map<String, Object> input, FieldErrors errors) {
		if (!Requests.has(input, "version")) {
			return null;
		}

		Integer version = Requests.integer(input, "version");

		if (version == null) {
			errors.add("version", Messages.MISSING_NUMBER);

			return null;
		}

		if (version < 1) {
			errors.add("version", Messages.tooSmallNumber(1));

			return null;
		}

		return version;
	}

	public static String readComment(Map<String, Object> input, FieldErrors errors) {
		String comment = Requests.string(input, "comment");

		if (comment == null) {
			return "";
		}

		String trimmed = comment.trim();

		if (trimmed.length() > COMMENT_MAX) {
			errors.add("comment", Requests.tooBigString(COMMENT_MAX));
		}

		return trimmed;
	}

	private static String requireEnvironment(Map<String, Object> input, FieldErrors errors) {
		String environmentKey = trimmedOrNull(Requests.string(input, "environment"));

		if (environmentKey == null) {
			errors.add("environment", "Select an environment.");
		}

		return environmentKey;
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

	private static List<String> readTags(Map<String, Object> input, FieldErrors errors) {
		if (!input.containsKey("tags")) {
			return List.of();
		}

		List<String> tags = Requests.strings(input, "tags");

		if (tags == null) {
			errors.add("tags", Messages.expected("array of strings", "undefined"));

			return List.of();
		}

		if (tags.size() > TAGS_MAX) {
			errors.add("tags", Messages.tooBigArray(TAGS_MAX));
		}

		Set<String> unique = new LinkedHashSet<>();

		for (String tag : tags) {
			String normalized = tag.trim().toLowerCase();

			if (normalized.isEmpty()) {
				continue;
			}

			if (!TAG.matcher(normalized).matches()) {
				errors.add("tags", TAG_MESSAGE);

				break;
			}

			unique.add(normalized);
		}

		return new ArrayList<>(unique);
	}

	private static String readMaintainerId(Map<String, Object> input, FieldErrors errors) {
		String maintainerId = trimmedOrNull(Requests.string(input, "maintainerId"));

		if (maintainerId != null && !OBJECT_ID.matcher(maintainerId).matches()) {
			errors.add("maintainerId", "Select a valid member.");

			return null;
		}

		return maintainerId;
	}

	private static List<VariationInput> readVariations(Map<String, Object> input, String kind, FieldErrors errors) {
		List<Object> items = Requests.list(input, "variations");

		if (items == null) {
			errors.add("variations", Messages.expected("array", "undefined"));

			return List.of();
		}

		if (items.size() < VARIATIONS_MIN) {
			errors.add("variations", Messages.tooSmallArray(VARIATIONS_MIN));

			return List.of();
		}

		if (items.size() > VARIATIONS_MAX) {
			errors.add("variations", Messages.tooBigArray(VARIATIONS_MAX));

			return List.of();
		}

		if (Flag.BOOLEAN.equals(kind) && items.size() != VARIATIONS_MIN) {
			errors.add("variations", "A boolean flag has exactly two variations.");

			return List.of();
		}

		List<VariationInput> variations = new ArrayList<>();
		Set<String> seenValues = new LinkedHashSet<>();

		for (int index = 0; index < items.size(); index += 1) {
			Map<String, Object> item = Requests.asMap(items.get(index));

			if (item == null) {
				errors.add("variations." + index, Messages.expected("object", "undefined"));

				continue;
			}

			String name = Requests.string(item, "name");
			String trimmedName = name == null ? "" : name.trim();

			if (trimmedName.isEmpty()) {
				errors.add("variations." + index + ".name", Requests.tooSmallString(1));
			} else if (trimmedName.length() > VARIATION_NAME_MAX) {
				errors.add("variations." + index + ".name", Requests.tooBigString(VARIATION_NAME_MAX));
			}

			Object value = readVariationValue(item, kind, index, errors);

			if (value != null && !seenValues.add(String.valueOf(value))) {
				errors.add("variations." + index + ".value", "Each variation needs a distinct value.");
			}

			String description = Requests.string(item, "description");
			String trimmedDescription = description == null ? "" : description.trim();

			if (trimmedDescription.length() > VARIATION_DESCRIPTION_MAX) {
				errors.add("variations." + index + ".description", Requests.tooBigString(VARIATION_DESCRIPTION_MAX));
			}

			variations.add(new VariationInput(trimmedName, value, trimmedDescription));
		}

		if (Flag.BOOLEAN.equals(kind) && errors.isEmpty()
				&& !(Boolean.TRUE.equals(variations.get(0).value()) && Boolean.FALSE.equals(variations.get(1).value()))) {
			errors.add("variations", "A boolean flag serves true first and false second.");
		}

		return variations;
	}

	private static Object readVariationValue(Map<String, Object> item, String kind, int index, FieldErrors errors) {
		Object value = item.get("value");
		String field = "variations." + index + ".value";

		if (Flag.BOOLEAN.equals(kind)) {
			Boolean flag = Requests.bool(item, "value");

			if (flag == null) {
				errors.add(field, Messages.expected("boolean", "undefined"));
			}

			return flag;
		}

		if (value instanceof Boolean || value instanceof Number) {
			return value;
		}

		if (value instanceof String text) {
			String trimmed = text.trim();

			if (trimmed.isEmpty()) {
				errors.add(field, Requests.tooSmallString(1));
			} else if (trimmed.length() > VARIATION_VALUE_MAX) {
				errors.add(field, Requests.tooBigString(VARIATION_VALUE_MAX));
			}

			return trimmed;
		}

		errors.add(field, Messages.expected("string, number, or boolean", "undefined"));

		return null;
	}

	private static List<Map<String, Object>> readVariationNames(Map<String, Object> input, FieldErrors errors) {
		List<Object> items = Requests.list(input, "variationNames");

		if (items == null) {
			errors.add("variationNames", Messages.expected("array", "undefined"));

			return List.of();
		}

		List<Map<String, Object>> renames = new ArrayList<>();

		for (int index = 0; index < items.size(); index += 1) {
			Map<String, Object> item = Requests.asMap(items.get(index));
			String id = item == null ? null : Requests.string(item, "id");

			if (id == null || id.isBlank()) {
				errors.add("variationNames." + index + ".id", Messages.MISSING_STRING);

				continue;
			}

			Map<String, Object> rename = new LinkedHashMap<>();
			rename.put("id", id.trim());

			if (item.containsKey("name")) {
				String name = Requests.string(item, "name");
				String trimmed = name == null ? "" : name.trim();

				if (trimmed.isEmpty()) {
					errors.add("variationNames." + index + ".name", Requests.tooSmallString(1));
				} else if (trimmed.length() > VARIATION_NAME_MAX) {
					errors.add("variationNames." + index + ".name", Requests.tooBigString(VARIATION_NAME_MAX));
				}

				rename.put("name", trimmed);
			}

			if (item.containsKey("description")) {
				String description = Requests.string(item, "description");
				String trimmed = description == null ? "" : description.trim();

				if (trimmed.length() > VARIATION_DESCRIPTION_MAX) {
					errors.add("variationNames." + index + ".description",
							Requests.tooBigString(VARIATION_DESCRIPTION_MAX));
				}

				rename.put("description", trimmed);
			}

			renames.add(rename);
		}

		return renames;
	}

	private static int readVariationIndex(Map<String, Object> input, String field, int fallback, int size,
			FieldErrors errors) {
		if (!Requests.has(input, field)) {
			return fallback;
		}

		Integer index = Requests.integer(input, field);

		if (index == null) {
			errors.add(field, Messages.MISSING_NUMBER);

			return fallback;
		}

		if (index < 0) {
			errors.add(field, Messages.tooSmallNumber(0));

			return fallback;
		}

		if (size > 0 && index >= size) {
			errors.add(field, Messages.tooBigNumber(size - 1));

			return fallback;
		}

		return index;
	}

	private static String readOption(Map<String, Object> input, String field, List<String> options, String fallback,
			FieldErrors errors) {
		String value = trimmedOrNull(Requests.string(input, field));

		if (value == null) {
			return fallback;
		}

		if (!options.contains(value)) {
			errors.add(field, Messages.invalidOption(options));

			return fallback;
		}

		return value;
	}

	private static String trimmedOrNull(String value) {
		if (value == null) {
			return null;
		}

		String trimmed = value.trim();

		return trimmed.isEmpty() ? null : trimmed;
	}

	private FlagValidator() {
	}
}
