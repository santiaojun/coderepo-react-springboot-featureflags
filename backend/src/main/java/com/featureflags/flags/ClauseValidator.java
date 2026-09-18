package com.featureflags.flags;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.featureflags.shared.FieldErrors;
import com.featureflags.shared.Messages;
import com.featureflags.shared.Requests;

/**
 * Parses attribute conditions. Shared by flag targeting rules and segment
 * rules, which accept the same clause shape.
 */
public final class ClauseValidator {

	public static final List<String> OPERATORS = List.of("in", "contains", "startsWith", "endsWith", "greaterThan",
			"lessThan");

	public static final int CLAUSES_MAX = 10;

	private static final Pattern ATTRIBUTE = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]{0,59}$");

	private static final String ATTRIBUTE_MESSAGE = "Use a letter followed by letters, numbers, or underscores.";

	private static final int VALUES_MAX = 50;

	private static final int VALUE_MAX = 200;

	public static List<RuleClause> readClauses(Map<String, Object> source, String field, FieldErrors errors) {
		List<Object> items = Requests.list(source, "clauses");

		if (items == null) {
			if (source.containsKey("clauses")) {
				errors.add(field + ".clauses", Messages.expected("array", "undefined"));
			}

			return List.of();
		}

		if (items.size() > CLAUSES_MAX) {
			errors.add(field + ".clauses", Messages.tooBigArray(CLAUSES_MAX));

			return List.of();
		}

		List<RuleClause> clauses = new ArrayList<>();

		for (int index = 0; index < items.size(); index += 1) {
			Map<String, Object> entry = Requests.asMap(items.get(index));
			String clauseField = field + ".clauses." + index;

			if (entry == null) {
				errors.add(clauseField, Messages.expected("object", "undefined"));

				continue;
			}

			RuleClause clause = new RuleClause();
			clause.setAttribute(readAttribute(entry, clauseField, errors));
			clause.setOp(readOperator(entry, clauseField, errors));
			clause.setValues(readValues(entry, clauseField, errors));
			clause.setNegate(Boolean.TRUE.equals(Requests.bool(entry, "negate")));
			clauses.add(clause);
		}

		return clauses;
	}

	private static String readAttribute(Map<String, Object> entry, String field, FieldErrors errors) {
		String attribute = Requests.string(entry, "attribute");

		if (attribute == null) {
			errors.add(field + ".attribute", Messages.MISSING_STRING);

			return null;
		}

		String trimmed = attribute.trim();

		if (!ATTRIBUTE.matcher(trimmed).matches()) {
			errors.add(field + ".attribute", ATTRIBUTE_MESSAGE);
		}

		return trimmed;
	}

	private static String readOperator(Map<String, Object> entry, String field, FieldErrors errors) {
		String op = Requests.string(entry, "op");

		if (op == null) {
			errors.add(field + ".op", Messages.MISSING_STRING);

			return "in";
		}

		String trimmed = op.trim();

		if (!OPERATORS.contains(trimmed)) {
			errors.add(field + ".op", Messages.invalidOption(OPERATORS));

			return "in";
		}

		return trimmed;
	}

	private static List<Object> readValues(Map<String, Object> entry, String field, FieldErrors errors) {
		List<Object> items = Requests.list(entry, "values");

		if (items == null) {
			errors.add(field + ".values", Messages.expected("array", "undefined"));

			return List.of();
		}

		if (items.isEmpty()) {
			errors.add(field + ".values", Messages.tooSmallArray(1));

			return List.of();
		}

		if (items.size() > VALUES_MAX) {
			errors.add(field + ".values", Messages.tooBigArray(VALUES_MAX));

			return List.of();
		}

		List<Object> values = new ArrayList<>();

		for (Object item : items) {
			if (item instanceof Number || item instanceof Boolean) {
				values.add(item);

				continue;
			}

			if (item instanceof String text) {
				String trimmed = text.trim();

				if (trimmed.isEmpty()) {
					continue;
				}

				if (trimmed.length() > VALUE_MAX) {
					errors.add(field + ".values", Requests.tooBigString(VALUE_MAX));

					break;
				}

				values.add(trimmed);

				continue;
			}

			errors.add(field + ".values", Messages.expected("string, number, or boolean", "undefined"));

			break;
		}

		if (values.isEmpty() && !errors.has(field + ".values")) {
			errors.add(field + ".values", Messages.tooSmallArray(1));
		}

		return values;
	}

	private ClauseValidator() {
	}
}
