package com.featureflags.history;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class ChangeRecordRepository {

	private final MongoTemplate mongoTemplate;

	public ChangeRecordRepository(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	public List<ChangeRecord> list(String flagKey, String environmentId, String actorId, String action, int limit) {
		List<Criteria> conditions = new ArrayList<>();

		if (flagKey != null) {
			conditions.add(Criteria.where("flagKey").is(flagKey));
		}

		if (environmentId != null) {
			// Flag lifecycle records (create, rename, retire) are not environment
			// scoped, so they belong in every environment's timeline.
			conditions.add(new Criteria().orOperator(
					Criteria.where("environmentId").is(new ObjectId(environmentId)),
					Criteria.where("environmentId").is(null)));
		}

		if (actorId != null) {
			conditions.add(Criteria.where("actorId").is(new ObjectId(actorId)));
		}

		if (action != null) {
			conditions.add(Criteria.where("action").is(action));
		}

		return mongoTemplate.find(ordered(Query.query(all(conditions))).limit(limit), ChangeRecord.class);
	}

	public ChangeRecord findVersion(String flagId, String environmentId, int version) {
		return mongoTemplate.findOne(Query.query(Criteria.where("flagId").is(new ObjectId(flagId)).and("environmentId")
				.is(new ObjectId(environmentId)).and("version").is(version)), ChangeRecord.class);
	}

	public long countByFlagId(String flagId) {
		return mongoTemplate.count(Query.query(Criteria.where("flagId").is(new ObjectId(flagId))), ChangeRecord.class);
	}

	public ChangeRecord save(ChangeRecord record) {
		return mongoTemplate.save(record);
	}

	private Criteria all(List<Criteria> conditions) {
		return conditions.isEmpty() ? new Criteria() : new Criteria().andOperator(conditions.toArray(new Criteria[0]));
	}

	private Query ordered(Query query) {
		return query.with(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("version")));
	}
}
