package com.featureflags.flags;

import java.util.LinkedHashMap;
import java.util.Map;

public class Variation {

	private String id;

	private String name;

	private Object value;

	private String description = "";

	public Map<String, Object> toMap() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("id", id);
		body.put("name", name);
		body.put("value", value);
		body.put("description", description);

		return body;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
