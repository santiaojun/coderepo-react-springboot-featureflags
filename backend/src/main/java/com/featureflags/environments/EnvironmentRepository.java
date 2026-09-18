package com.featureflags.environments;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class EnvironmentRepository {

	private final MongoTemplate mongoTemplate;

	public EnvironmentRepository(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	public List<Environment> list() {
		return mongoTemplate.find(new Query().with(Sort.by(Sort.Order.asc("sortOrder"))), Environment.class);
	}

	public Environment findByKey(String key) {
		return mongoTemplate.findOne(Query.query(Criteria.where("key").is(key)), Environment.class);
	}

	public Environment findById(String id) {
		return mongoTemplate.findById(new ObjectId(id), Environment.class);
	}
}
