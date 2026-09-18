package com.featureflags.flags;

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

@Document(collection = "flags")
public class Flag {

	public static final String BOOLEAN = "boolean";

	public static final String MULTIVARIATE = "multivariate";

	public static final String ACTIVE = "active";

	public static final String RETIRED = "retired";

	@Id
	private String id;

	private String key;

	private String name;

	private String description = "";

	private String kind = BOOLEAN;

	private List<Variation> variations = new ArrayList<>();

	private List<String> tags = new ArrayList<>();

	private boolean temporary = true;

	private String status = ACTIVE;

	@Field(targetType = FieldType.OBJECT_ID)
	private String maintainerId;

	@CreatedDate
	private Instant createdAt;

	@LastModifiedDate
	private Instant updatedAt;

	public boolean isRetired() {
		return RETIRED.equals(status);
	}

	public Variation findVariation(String variationId) {
		return variations.stream().filter(variation -> variation.getId().equals(variationId)).findFirst().orElse(null);
	}

	public boolean hasVariation(String variationId) {
		return variationId != null && findVariation(variationId) != null;
	}

	public String nextVariationId() {
		int highest = 0;

		for (Variation variation : variations) {
			String id = variation.getId();

			if (id != null && id.startsWith("v")) {
				try {
					highest = Math.max(highest, Integer.parseInt(id.substring(1)));
				} catch (NumberFormatException exception) {
					highest = Math.max(highest, variations.size());
				}
			}
		}

		return "v" + (highest + 1);
	}

	public Map<String, Object> toMap() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("_id", id);
		body.put("key", key);
		body.put("name", name);
		body.put("description", description);
		body.put("kind", kind);
		body.put("variations", variations.stream().map(Variation::toMap).toList());
		body.put("tags", new ArrayList<>(tags));
		body.put("temporary", temporary);
		body.put("status", status);
		body.put("maintainerId", maintainerId);
		body.put("createdAt", Dates.iso(createdAt));
		body.put("updatedAt", Dates.iso(updatedAt));

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

	public String getKind() {
		return kind;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

	public List<Variation> getVariations() {
		return variations;
	}

	public void setVariations(List<Variation> variations) {
		this.variations = variations;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public boolean isTemporary() {
		return temporary;
	}

	public void setTemporary(boolean temporary) {
		this.temporary = temporary;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getMaintainerId() {
		return maintainerId;
	}

	public void setMaintainerId(String maintainerId) {
		this.maintainerId = maintainerId;
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
}
