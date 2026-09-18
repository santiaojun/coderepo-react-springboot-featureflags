package com.featureflags.segments;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.featureflags.shared.Dates;

@Document(collection = "segments")
public class Segment {

	@Id
	private String id;

	private String key;

	private String name;

	private String description = "";

	@Field(targetType = FieldType.OBJECT_ID)
	private String environmentId;

	private String environmentKey;

	private List<String> includedUserKeys = new ArrayList<>();

	private List<String> excludedUserKeys = new ArrayList<>();

	private List<SegmentRule> rules = new ArrayList<>();

	@CreatedDate
	private Instant createdAt;

	@LastModifiedDate
	private Instant updatedAt;

	@Field(targetType = FieldType.OBJECT_ID)
	private String lastModifiedBy;

	private String lastModifiedByName;

	public Map<String, Object> toMap() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("_id", id);
		body.put("key", key);
		body.put("name", name);
		body.put("description", description);
		body.put("environmentId", environmentId);
		body.put("environmentKey", environmentKey);
		body.put("includedUserKeys", new ArrayList<>(includedUserKeys));
		body.put("excludedUserKeys", new ArrayList<>(excludedUserKeys));
		body.put("rules", rules.stream().map(SegmentRule::toMap).toList());
		body.put("createdAt", Dates.iso(createdAt));
		body.put("updatedAt", Dates.iso(updatedAt));
		body.put("lastModifiedBy", lastModifiedBy);
		body.put("lastModifiedByName", lastModifiedByName);

		return body;
	}

	public Map<String, Object> toSummaryMap() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("_id", id);
		body.put("key", key);
		body.put("name", name);
		body.put("environmentKey", environmentKey);

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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getEnvironmentId() {
		return environmentId;
	}

	public void setEnvironmentId(String environmentId) {
		this.environmentId = environmentId;
	}

	public String getEnvironmentKey() {
		return environmentKey;
	}

	public void setEnvironmentKey(String environmentKey) {
		this.environmentKey = environmentKey;
	}

	public List<String> getIncludedUserKeys() {
		return includedUserKeys;
	}

	public void setIncludedUserKeys(List<String> includedUserKeys) {
		this.includedUserKeys = includedUserKeys;
	}

	public List<String> getExcludedUserKeys() {
		return excludedUserKeys;
	}

	public void setExcludedUserKeys(List<String> excludedUserKeys) {
		this.excludedUserKeys = excludedUserKeys;
	}

	public List<SegmentRule> getRules() {
		return rules;
	}

	public void setRules(List<SegmentRule> rules) {
		this.rules = rules;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

	public String getLastModifiedBy() {
		return lastModifiedBy;
	}

	public void setLastModifiedBy(String lastModifiedBy) {
		this.lastModifiedBy = lastModifiedBy;
	}

	public String getLastModifiedByName() {
		return lastModifiedByName;
	}

	public void setLastModifiedByName(String lastModifiedByName) {
		this.lastModifiedByName = lastModifiedByName;
	}
}
