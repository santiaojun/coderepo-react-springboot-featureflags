package com.featureflags.flags;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class FlagRepository {

	private final MongoTemplate mongoTemplate;

	public FlagRepository(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	public List<Flag> list(String search, String tag, String status) {
		List<Criteria> conditions = new ArrayList<>();

		if (status != null) {
			conditions.add(Criteria.where("status").is(status));
		}

		if (tag != null) {
			conditions.add(Criteria.where("tags").is(tag));
		}

		if (search != null && !search.isEmpty()) {
			Pattern pattern = expression(search);
			conditions.add(new Criteria().orOperator(Criteria.where("key").regex(pattern),
					Criteria.where("name").regex(pattern), Criteria.where("description").regex(pattern)));
		}

		return mongoTemplate.find(ordered(Query.query(all(conditions))), Flag.class);
	}

	public List<Flag> listAll() {
		return mongoTemplate.find(ordered(new Query()), Flag.class);
	}

	public List<String> listTags() {
		return mongoTemplate.findDistinct(new Query(), "tags", Flag.class, String.class).stream().sorted().toList();
	}

	public Flag findByKey(String key) {
		return mongoTemplate.findOne(Query.query(Criteria.where("key").is(key)), Flag.class);
	}

	public Flag findById(String id) {
		return mongoTemplate.findById(new ObjectId(id), Flag.class);
	}

	public boolean existsByKey(String key) {
		return mongoTemplate.exists(Query.query(Criteria.where("key").is(key)), Flag.class);
	}

	public long countByStatus(String status) {
		return mongoTemplate.count(Query.query(Criteria.where("status").is(status)), Flag.class);
	}

	public Flag save(Flag flag) {
		return mongoTemplate.save(flag);
	}

	public void remove(String id) {
		mongoTemplate.remove(Query.query(Criteria.where("_id").is(new ObjectId(id))), Flag.class);
	}

	private Pattern expression(String value) {
		return Pattern.compile(Pattern.quote(value), Pattern.CASE_INSENSITIVE);
	}

	private Criteria all(List<Criteria> conditions) {
		return conditions.isEmpty() ? new Criteria() : new Criteria().andOperator(conditions.toArray(new Criteria[0]));
	}

	private Query ordered(Query query) {
		return query.with(Sort.by(Sort.Order.asc("key")));
	}
}
