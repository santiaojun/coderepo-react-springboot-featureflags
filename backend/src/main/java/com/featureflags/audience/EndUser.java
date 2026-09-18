package com.featureflags.audience;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.featureflags.shared.Dates;

@Document(collection = "endusers")
public class EndUser {

	@Id
	private String id;

	private String key;

	private String name;

	private String email;

	private Map<String, Object> attributes = new LinkedHashMap<>();

	private Instant lastSeenAt;

	@CreatedDate
	private Instant createdAt;

	/**
	 * Built-in fields and custom attributes flattened into the single map the
	 * evaluator matches clauses against.
	 */
	public Map<String, Object> toEvaluationAttributes() {
		Map<String, Object> flattened = new LinkedHashMap<>();
		flattened.put("key", key);
		flattened.put("name", name);
		flattened.put("email", email);
		flattened.putAll(attributes);

		return flattened;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("_id", id);
		body.put("key", key);
		body.put("name", name);
		body.put("email", email);
		body.put("attributes", new LinkedHashMap<>(attributes));
		body.put("lastSeenAt", Dates.iso(lastSeenAt));
		body.put("createdAt", Dates.iso(createdAt));

		return body;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	public Instant getLastSeenAt() {
		return lastSeenAt;
	}

	public void setLastSeenAt(Instant lastSeenAt) {
		this.lastSeenAt = lastSeenAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
