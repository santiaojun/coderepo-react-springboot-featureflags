package com.featureflags.members;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import com.featureflags.shared.Dates;

@Document(collection = "members")
public class Member {

	@Id
	private String id;

	private String name;

	private String email;

	private String avatarColor;

	private String role = "engineer";

	private String title = "";

	private int sortOrder;

	@CreatedDate
	private Instant createdAt;

	@LastModifiedDate
	private Instant updatedAt;

	public Map<String, Object> toMap() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("_id", id);
		body.put("name", name);
		body.put("email", email);
		body.put("avatarColor", avatarColor);
		body.put("role", role);
		body.put("title", title);
		body.put("sortOrder", sortOrder);
		body.put("createdAt", Dates.iso(createdAt));
		body.put("updatedAt", Dates.iso(updatedAt));

		return body;
	}

	public Map<String, Object> toSummaryMap() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("_id", id);
		body.put("name", name);
		body.put("email", email);
		body.put("avatarColor", avatarColor);
		body.put("role", role);

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

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getAvatarColor() {
		return avatarColor;
	}

	public void setAvatarColor(String avatarColor) {
		this.avatarColor = avatarColor;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
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
