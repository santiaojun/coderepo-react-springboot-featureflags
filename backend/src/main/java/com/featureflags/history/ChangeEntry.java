package com.featureflags.history;

import java.util.LinkedHashMap;
import java.util.Map;

public class ChangeEntry {

	private String field;

	private String label;

	private Object from;

	private Object to;

	public static ChangeEntry of(String field, String label, Object from, Object to) {
		ChangeEntry entry = new ChangeEntry();
		entry.setField(field);
		entry.setLabel(label);
		entry.setFrom(from);
		entry.setTo(to);

		return entry;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("field", field);
		body.put("label", label);
		body.put("from", from);
		body.put("to", to);

		return body;
	}

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public Object getFrom() {
		return from;
	}

	public void setFrom(Object from) {
		this.from = from;
	}

	public Object getTo() {
		return to;
	}

	public void setTo(Object to) {
		this.to = to;
	}
}
