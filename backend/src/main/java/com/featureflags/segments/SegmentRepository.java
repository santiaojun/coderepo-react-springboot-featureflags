package com.featureflags.segments;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class SegmentRepository {

	private final MongoTemplate mongoTemplate;

	public SegmentRepository(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	public List<Segment> listByEnvironment(String environmentId) {
		Query query = Query.query(Criteria.where("environmentId").is(new ObjectId(environmentId)));

		return mongoTemplate.find(query.with(Sort.by(Sort.Order.asc("name"))), Segment.class);
	}

	public Segment findByKey(String key, String environmentId) {
		return mongoTemplate.findOne(
				Query.query(Criteria.where("key").is(key).and("environmentId").is(new ObjectId(environmentId))),
				Segment.class);
	}

	public List<Segment> findByIds(List<String> ids) {
		return mongoTemplate.find(Query.query(Criteria.where("_id").in(objectIds(ids))), Segment.class);
	}

	public long countByEnvironment(String environmentId) {
		return mongoTemplate.count(Query.query(Criteria.where("environmentId").is(new ObjectId(environmentId))),
				Segment.class);
	}

	public Segment save(Segment segment) {
		return mongoTemplate.save(segment);
	}

	public void remove(String id) {
		mongoTemplate.remove(Query.query(Criteria.where("_id").is(new ObjectId(id))), Segment.class);
	}

	private List<ObjectId> objectIds(List<String> ids) {
		return ids.stream().map(ObjectId::new).toList();
	}
}
