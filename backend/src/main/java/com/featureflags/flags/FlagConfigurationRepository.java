package com.featureflags.flags;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class FlagConfigurationRepository {

	private final MongoTemplate mongoTemplate;

	public FlagConfigurationRepository(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	public FlagConfiguration findOne(String flagId, String environmentId) {
		return mongoTemplate.findOne(Query.query(matching(flagId, environmentId)), FlagConfiguration.class);
	}

	public List<FlagConfiguration> listByEnvironment(String environmentId) {
		return mongoTemplate.find(Query.query(Criteria.where("environmentId").is(new ObjectId(environmentId))),
				FlagConfiguration.class);
	}

	public List<FlagConfiguration> listByFlagId(String flagId) {
		return mongoTemplate.find(Query.query(Criteria.where("flagId").is(new ObjectId(flagId))),
				FlagConfiguration.class);
	}

	public List<FlagConfiguration> listBySegmentId(String segmentId) {
		return mongoTemplate.find(Query.query(Criteria.where("rules.segmentIds").is(new ObjectId(segmentId))),
				FlagConfiguration.class);
	}

	public long countByEnvironment(String environmentId) {
		return mongoTemplate.count(Query.query(Criteria.where("environmentId").is(new ObjectId(environmentId))),
				FlagConfiguration.class);
	}

	public long countEnabledByEnvironment(String environmentId) {
		return mongoTemplate.count(
				Query.query(Criteria.where("environmentId").is(new ObjectId(environmentId)).and("enabled").is(true)),
				FlagConfiguration.class);
	}

	public long countRollingOutByEnvironment(String environmentId) {
		return mongoTemplate.count(Query.query(Criteria.where("environmentId").is(new ObjectId(environmentId))
				.and("enabled").is(true).and("fallthrough.kind").is(Serve.ROLLOUT)), FlagConfiguration.class);
	}

	public long countTargetedByEnvironment(String environmentId) {
		return mongoTemplate.count(Query.query(Criteria.where("environmentId").is(new ObjectId(environmentId))
				.and("enabled").is(true).and("rules.0").exists(true)), FlagConfiguration.class);
	}

	public FlagConfiguration save(FlagConfiguration configuration) {
		return mongoTemplate.save(configuration);
	}

	public void removeByFlagId(String flagId) {
		mongoTemplate.remove(Query.query(Criteria.where("flagId").is(new ObjectId(flagId))), FlagConfiguration.class);
	}

	private Criteria matching(String flagId, String environmentId) {
		return Criteria.where("flagId").is(new ObjectId(flagId)).and("environmentId")
				.is(new ObjectId(environmentId));
	}
}
