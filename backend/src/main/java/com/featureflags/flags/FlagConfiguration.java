package com.featureflags.flags;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.featureflags.shared.Dates;

@Document(collection = "flagconfigurations")
public class FlagConfiguration {

	@Id
	private String id;

	@Field(targetType = FieldType.OBJECT_ID)
	private String flagId;

	@Field(targetType = FieldType.OBJECT_ID)
	private String environmentId;

	private String flagKey;

	private String environmentKey;

	private boolean enabled;

	private int version = 1;

	private String offVariationId;

	private List<FlagTarget> targets = new ArrayList<>();

	private List<TargetRule> rules = new ArrayList<>();

	private Serve fallthrough = new Serve();

	@CreatedDate
	private Instant createdAt;

	private Instant lastModifiedAt;

	@Field(targetType = FieldType.OBJECT_ID)
	private String lastModifiedBy;

	private String lastModifiedByName;

	public int targetedUserCount() {
		return targets.stream().mapToInt(target -> target.getUserKeys().size()).sum();
	}

	public Map<String, Object> toMap() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("_id", id);
		body.put("flagId", flagId);
		body.put("environmentId", environmentId);
		body.put("flagKey", flagKey);
		body.put("environmentKey", environmentKey);
		body.put("enabled", enabled);
		body.put("version", version);
		body.put("offVariationId", offVariationId);
		body.put("targets", targets.stream().map(FlagTarget::toMap).toList());
		body.put("rules", rules.stream().map(TargetRule::toMap).toList());
		body.put("fallthrough", fallthrough.toMap());
		body.put("lastModifiedAt", Dates.iso(lastModifiedAt));
		body.put("lastModifiedBy", lastModifiedBy);
		body.put("lastModifiedByName", lastModifiedByName);

		return body;
	}

	/**
	 * The restorable portion of a configuration. Change records store this so an
	 * earlier configuration can be written back as a new version.
	 */
	public Map<String, Object> toSnapshot() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("enabled", enabled);
		body.put("offVariationId", offVariationId);
		body.put("targets", targets.stream().map(FlagTarget::toMap).toList());
		body.put("rules", rules.stream().map(TargetRule::toMap).toList());
		body.put("fallthrough", fallthrough.toMap());

		return body;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getFlagId() {
		return flagId;
	}

	public void setFlagId(String flagId) {
		this.flagId = flagId;
	}

	public String getEnvironmentId() {
		return environmentId;
	}

	public void setEnvironmentId(String environmentId) {
		this.environmentId = environmentId;
	}

	public String getFlagKey() {
		return flagKey;
	}

	public void setFlagKey(String flagKey) {
		this.flagKey = flagKey;
	}

	public String getEnvironmentKey() {
		return environmentKey;
	}

	public void setEnvironmentKey(String environmentKey) {
		this.environmentKey = environmentKey;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public String getOffVariationId() {
		return offVariationId;
	}

	public void setOffVariationId(String offVariationId) {
		this.offVariationId = offVariationId;
	}

	public List<FlagTarget> getTargets() {
		return targets;
	}

	public void setTargets(List<FlagTarget> targets) {
		this.targets = targets;
	}

	public List<TargetRule> getRules() {
		return rules;
	}

	public void setRules(List<TargetRule> rules) {
		this.rules = rules;
	}

	public Serve getFallthrough() {
		return fallthrough;
	}

	public void setFallthrough(Serve fallthrough) {
		this.fallthrough = fallthrough;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getLastModifiedAt() {
		return lastModifiedAt;
	}

	public void setLastModifiedAt(Instant lastModifiedAt) {
		this.lastModifiedAt = lastModifiedAt;
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
