package com.featureflags.audience;

import java.util.Map;

import com.featureflags.shared.FieldErrors;
import com.featureflags.shared.Messages;
import com.featureflags.shared.Requests;

public final class AudienceValidator {

	private static final int LIMIT_DEFAULT = 25;

	private static final int LIMIT_MAX = 200;

	private static final int FILTER_MAX = 60;

	public record ListQuery(String search, String plan, String country, int limit) {
	}

	public static ListQuery validateList(Map<String, String> parameters) {
		Map<String, Object> input = Requests.query(parameters);
		FieldErrors errors = new FieldErrors();
		String search = readFilter(input, "search", errors);
		String plan = readFilter(input, "plan", errors);
		String country = readFilter(input, "country", errors);
		int limit = LIMIT_DEFAULT;

		if (Requests.has(input, "limit")) {
			Integer requested = Requests.integer(input, "limit");

			if (requested == null) {
				errors.add("limit", Messages.MISSING_NUMBER);
			} else if (requested < 1) {
				errors.add("limit", Messages.tooSmallNumber(1));
			} else if (requested > LIMIT_MAX) {
				errors.add("limit", Messages.tooBigNumber(LIMIT_MAX));
			} else {
				limit = requested;
			}
		}

		errors.throwIfAny();

		return new ListQuery(search, plan, country, limit);
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

	private static String readFilter(Map<String, Object> input, String field, FieldErrors errors) {
		String value = Requests.string(input, field);

		if (value == null) {
			return null;
		}

		String trimmed = value.trim();

		if (trimmed.isEmpty()) {
			return null;
		}

		if (trimmed.length() > FILTER_MAX) {
			errors.add(field, Requests.tooBigString(FILTER_MAX));

			return null;
		}

		return trimmed;
	}

	private AudienceValidator() {
	}
}
