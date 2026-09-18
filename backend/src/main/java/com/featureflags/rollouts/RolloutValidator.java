package com.featureflags.rollouts;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.featureflags.flags.FlagValidator;
import com.featureflags.flags.Rollout;
import com.featureflags.flags.RolloutWeight;
import com.featureflags.shared.FieldErrors;
import com.featureflags.shared.Messages;
import com.featureflags.shared.Requests;

public final class RolloutValidator {

	private static final Pattern ATTRIBUTE = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]{0,59}$");

	private static final Pattern VARIATION_ID = Pattern.compile("^v[0-9]{1,3}$");

	private static final String ATTRIBUTE_MESSAGE = "Use a letter followed by letters, numbers, or underscores.";

	private static final int WEIGHTS_MIN = 2;

	private static final int WEIGHTS_MAX = 8;

	public record RolloutInput(Rollout rollout, Integer version, String comment) {
	}

	public record PreviewInput(Rollout rollout) {
	}

	public static RolloutInput validateUpdate(Map<String, Object> body) {
		Map<String, Object> input = Requests.body(body);
		FieldErrors errors = new FieldErrors();
		Rollout rollout = readRollout(input, "rollout", errors);
		Integer version = FlagValidator.readVersion(input, errors);
		String comment = FlagValidator.readComment(input, errors);

		errors.throwIfAny();

		return new RolloutInput(rollout, version, comment);
	}

	public static PreviewInput validatePreview(Map<String, Object> body) {
		Map<String, Object> input = Requests.body(body);
		FieldErrors errors = new FieldErrors();
		Rollout rollout = readRollout(input, "rollout", errors);

		errors.throwIfAny();

		return new PreviewInput(rollout);
	}

	/**
	 * Reads the {@code rollout} object out of {@code source}. Shared with
	 * targeting, where a rule can serve a rollout instead of a variation.
	 */
	public static Rollout readRollout(Map<String, Object> source, String field, FieldErrors errors) {
		Map<String, Object> input = Requests.map(source, "rollout");

		if (input == null) {
			errors.add(field + ".rollout", Messages.expected("object", "undefined"));

			return new Rollout();
		}

		Rollout rollout = new Rollout();
		rollout.setBucketBy(readBucketBy(input, field, errors));
		rollout.setWeights(readWeights(input, field, errors));

		return rollout;
	}

	private static String readBucketBy(Map<String, Object> input, String field, FieldErrors errors) {
		String bucketBy = Requests.string(input, "bucketBy");

		if (bucketBy == null || bucketBy.trim().isEmpty()) {
			return Rollout.DEFAULT_BUCKET_BY;
		}

		String trimmed = bucketBy.trim();

		if (!ATTRIBUTE.matcher(trimmed).matches()) {
			errors.add(field + ".rollout.bucketBy", ATTRIBUTE_MESSAGE);

			return Rollout.DEFAULT_BUCKET_BY;
		}

		return trimmed;
	}

	private static List<RolloutWeight> readWeights(Map<String, Object> input, String field, FieldErrors errors) {
		String weightsField = field + ".rollout.weights";
		List<Object> items = Requests.list(input, "weights");

		if (items == null) {
			errors.add(weightsField, Messages.expected("array", "undefined"));

			return List.of();
		}

		if (items.size() < WEIGHTS_MIN) {
			errors.add(weightsField, Messages.tooSmallArray(WEIGHTS_MIN));

			return List.of();
		}

		if (items.size() > WEIGHTS_MAX) {
			errors.add(weightsField, Messages.tooBigArray(WEIGHTS_MAX));

			return List.of();
		}

		List<RolloutWeight> weights = new ArrayList<>();
		Set<String> seen = new LinkedHashSet<>();
		int total = 0;

		for (int index = 0; index < items.size(); index += 1) {
			Map<String, Object> item = Requests.asMap(items.get(index));
			String entryField = weightsField + "." + index;

			if (item == null) {
				errors.add(entryField, Messages.expected("object", "undefined"));

				continue;
			}

			String variationId = readVariationId(item, entryField + ".variationId", errors);

			if (variationId != null && !seen.add(variationId)) {
				errors.add(entryField + ".variationId", "Each variation can appear once in a rollout.");
			}

			Integer weight = Requests.integer(item, "weight");

			if (weight == null) {
				errors.add(entryField + ".weight", Messages.MISSING_NUMBER);

				continue;
			}

			if (weight < 0) {
				errors.add(entryField + ".weight", Messages.tooSmallNumber(0));

				continue;
			}

			if (weight > Rollout.TOTAL_WEIGHT) {
				errors.add(entryField + ".weight", Messages.tooBigNumber(Rollout.TOTAL_WEIGHT));

				continue;
			}

			total += weight;
			RolloutWeight entry = new RolloutWeight();
			entry.setVariationId(variationId);
			entry.setWeight(weight);
			weights.add(entry);
		}

		if (weights.size() == items.size() && total != Rollout.TOTAL_WEIGHT) {
			errors.add(weightsField, "Rollout percentages must add up to 100.");
		}

		return weights;
	}

	private static String readVariationId(Map<String, Object> item, String field, FieldErrors errors) {
		String variationId = Requests.string(item, "variationId");

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

	private RolloutValidator() {
	}
}
