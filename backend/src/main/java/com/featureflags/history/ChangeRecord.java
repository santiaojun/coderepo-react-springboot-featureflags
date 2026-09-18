package com.featureflags.history;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.featureflags.shared.Dates;

@Document(collection = "changerecords")
public class ChangeRecord {

	public static final String FLAG_CREATED = "flag_created";

	public static final String FLAG_UPDATED = "flag_updated";

	public static final String TOGGLED = "toggled";

	public static final String TARGETING_UPDATED = "targeting_updated";

	public static final String ROLLOUT_UPDATED = "rollout_updated";

	public static final String ROLLED_BACK = "rolled_back";

	public static final String FLAG_RETIRED = "flag_retired";

	public static final String FLAG_RESTORED = "flag_restored";

	public static final List<String> ACTIONS = List.of(FLAG_CREATED, FLAG_UPDATED, TOGGLED, TARGETING_UPDATED,
			ROLLOUT_UPDATED, ROLLED_BACK, FLAG_RETIRED, FLAG_RESTORED);

	@Id
	private String id;

	@Field(targetType = FieldType.OBJECT_ID)
	private String flagId;

	private String flagKey;

	private String flagName;

	@Field(targetType = FieldType.OBJECT_ID)
	private String environmentId;

	private String environmentKey;

	private int version;

	private String action;

	private String summary;

	private List<ChangeEntry> changes = new ArrayList<>();

	private Map<String, Object> snapshot = new LinkedHashMap<>();

	private Integer restoredFromVersion;

	private String comment = "";

	@Field(targetType = FieldType.OBJECT_ID)
	private String actorId;

	private String actorName;

	private Instant createdAt;

	public Map<String, Object> toMap() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("_id", id);
		body.put("flagId", flagId);
		body.put("flagKey", flagKey);
		body.put("flagName", flagName);
		body.put("environmentId", environmentId);
		body.put("environmentKey", environmentKey);
		body.put("version", version);
		body.put("action", action);
		body.put("summary", summary);
		body.put("changes", changes.stream().map(ChangeEntry::toMap).toList());
		body.put("restoredFromVersion", restoredFromVersion);
		body.put("comment", comment);
		body.put("actorId", actorId);
		body.put("actorName", actorName);
		body.put("createdAt", Dates.iso(createdAt));

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

	public String getFlagKey() {
		return flagKey;
	}

	public void setFlagKey(String flagKey) {
		this.flagKey = flagKey;
	}

	public String getFlagName() {
		return flagName;
	}

	public void setFlagName(String flagName) {
		this.flagName = flagName;
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

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public List<ChangeEntry> getChanges() {
		return changes;
	}

	public void setChanges(List<ChangeEntry> changes) {
		this.changes = changes;
	}

	public Map<String, Object> getSnapshot() {
		return snapshot;
	}

	public void setSnapshot(Map<String, Object> snapshot) {
		this.snapshot = snapshot;
	}

	public Integer getRestoredFromVersion() {
		return restoredFromVersion;
	}

	public void setRestoredFromVersion(Integer restoredFromVersion) {
		this.restoredFromVersion = restoredFromVersion;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getActorId() {
		return actorId;
	}

	public void setActorId(String actorId) {
		this.actorId = actorId;
	}

	public String getActorName() {
		return actorName;
	}

	public void setActorName(String actorName) {
		this.actorName = actorName;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
