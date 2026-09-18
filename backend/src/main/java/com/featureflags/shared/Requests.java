package com.featureflags.shared;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class Requests {

	private static final Pattern DATE_ONLY = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

	public static Map<String, Object> body(Map<String, Object> body) {
		return body == null ? Map.of() : body;
	}

	public static Map<String, Object> query(Map<String, String> parameters) {
		return new LinkedHashMap<>(parameters);
	}

	public static boolean has(Map<String, Object> input, String key) {
		return input.get(key) != null;
	}

	public static String string(Map<String, Object> input, String key) {
		Object value = input.get(key);

		return value instanceof String text ? text : null;
	}

	public static Boolean bool(Map<String, Object> input, String key) {
		Object value = input.get(key);

		if (value instanceof Boolean flag) {
			return flag;
		}

		if (value instanceof String text) {
			if ("true".equalsIgnoreCase(text.trim())) {
				return Boolean.TRUE;
			}

			if ("false".equalsIgnoreCase(text.trim())) {
				return Boolean.FALSE;
			}
		}

		return null;
	}

	public static Integer integer(Map<String, Object> input, String key) {
		Object value = input.get(key);

		if (value instanceof Number number) {
			return number.intValue();
		}

		if (value instanceof String text) {
			try {
				return Integer.valueOf(text.trim());
			} catch (NumberFormatException exception) {
				return null;
			}
		}

		return null;
	}

	public static Instant date(Map<String, Object> input, String key) {
		Object value = input.get(key);

		return value instanceof String text ? parseDate(text) : null;
	}

	private static Instant parseDate(String value) {
		try {
			if (DATE_ONLY.matcher(value).matches()) {
				return LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant();
			}

			return OffsetDateTime.parse(value).toInstant();
		} catch (RuntimeException exception) {
			return null;
		}
	}

	public static List<Object> list(Map<String, Object> input, String key) {
		Object value = input.get(key);

		return value instanceof List<?> items ? new ArrayList<>(items) : null;
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> map(Map<String, Object> input, String key) {
		Object value = input.get(key);

		return value instanceof Map<?, ?> nested ? new LinkedHashMap<>((Map<String, Object>) nested) : null;
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> asMap(Object value) {
		return value instanceof Map<?, ?> nested ? new LinkedHashMap<>((Map<String, Object>) nested) : null;
	}

	public static List<String> strings(Map<String, Object> input, String key) {
		List<Object> items = list(input, key);

		if (items == null) {
			return null;
		}

		List<String> values = new ArrayList<>();

		for (Object item : items) {
			if (!(item instanceof String text)) {
				return null;
			}

			values.add(text.trim());
		}

		return values;
	}

	public static List<String> commaSeparated(Map<String, Object> input, String key) {
		String value = string(input, key);

		if (value == null || value.isEmpty()) {
			return List.of();
		}

		return new ArrayList<>(List.of(value.split(","))).stream().filter(item -> !item.isEmpty()).toList();
	}

	public static String tooSmallString(int minimum) {
		return "Too small: expected string to have >=" + minimum + " characters";
	}

	public static String tooBigString(int maximum) {
		return "Too big: expected string to have <=" + maximum + " characters";
	}

	private Requests() {
	}
}
