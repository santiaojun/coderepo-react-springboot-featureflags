package com.featureflags.segments;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.featureflags.flags.RuleClause;

public class SegmentRule {

	private String id;

	private String description = "";

	private List<RuleClause> clauses = new ArrayList<>();

	public Map<String, Object> toMap() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("id", id);
		body.put("description", description);
		body.put("clauses", clauses.stream().map(RuleClause::toMap).toList());

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

	public List<RuleClause> getClauses() {
		return clauses;
	}

	public void setClauses(List<RuleClause> clauses) {
		this.clauses = clauses;
	}
}
