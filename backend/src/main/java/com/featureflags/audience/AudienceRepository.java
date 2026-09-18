package com.featureflags.audience;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class AudienceRepository {

	private final MongoTemplate mongoTemplate;

	public AudienceRepository(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	public List<EndUser> search(String search, String plan, String country, int limit) {
		List<Criteria> conditions = new ArrayList<>();

		if (plan != null) {
			conditions.add(Criteria.where("attributes.plan").is(plan));
		}

		if (country != null) {
			conditions.add(Criteria.where("attributes.country").is(country));
		}

		if (search != null && !search.isEmpty()) {
			Pattern pattern = Pattern.compile(Pattern.quote(search), Pattern.CASE_INSENSITIVE);
			conditions.add(new Criteria().orOperator(Criteria.where("key").regex(pattern),
					Criteria.where("name").regex(pattern), Criteria.where("email").regex(pattern)));
		}

		return mongoTemplate.find(ordered(Query.query(all(conditions))).limit(limit), EndUser.class);
	}

	public List<EndUser> listAll() {
		return mongoTemplate.find(ordered(new Query()), EndUser.class);
	}

	public EndUser findByKey(String key) {
		return mongoTemplate.findOne(Query.query(Criteria.where("key").is(key)), EndUser.class);
	}

	public List<EndUser> findByKeys(List<String> keys) {
		return mongoTemplate.find(ordered(Query.query(Criteria.where("key").in(keys))), EndUser.class);
	}

	public long count() {
		return mongoTemplate.count(new Query(), EndUser.class);
	}

	public List<String> listPlans() {
		return mongoTemplate.findDistinct(new Query(), "attributes.plan", EndUser.class, String.class).stream().sorted()
				.toList();
	}

	public List<String> listCountries() {
		return mongoTemplate.findDistinct(new Query(), "attributes.country", EndUser.class, String.class).stream()
				.sorted().toList();
	}

	private Criteria all(List<Criteria> conditions) {
		return conditions.isEmpty() ? new Criteria() : new Criteria().andOperator(conditions.toArray(new Criteria[0]));
	}

	private Query ordered(Query query) {
		return query.with(Sort.by(Sort.Order.asc("name"), Sort.Order.asc("key")));
	}
}
