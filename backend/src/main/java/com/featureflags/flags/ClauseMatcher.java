package com.featureflags.flags;

import java.util.List;
import java.util.Map;

/**
 * Matches attribute conditions against an audience member. Shared by flag
 * targeting rules and segment rules so both read the same way.
 */
public final class ClauseMatcher {

	public static boolean matchesAll(List<RuleClause> clauses, Map<String, Object> attributes) {
		for (RuleClause clause : clauses) {
			if (!matches(clause, attributes)) {
				return false;
			}
		}

		return true;
	}

	public static boolean matches(RuleClause clause, Map<String, Object> attributes) {
		Object attribute = attributes.get(clause.getAttribute());

		// A missing attribute never satisfies a clause, including a negated one.
		if (attribute == null) {
			return false;
		}

		boolean matched = switch (clause.getOp()) {
			case "in" -> clause.getValues().stream().anyMatch(value -> equalValues(attribute, value));
			case "contains" -> clause.getValues().stream().anyMatch(value -> text(attribute).contains(text(value)));
			case "startsWith" -> clause.getValues().stream().anyMatch(value -> text(attribute).startsWith(text(value)));
			case "endsWith" -> clause.getValues().stream().anyMatch(value -> text(attribute).endsWith(text(value)));
			case "greaterThan" -> compare(attribute, clause.getValues()) > 0;
			case "lessThan" -> compare(attribute, clause.getValues()) < 0;
			default -> false;
		};

		return clause.isNegate() != matched;
	}

	private static int compare(Object attribute, List<Object> values) {
		Double left = number(attribute);
		Double right = values.isEmpty() ? null : number(values.get(0));

		if (left == null || right == null) {
			return 0;
		}

		return Double.compare(left, right);
	}

	private static boolean equalValues(Object attribute, Object value) {
		Double left = number(attribute);
		Double right = number(value);

		if (left != null && right != null) {
			return left.doubleValue() == right.doubleValue();
		}

		return text(attribute).equals(text(value));
	}

	private static Double number(Object value) {
		return value instanceof Number number ? number.doubleValue() : null;
	}

	private static String text(Object value) {
		return String.valueOf(value).toLowerCase();
	}

	private ClauseMatcher() {
	}
}
