package com.featureflags.members;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class MemberRepository {

	private final MongoTemplate mongoTemplate;

	public MemberRepository(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	public List<Member> list(List<String> ids) {
		Criteria criteria = new Criteria();

		if (ids != null) {
			criteria = Criteria.where("_id").in(objectIds(ids));
		}

		return mongoTemplate.find(ordered(Query.query(criteria)), Member.class);
	}

	public Member findById(String id) {
		return mongoTemplate.findById(new ObjectId(id), Member.class);
	}

	private Query ordered(Query query) {
		return query.with(Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("name")));
	}

	private List<ObjectId> objectIds(List<String> ids) {
		return ids.stream().map(ObjectId::new).toList();
	}
}
