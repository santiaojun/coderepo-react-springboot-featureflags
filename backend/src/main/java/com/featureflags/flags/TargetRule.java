package com.featureflags.flags;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

public class TargetRule {

	private String id;

	private String description = "";

	@Field(targetType = FieldType.OBJECT_ID)
	private List<String> segmentIds = new ArrayList<>();

	private List<RuleClause> clauses = new ArrayList<>();

	private Serve serve = new Serve();

	public Map<String, Object> toMap() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("id", id);
		body.put("description", description);
		body.put("segmentIds", new ArrayList<>(segmentIds));
		body.put("clauses", clauses.stream().map(RuleClause::toMap).toList());
		body.put("serve", serve.toMap());

		return body;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<String> getSegmentIds() {
		return segmentIds;
	}

	public void setSegmentIds(List<String> segmentIds) {
		this.segmentIds = segmentIds;
	}

	public List<RuleClause> getClauses() {
		return clauses;
	}

	public void setClauses(List<RuleClause> clauses) {
		this.clauses = clauses;
	}

	public Serve getServe() {
		return serve;
	}

	public void setServe(Serve serve) {
		this.serve = serve;
	}
}
