package com.featureflags.seed;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import com.featureflags.audience.EndUser;
import com.featureflags.auth.WorkspaceAccount;
import com.featureflags.environments.Environment;
import com.featureflags.flags.Flag;
import com.featureflags.flags.FlagConfiguration;
import com.featureflags.flags.FlagSummaries;
import com.featureflags.flags.FlagTarget;
import com.featureflags.flags.Rollout;
import com.featureflags.flags.RolloutWeight;
import com.featureflags.flags.RuleClause;
import com.featureflags.flags.Serve;
import com.featureflags.flags.TargetRule;
import com.featureflags.flags.Variation;
import com.featureflags.history.ChangeEntry;
import com.featureflags.history.ChangeRecord;
import com.featureflags.members.Member;
import com.featureflags.segments.Segment;
import com.featureflags.segments.SegmentRule;

import at.favre.lib.crypto.bcrypt.BCrypt;

@SpringBootApplication(scanBasePackages = "com.featureflags")
@EntityScan("com.featureflags")
public class SeedRunner {

	private static final String SEPARATOR = "========================================";

	private static final String DEVELOPMENT = "development";

	public static void main(String[] args) {
		var context = new SpringApplicationBuilder(SeedRunner.class).web(WebApplicationType.NONE)
				.logStartupInfo(false).run(args);

		try {
			seed(context.getBean(MongoTemplate.class));
		} catch (RuntimeException exception) {
			System.err.println();
			System.err.println("Seeding failed: " + exception.getMessage());
			exception.printStackTrace();
			SpringApplication.exit(context, () -> 1);
			System.exit(1);
		}

		System.out.println("Disconnected from MongoDB");
		SpringApplication.exit(context, () -> 0);
		System.exit(0);
	}

	static void seed(MongoTemplate mongoTemplate) {
		System.out.println(SEPARATOR);
		System.out.println("Database Seeding");
		System.out.println(SEPARATOR);
		System.out.println();
		System.out.println("Connecting to MongoDB...");
		mongoTemplate.executeCommand(new Document("ping", 1));
		System.out.println("Connected to MongoDB");

		clearDatabase(mongoTemplate);

		Map<String, Environment> environments = seedEnvironments(mongoTemplate);
		Map<String, Member> members = seedMembers(mongoTemplate);
		seedAccounts(mongoTemplate, members);
		seedAudience(mongoTemplate);
		Map<String, Segment> segments = seedSegments(mongoTemplate, environments);
		Map<String, Flag> flags = seedFlags(mongoTemplate, members);
		seedConfigurations(mongoTemplate, environments, members, segments, flags);
		seedRetirements(mongoTemplate, members, flags);

		report(mongoTemplate);
	}

	private static void clearDatabase(MongoTemplate mongoTemplate) {
		System.out.println("Clearing existing collections...");

		for (Class<?> type : List.of(ChangeRecord.class, FlagConfiguration.class, Flag.class, Segment.class,
				EndUser.class, Environment.class, Member.class, WorkspaceAccount.class)) {
			mongoTemplate.remove(new Query(), type);
		}
	}

	private static Map<String, Environment> seedEnvironments(MongoTemplate mongoTemplate) {
		System.out.println();
		System.out.println("Seeding environments...");

		Map<String, Environment> byKey = new LinkedHashMap<>();

		for (SeedData.EnvironmentRow row : SeedData.environmentRows()) {
			Environment environment = new Environment();
			environment.setId(new ObjectId().toHexString());
			environment.setKey(row.key());
			environment.setName(row.name());
			environment.setDescription(row.description());
			environment.setColor(row.color());
			environment.setCritical(row.critical());
			environment.setSortOrder(row.sortOrder());
			byKey.put(row.key(), mongoTemplate.insert(environment));
		}

		System.out.println("  Created " + byKey.size() + " environments");

		return byKey;
	}

	private static Map<String, Member> seedMembers(MongoTemplate mongoTemplate) {
		System.out.println();
		System.out.println("Seeding members...");

		Map<String, Member> byEmail = new LinkedHashMap<>();

		for (SeedData.MemberRow row : SeedData.memberRows()) {
			Member member = new Member();
			member.setId(new ObjectId().toHexString());
			member.setName(row.name());
			member.setEmail(row.email());
			member.setRole(row.role());
			member.setTitle(row.title());
			member.setAvatarColor(row.avatarColor());
			member.setSortOrder(row.sortOrder());
			byEmail.put(row.email(), mongoTemplate.insert(member));
		}

		System.out.println("  Created " + byEmail.size() + " members");

		return byEmail;
	}

	private static void seedAccounts(MongoTemplate mongoTemplate, Map<String, Member> members) {
		System.out.println();
		System.out.println("Seeding sign-in accounts...");

		String passwordHash = BCrypt.with(BCrypt.Version.VERSION_2B).hashToString(SeedData.PASSWORD_ROUNDS,
				SeedData.DEMO_PASSWORD.toCharArray());
		List<String> allowedProfileIds = members.values().stream().map(Member::getId).toList();

		for (Member member : members.values()) {
			WorkspaceAccount account = new WorkspaceAccount();
			account.setId(new ObjectId().toHexString());
			account.setName(member.getName());
			account.setEmail(member.getEmail());
			account.setPasswordHash(passwordHash);
			account.setAllowedProfileIds(new ArrayList<>(allowedProfileIds));
			mongoTemplate.insert(account);
		}

		System.out.println("  Created " + members.size() + " accounts, each able to open any of the " + members.size()
				+ " member profiles");
	}

	private static void seedAudience(MongoTemplate mongoTemplate) {
		System.out.println();
		System.out.println("Seeding audience...");

		int created = 0;

		for (SeedData.EndUserRow row : SeedData.endUserRows()) {
			EndUser user = new EndUser();
			user.setId(new ObjectId().toHexString());
			user.setKey(row.key());
			user.setName(row.name());
			user.setEmail(row.email());
			Map<String, Object> attributes = new LinkedHashMap<>();
			attributes.put("plan", row.plan());
			attributes.put("country", row.country());
			attributes.put("signupYear", row.signupYear());
			attributes.put("betaOptIn", row.betaOptIn());
			attributes.put("deviceType", row.deviceType());
			user.setAttributes(attributes);
			user.setLastSeenAt(daysAgo(row.lastSeenDaysAgo()));
			mongoTemplate.insert(user);
			created += 1;
		}

		System.out.println("  Created " + created + " audience members");
	}

	private static Map<String, Segment> seedSegments(MongoTemplate mongoTemplate,
			Map<String, Environment> environments) {
		System.out.println();
		System.out.println("Seeding segments...");

		Map<String, Segment> byEnvironmentAndKey = new LinkedHashMap<>();

		for (Environment environment : environments.values()) {
			for (SeedData.SegmentRow row : SeedData.segmentRows()) {
				Segment segment = new Segment();
				segment.setId(new ObjectId().toHexString());
				segment.setKey(row.key());
				segment.setName(row.name());
				segment.setDescription(row.description());
				segment.setEnvironmentId(environment.getId());
				segment.setEnvironmentKey(environment.getKey());
				segment.setIncludedUserKeys(new ArrayList<>(row.includedUserKeys()));
				segment.setExcludedUserKeys(new ArrayList<>(row.excludedUserKeys()));

				if (!row.clauses().isEmpty()) {
					SegmentRule rule = new SegmentRule();
					rule.setId("r1");
					rule.setDescription(row.ruleDescription());
					rule.setClauses(clausesOf(row.clauses()));
					segment.setRules(List.of(rule));
				}

				byEnvironmentAndKey.put(segmentReference(environment.getKey(), row.key()),
						mongoTemplate.insert(segment));
			}
		}

		System.out.println("  Created " + byEnvironmentAndKey.size() + " segments across " + environments.size()
				+ " environments");

		return byEnvironmentAndKey;
	}

	private static Map<String, Flag> seedFlags(MongoTemplate mongoTemplate, Map<String, Member> members) {
		System.out.println();
		System.out.println("Seeding flags...");

		Map<String, Flag> byKey = new LinkedHashMap<>();

		for (SeedData.FlagRow row : SeedData.flagRows()) {
			Flag flag = new Flag();
			flag.setId(new ObjectId().toHexString());
			flag.setKey(row.key());
			flag.setName(row.name());
			flag.setDescription(row.description());
			flag.setKind(row.kind());
			flag.setTags(new ArrayList<>(row.tags()));
			flag.setTemporary(row.temporary());
			flag.setStatus(row.retired() ? Flag.RETIRED : Flag.ACTIVE);
			flag.setMaintainerId(members.get(row.maintainerEmail()).getId());
			flag.setVariations(variationsOf(row.variations()));
			byKey.put(row.key(), mongoTemplate.insert(flag));
		}

		System.out.println("  Created " + byKey.size() + " flags");

		return byKey;
	}

	private static void seedConfigurations(MongoTemplate mongoTemplate, Map<String, Environment> environments,
			Map<String, Member> members, Map<String, Segment> segments, Map<String, Flag> flags) {
		System.out.println();
		System.out.println("Seeding flag configurations and change history...");

		Map<String, SeedData.ConfigRow> scripted = new LinkedHashMap<>();

		for (SeedData.ConfigRow row : SeedData.configRows()) {
			scripted.put(configReference(row.flagKey(), row.environmentKey()), row);
		}

		int configurations = 0;
		int records = 0;

		for (Flag flag : flags.values()) {
			for (Environment environment : environments.values()) {
				SeedData.ConfigRow row = scripted.get(configReference(flag.getKey(), environment.getKey()));
				List<SeedData.StateRow> states = row == null ? List.of(defaultState(flag, environment))
						: row.states();
				records += replay(mongoTemplate, flag, environment, states, members, segments);
				configurations += 1;
			}
		}

		System.out.println("  Created " + configurations + " configurations and " + records + " change records");
	}

	private static SeedData.StateRow defaultState(Flag flag, Environment environment) {
		List<Variation> variations = flag.getVariations();
		String fallthroughVariationId = variations.get(0).getId();
		String offVariationId = variations.get(variations.size() - 1).getId();

		return new SeedData.StateRow(37, SeedData.memberRows().get(0).email(), ChangeRecord.FLAG_CREATED, "", null,
				DEVELOPMENT.equals(environment.getKey()), offVariationId, fallthroughVariationId, null, List.of(),
				List.of());
	}

	private static int replay(MongoTemplate mongoTemplate, Flag flag, Environment environment,
			List<SeedData.StateRow> states, Map<String, Member> members, Map<String, Segment> segments) {
		FlagConfiguration previous = null;
		FlagConfiguration current = null;
		int version = 0;

		for (SeedData.StateRow state : states) {
			version += 1;
			Member actor = members.get(state.actorEmail());
			Instant at = daysAgo(state.daysAgo());
			current = buildConfiguration(flag, environment, state, segments);
			current.setVersion(version);
			current.setLastModifiedAt(at);
			current.setLastModifiedBy(actor.getId());
			current.setLastModifiedByName(actor.getName());

			ChangeRecord record = new ChangeRecord();
			record.setId(new ObjectId().toHexString());
			record.setFlagId(flag.getId());
			record.setFlagKey(flag.getKey());
			record.setFlagName(flag.getName());
			record.setEnvironmentId(environment.getId());
			record.setEnvironmentKey(environment.getKey());
			record.setVersion(version);
			record.setAction(state.action());
			record.setSummary(summaryFor(flag, environment, state, current));
			record.setChanges(diff(flag, previous, current));
			record.setSnapshot(current.toSnapshot());
			record.setRestoredFromVersion(state.restoredFromVersion());
			record.setComment(state.comment());
			record.setActorId(actor.getId());
			record.setActorName(actor.getName());
			record.setCreatedAt(at);
			mongoTemplate.insert(record);

			previous = current;
		}

		current.setId(new ObjectId().toHexString());
		current.setCreatedAt(daysAgo(states.get(0).daysAgo()));
		mongoTemplate.insert(current);

		return states.size();
	}

	private static FlagConfiguration buildConfiguration(Flag flag, Environment environment,
			SeedData.StateRow state, Map<String, Segment> segments) {
		FlagConfiguration configuration = new FlagConfiguration();
		configuration.setFlagId(flag.getId());
		configuration.setEnvironmentId(environment.getId());
		configuration.setFlagKey(flag.getKey());
		configuration.setEnvironmentKey(environment.getKey());
		configuration.setEnabled(state.enabled());
		configuration.setOffVariationId(state.offVariationId());
		configuration.setFallthrough(serveOf(state.fallthroughVariationId(), state.fallthroughWeights()));
		configuration.setTargets(targetsOf(state.targets()));
		configuration.setRules(rulesOf(state.rules(), environment.getKey(), segments));

		return configuration;
	}

	private static Serve serveOf(String variationId, List<SeedData.WeightRow> weights) {
		if (weights == null || weights.isEmpty()) {
			return Serve.ofVariation(variationId);
		}

		Rollout rollout = new Rollout();
		List<RolloutWeight> entries = new ArrayList<>();

		for (SeedData.WeightRow weight : weights) {
			RolloutWeight entry = new RolloutWeight();
			entry.setVariationId(weight.variationId());
			entry.setWeight(weight.weight());
			entries.add(entry);
		}

		rollout.setWeights(entries);

		return Serve.ofRollout(rollout);
	}

	private static List<FlagTarget> targetsOf(List<SeedData.TargetRow> rows) {
		List<FlagTarget> targets = new ArrayList<>();

		for (SeedData.TargetRow row : rows) {
			FlagTarget target = new FlagTarget();
			target.setVariationId(row.variationId());
			target.setUserKeys(new ArrayList<>(row.userKeys()));
			targets.add(target);
		}

		return targets;
	}

	private static List<TargetRule> rulesOf(List<SeedData.RuleRow> rows, String environmentKey,
			Map<String, Segment> segments) {
		List<TargetRule> rules = new ArrayList<>();

		for (int index = 0; index < rows.size(); index += 1) {
			SeedData.RuleRow row = rows.get(index);
			List<String> segmentIds = new ArrayList<>();

			for (String segmentKey : row.segmentKeys()) {
				Segment segment = segments.get(segmentReference(environmentKey, segmentKey));

				if (segment == null) {
					throw new IllegalStateException(
							"Seed rule references unknown segment " + segmentKey + " in " + environmentKey);
				}

				segmentIds.add(segment.getId());
			}

			TargetRule rule = new TargetRule();
			rule.setId("r" + (index + 1));
			rule.setDescription(row.description());
			rule.setSegmentIds(segmentIds);
			rule.setClauses(clausesOf(row.clauses()));
			rule.setServe(serveOf(row.serveVariationId(), row.serveWeights()));
			rules.add(rule);
		}

		return rules;
	}

	private static List<RuleClause> clausesOf(List<SeedData.ClauseRow> rows) {
		List<RuleClause> clauses = new ArrayList<>();

		for (SeedData.ClauseRow row : rows) {
			RuleClause clause = new RuleClause();
			clause.setAttribute(row.attribute());
			clause.setOp(row.op());
			clause.setValues(new ArrayList<>(row.values()));
			clause.setNegate(row.negate());
			clauses.add(clause);
		}

		return clauses;
	}

	private static List<Variation> variationsOf(List<SeedData.VariationRow> rows) {
		List<Variation> variations = new ArrayList<>();

		for (int index = 0; index < rows.size(); index += 1) {
			SeedData.VariationRow row = rows.get(index);
			Variation variation = new Variation();
			variation.setId("v" + (index + 1));
			variation.setName(row.name());
			variation.setValue(row.value());
			variation.setDescription(row.description());
			variations.add(variation);
		}

		return variations;
	}

	private static String summaryFor(Flag flag, Environment environment, SeedData.StateRow state,
			FlagConfiguration configuration) {
		String serving = configuration.isEnabled()
				? FlagSummaries.describeServe(flag, configuration.getFallthrough())
				: FlagSummaries.variationName(flag, configuration.getOffVariationId());

		return switch (state.action()) {
			case ChangeRecord.FLAG_CREATED -> "Created serving " + serving;
			case ChangeRecord.TOGGLED -> "Turned " + (configuration.isEnabled() ? "on" : "off") + " in "
					+ environment.getName() + ", serving " + serving;
			case ChangeRecord.ROLLOUT_UPDATED -> "Rollout set to "
					+ FlagSummaries.describeServe(flag, configuration.getFallthrough()) + " in "
					+ environment.getName();
			case ChangeRecord.ROLLED_BACK -> "Rolled back to version " + state.restoredFromVersion() + " in "
					+ environment.getName();
			default -> "Targeting updated in " + environment.getName();
		};
	}

	private static List<ChangeEntry> diff(Flag flag, FlagConfiguration previous, FlagConfiguration current) {
		List<ChangeEntry> entries = new ArrayList<>();

		if (previous == null) {
			return entries;
		}

		if (previous.isEnabled() != current.isEnabled()) {
			entries.add(ChangeEntry.of("enabled", "Flag state", previous.isEnabled() ? "on" : "off",
					current.isEnabled() ? "on" : "off"));
		}

		if (!String.valueOf(previous.getOffVariationId()).equals(String.valueOf(current.getOffVariationId()))) {
			entries.add(ChangeEntry.of("offVariationId", "Serves when off",
					FlagSummaries.variationName(flag, previous.getOffVariationId()),
					FlagSummaries.variationName(flag, current.getOffVariationId())));
		}

		String previousTargets = FlagSummaries.describeTargets(flag, previous.getTargets());
		String currentTargets = FlagSummaries.describeTargets(flag, current.getTargets());

		if (!previousTargets.equals(currentTargets)) {
			entries.add(ChangeEntry.of("targets", "Individual targets", previousTargets, currentTargets));
		}

		List<String> previousRules = ruleLabels(flag, previous.getRules());
		List<String> currentRules = ruleLabels(flag, current.getRules());

		if (!previousRules.equals(currentRules)) {
			entries.add(ChangeEntry.of("rules", "Targeting rules", previousRules, currentRules));
		}

		String previousServe = FlagSummaries.describeServe(flag, previous.getFallthrough());
		String currentServe = FlagSummaries.describeServe(flag, current.getFallthrough());

		if (!previousServe.equals(currentServe)) {
			entries.add(ChangeEntry.of("fallthrough", "Serves everyone else", previousServe, currentServe));
		}

		return entries;
	}

	private static List<String> ruleLabels(Flag flag, List<TargetRule> rules) {
		List<String> labels = new ArrayList<>();

		for (TargetRule rule : rules) {
			String label = rule.getDescription().isEmpty() ? "Rule " + rule.getId() : rule.getDescription();
			labels.add(label + " serves " + FlagSummaries.describeServe(flag, rule.getServe()));
		}

		return labels;
	}

	private static void seedRetirements(MongoTemplate mongoTemplate, Map<String, Member> members,
			Map<String, Flag> flags) {
		int retired = 0;

		for (SeedData.FlagRow row : SeedData.flagRows()) {
			if (!row.retired()) {
				continue;
			}

			Flag flag = flags.get(row.key());
			Member actor = members.get(row.maintainerEmail());
			ChangeRecord record = new ChangeRecord();
			record.setId(new ObjectId().toHexString());
			record.setFlagId(flag.getId());
			record.setFlagKey(flag.getKey());
			record.setFlagName(flag.getName());
			record.setVersion(0);
			record.setAction(ChangeRecord.FLAG_RETIRED);
			record.setSummary("Flag retired");
			record.setChanges(List.of(ChangeEntry.of("status", "Status", Flag.ACTIVE, Flag.RETIRED)));
			record.setComment("Reporting migration finished.");
			record.setActorId(actor.getId());
			record.setActorName(actor.getName());
			record.setCreatedAt(daysAgo(row.retiredDaysAgo()));
			mongoTemplate.insert(record);
			retired += 1;
		}

		if (retired > 0) {
			System.out.println("  Recorded " + retired + " flag retirement" + (retired == 1 ? "" : "s"));
		}
	}

	private static Instant daysAgo(int days) {
		return Instant.now().minus(Duration.ofDays(days));
	}

	private static String configReference(String flagKey, String environmentKey) {
		return flagKey + "@" + environmentKey;
	}

	private static String segmentReference(String environmentKey, String segmentKey) {
		return environmentKey + "/" + segmentKey;
	}

	private static void report(MongoTemplate mongoTemplate) {
		System.out.println();
		System.out.println(SEPARATOR);
		System.out.println("Seeding completed successfully!");
		System.out.println(SEPARATOR);
		System.out.println();
		System.out.println("Collection counts:");
		System.out.println("  Environments:   " + mongoTemplate.count(new Query(), Environment.class));
		System.out.println("  Members:        " + mongoTemplate.count(new Query(), Member.class));
		System.out.println("  Accounts:       " + mongoTemplate.count(new Query(), WorkspaceAccount.class));
		System.out.println("  Audience:       " + mongoTemplate.count(new Query(), EndUser.class));
		System.out.println("  Segments:       " + mongoTemplate.count(new Query(), Segment.class));
		System.out.println("  Flags:          " + mongoTemplate.count(new Query(), Flag.class));
		System.out.println("  Configurations: " + mongoTemplate.count(new Query(), FlagConfiguration.class));
		System.out.println("  Change records: " + mongoTemplate.count(new Query(), ChangeRecord.class));
		System.out.println();
		System.out.println("Demo accounts:");

		for (SeedData.MemberRow row : SeedData.memberRows()) {
			System.out.println("  Email: " + row.email() + " | Password: " + SeedData.DEMO_PASSWORD);
		}

		System.out.println();
		System.out.println("Any account can open any member profile. History is seeded relative to today.");
		System.out.println(SEPARATOR);
		System.out.println();
	}
}
