package com.featureflags.history;

import java.util.Map;
import java.util.regex.Pattern;

import com.featureflags.shared.FieldErrors;
import com.featureflags.shared.Messages;
import com.featureflags.shared.Requests;

public final class HistoryValidator {

	private static final Pattern OBJECT_ID = Pattern.compile("^[a-fA-F\\d]{24}$");

	private static final int LIMIT_DEFAULT = 50;

	private static final int LIMIT_MAX = 200;

	public record ListQuery(String flagKey, String environmentKey, String actorId, String action, int limit) {
	}

	public static ListQuery validateList(Map<String, String> parameters) {
		Map<String, Object> input = Requests.query(parameters);
		FieldErrors errors = new FieldErrors();
		String flagKey = trimmedOrNull(Requests.string(input, "flagKey"));
		String environmentKey = trimmedOrNull(Requests.string(input, "environment"));
		String actorId = trimmedOrNull(Requests.string(input, "actorId"));
		String action = trimmedOrNull(Requests.string(input, "action"));
		int limit = LIMIT_DEFAULT;

		if (actorId != null && !OBJECT_ID.matcher(actorId).matches()) {
			errors.add("actorId", "Select a valid member.");
		}

		if (action != null && !ChangeRecord.ACTIONS.contains(action)) {
			errors.add("action", Messages.invalidOption(ChangeRecord.ACTIONS));
		}

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

		return new ListQuery(flagKey, environmentKey, actorId, action, limit);
	}

	private static String trimmedOrNull(String value) {
		if (value == null) {
			return null;
		}

		String trimmed = value.trim();

		return trimmed.isEmpty() ? null : trimmed;
	}

	private HistoryValidator() {
	}
}
