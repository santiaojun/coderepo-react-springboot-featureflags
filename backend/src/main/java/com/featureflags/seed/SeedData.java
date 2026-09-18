package com.featureflags.seed;

import java.util.List;

/**
 * The deterministic baseline the application starts from.
 *
 * <p>Configuration history is declared as an ordered list of states rather than
 * as finished change records. {@link SeedRunner} replays those states, writing
 * one version and one audit entry per step, so the seeded audit trail is
 * produced the same way a live change is and the final state always matches the
 * last record's snapshot.
 */
public final class SeedData {

	public static final String DEMO_PASSWORD = "flagpost2026";

	public static final int PASSWORD_ROUNDS = 10;

	public record EnvironmentRow(String key, String name, String description, String color, boolean critical,
			int sortOrder) {
	}

	public record MemberRow(String name, String email, String role, String title, String avatarColor, int sortOrder) {
	}

	public record EndUserRow(String key, String name, String email, String plan, String country, int signupYear,
			boolean betaOptIn, String deviceType, int lastSeenDaysAgo) {
	}

	public record ClauseRow(String attribute, String op, List<Object> values, boolean negate) {
	}

	public record SegmentRow(String key, String name, String description, List<String> includedUserKeys,
			List<String> excludedUserKeys, String ruleDescription, List<ClauseRow> clauses) {
	}

	public record VariationRow(String name, Object value, String description) {
	}

	public record FlagRow(String key, String name, String description, String kind, List<String> tags,
			boolean temporary, String maintainerEmail, boolean retired, int retiredDaysAgo,
			List<VariationRow> variations) {
	}

	public record WeightRow(String variationId, int weight) {
	}

	public record TargetRow(String variationId, List<String> userKeys) {
	}

	public record RuleRow(String description, List<String> segmentKeys, List<ClauseRow> clauses,
			String serveVariationId, List<WeightRow> serveWeights) {
	}

	public record StateRow(int daysAgo, String actorEmail, String action, String comment, Integer restoredFromVersion,
			boolean enabled, String offVariationId, String fallthroughVariationId, List<WeightRow> fallthroughWeights,
			List<TargetRow> targets, List<RuleRow> rules) {
	}

	public record ConfigRow(String flagKey, String environmentKey, List<StateRow> states) {
	}

	private static final String ADMIN = "amara.okafor@flagpost.dev";

	private static final String CHECKOUT = "priya.raghunathan@flagpost.dev";

	private static final String GROWTH = "diego.salazar@flagpost.dev";

	private static final String MOBILE = "lin.zhao@flagpost.dev";

	private static final String SUPPORT = "noor.haddad@flagpost.dev";

	public static List<EnvironmentRow> environmentRows() {
		return List.of(
				new EnvironmentRow("development", "Development", "Local and branch work. Safe to break.", "#0B57D0",
						false, 0),
				new EnvironmentRow("staging", "Staging", "Release candidate testing against production-like data.",
						"#E37400", false, 1),
				new EnvironmentRow("production", "Production", "Live customer traffic. Changes are confirmed twice.",
						"#B3261E", true, 2));
	}

	public static List<MemberRow> memberRows() {
		return List.of(
				new MemberRow("Amara Okafor", ADMIN, "admin", "Platform engineering lead", "#0B57D0", 0),
				new MemberRow("Priya Raghunathan", CHECKOUT, "engineer", "Checkout squad", "#7B1FA2", 1),
				new MemberRow("Diego Salazar", GROWTH, "engineer", "Growth experiments", "#137333", 2),
				new MemberRow("Lin Zhao", MOBILE, "engineer", "Mobile platform", "#B3261E", 3),
				new MemberRow("Noor Haddad", SUPPORT, "viewer", "Support operations", "#E37400", 4));
	}

	public static List<EndUserRow> endUserRows() {
		return List.of(
				new EndUserRow("user-1001", "Wren Halvorsen", "wren.halvorsen@northwind.example", "enterprise", "US",
						2022, false, "web", 1),
				new EndUserRow("user-1002", "Tomas Lindqvist", "tomas.lindqvist@arborplan.example", "pro", "DE", 2023,
						true, "web", 2),
				new EndUserRow("user-1003", "Adaeze Nwosu", "adaeze.nwosu@meridianlabs.example", "enterprise", "NG",
						2022, true, "web", 1),
				new EndUserRow("user-1004", "Hugo Marchetti", "hugo.marchetti@vellum.example", "free", "FR", 2025,
						false, "ios", 5),
				new EndUserRow("user-1005", "Sana Qureshi", "sana.qureshi@brightloom.example", "pro", "IN", 2024,
						false, "android", 3),
				new EndUserRow("user-1006", "Emil Nakamura", "emil.nakamura@tidalworks.example", "enterprise", "JP",
						2021, false, "web", 1),
				new EndUserRow("user-1007", "Rosa Delgado", "rosa.delgado@casaverde.example", "pro", "MX", 2024, true,
						"web", 4),
				new EndUserRow("user-1008", "Fionn Gallagher", "fionn.gallagher@stonebridge.example", "free", "CA",
						2026, false, "web", 2),
				new EndUserRow("user-1009", "Mei Lin Chow", "meilin.chow@lanternco.example", "pro", "AU", 2023, false,
						"ios", 6),
				new EndUserRow("user-1010", "Yusuf Demir", "yusuf.demir@aksaray.example", "enterprise", "DE", 2022,
						false, "web", 1),
				new EndUserRow("user-1011", "Camila Ferreira", "camila.ferreira@bosquetec.example", "pro", "BR", 2024,
						true, "android", 2),
				new EndUserRow("user-1012", "Oskar Baranowski", "oskar.baranowski@wisla.example", "free", "DE", 2025,
						false, "web", 9),
				new EndUserRow("user-1013", "Nadia Farouk", "nadia.farouk@dunesoft.example", "enterprise", "FR", 2021,
						true, "web", 1),
				new EndUserRow("user-1014", "Grant Whitfield", "grant.whitfield@orchardhill.example", "pro", "US",
						2023, false, "web", 3),
				new EndUserRow("user-1015", "Ines Almeida", "ines.almeida@atlantico.example", "free", "BR", 2026,
						false, "ios", 1),
				new EndUserRow("user-1016", "Kofi Mensah", "kofi.mensah@goldcoastdata.example", "pro", "NG", 2024,
						false, "android", 4),
				new EndUserRow("user-1017", "Beatrix Vogel", "beatrix.vogel@hafenwerk.example", "enterprise", "DE",
						2022, true, "web", 1),
				new EndUserRow("user-1018", "Arjun Pillai", "arjun.pillai@keralasoft.example", "pro", "IN", 2023,
						true, "web", 2),
				new EndUserRow("user-1019", "Solveig Brekke", "solveig.brekke@fjordline.example", "free", "AU", 2025,
						false, "web", 12),
				new EndUserRow("user-1020", "Marcus Oyelaran", "marcus.oyelaran@lagosgrid.example", "pro", "NG", 2024,
						false, "android", 3),
				new EndUserRow("user-1021", "Chiara Rossetti", "chiara.rossetti@viacorta.example", "enterprise", "FR",
						2021, false, "web", 1),
				new EndUserRow("user-1022", "Devon Pryce", "devon.pryce@harborlight.example", "free", "US", 2026,
						true, "web", 1),
				new EndUserRow("user-1023", "Hana Sato", "hana.sato@sakuragrid.example", "pro", "JP", 2023, false,
						"ios", 5),
				new EndUserRow("user-1024", "Liam Boucher", "liam.boucher@quebecois.example", "enterprise", "CA",
						2022, true, "web", 2),
				new EndUserRow("user-1025", "Zola Mabaso", "zola.mabaso@savannaco.example", "pro", "NG", 2025, false,
						"android", 7),
				new EndUserRow("user-1026", "Peter Halloran", "peter.halloran@clearwater.example", "free", "US", 2026,
						false, "web", 1),
				new EndUserRow("user-1027", "Aiko Yamashita", "aiko.yamashita@kitanotech.example", "enterprise", "JP",
						2021, false, "web", 2),
				new EndUserRow("user-1028", "Mateo Gutierrez", "mateo.gutierrez@andinopay.example", "pro", "MX", 2024,
						true, "web", 3),
				new EndUserRow("user-1029", "Freya Osmund", "freya.osmund@northgate.example", "free", "AU", 2025,
						false, "ios", 15),
				new EndUserRow("user-1030", "Ravi Subramanian", "ravi.subramanian@deccanworks.example", "enterprise",
						"IN", 2022, false, "web", 1),
				new EndUserRow("user-1031", "Elodie Chastain", "elodie.chastain@loirebase.example", "pro", "FR",
						2023, true, "web", 2),
				new EndUserRow("user-1032", "Bruno Tavares", "bruno.tavares@serratech.example", "free", "BR", 2026,
						false, "android", 4),
				new EndUserRow("user-1033", "Greta Lindholm", "greta.lindholm@balticbyte.example", "pro", "DE", 2024,
						false, "web", 2),
				new EndUserRow("user-1034", "Malik Sinclair", "malik.sinclair@riverbend.example", "enterprise", "US",
						2021, true, "web", 1),
				new EndUserRow("user-1035", "Tuva Nordstrom", "tuva.nordstrom@vikenlabs.example", "free", "CA", 2025,
						false, "web", 11),
				new EndUserRow("user-1036", "Ayaan Rahman", "ayaan.rahman@dhakaworks.example", "pro", "IN", 2023,
						false, "android", 3),
				new EndUserRow("user-1037", "Juliette Moreau", "juliette.moreau@rivegauche.example", "enterprise",
						"FR", 2022, false, "web", 1),
				new EndUserRow("user-1038", "Callum Hendry", "callum.hendry@lochsidedata.example", "pro", "AU", 2024,
						true, "ios", 2),
				new EndUserRow("user-1039", "Nia Thompson", "nia.thompson@brightharbor.example", "free", "US", 2026,
						false, "web", 1),
				new EndUserRow("user-1040", "Seo-yeon Park", "seoyeon.park@hangangtech.example", "enterprise", "JP",
						2022, true, "web", 2));
	}

	/** Segments are environment scoped, so this set is created in every environment. */
	public static List<SegmentRow> segmentRows() {
		return List.of(
				new SegmentRow("enterprise-accounts", "Enterprise accounts",
						"Every account on the enterprise plan.", List.of(), List.of(),
						"Plan is enterprise",
						List.of(new ClauseRow("plan", "in", List.of("enterprise"), false))),
				new SegmentRow("eu-customers", "European customers",
						"Accounts billed in the European Union.", List.of(), List.of(),
						"Country is Germany or France",
						List.of(new ClauseRow("country", "in", List.of("DE", "FR"), false))),
				new SegmentRow("beta-testers", "Beta testers",
						"Accounts that opted in to early features, plus two invited design partners.",
						List.of("user-1034", "user-1040"), List.of("user-1019"),
						"Opted in to beta",
						List.of(new ClauseRow("betaOptIn", "in", List.of(true), false))));
	}

	public static List<FlagRow> flagRows() {
		return List.of(
				new FlagRow("checkout-express-pay", "Express pay at checkout",
						"Shows the one-tap express pay button in the cart, either full width or compact.",
						"multivariate", List.of("checkout", "payments"), true, CHECKOUT, false, 0,
						List.of(new VariationRow("Full", "full", "Full width button above the order summary."),
								new VariationRow("Compact", "compact", "Inline button next to the pay action."),
								new VariationRow("Hidden", "off", "No express pay button."))),
				new FlagRow("new-navigation", "Redesigned primary navigation",
						"Replaces the sidebar with the new top navigation bar.", "boolean",
						List.of("web", "navigation"), true, ADMIN, false, 0,
						List.of(new VariationRow("New navigation", true, "Top navigation bar."),
								new VariationRow("Current navigation", false, "Existing sidebar."))),
				new FlagRow("search-ranking-v2", "Search ranking model",
						"Selects which ranking model serves search results.", "multivariate", List.of("search"), true,
						ADMIN, false, 0,
						List.of(new VariationRow("Baseline", "baseline", "Keyword ranking in production today."),
								new VariationRow("Semantic", "semantic", "Embedding based ranking."),
								new VariationRow("Hybrid", "hybrid", "Keyword and embedding blend."))),
				new FlagRow("dark-mode", "Dark theme",
						"Lets people choose the dark color scheme from account settings.", "boolean",
						List.of("web", "accessibility"), false, ADMIN, false, 0,
						List.of(new VariationRow("Dark theme available", true, "Theme switcher is shown."),
								new VariationRow("Light theme only", false, "Theme switcher is hidden."))),
				new FlagRow("annual-billing-discount", "Annual billing discount",
						"Offers the annual prepay discount on the upgrade screen.", "boolean",
						List.of("billing", "growth"), true, GROWTH, false, 0,
						List.of(new VariationRow("Discount shown", true, "Annual price is highlighted."),
								new VariationRow("Discount hidden", false, "Monthly price only."))),
				new FlagRow("mobile-offline-cart", "Offline cart on mobile",
						"Keeps the cart on device so it survives losing connectivity.", "boolean", List.of("mobile"),
						true, MOBILE, false, 0,
						List.of(new VariationRow("Offline cart on", true, "Cart is cached on device."),
								new VariationRow("Offline cart off", false, "Cart requires a connection."))),
				new FlagRow("support-live-chat", "Live chat in the help center",
						"Shows the live chat launcher during staffed support hours.", "boolean", List.of("support"),
						false, SUPPORT, false, 0,
						List.of(new VariationRow("Chat launcher shown", true, "Live chat is offered."),
								new VariationRow("Chat launcher hidden", false, "Email support only."))),
				new FlagRow("recommendation-carousel", "Recommendation carousel",
						"Controls which recommendation strategy fills the carousel on the home page.",
						"multivariate", List.of("growth", "web"), true, GROWTH, false, 0,
						List.of(new VariationRow("Off", "off", "No carousel."),
								new VariationRow("Related items", "related", "Same category items."),
								new VariationRow("Personalized", "personalized", "Ranked from account history."))),
				new FlagRow("bulk-csv-export", "Bulk CSV export",
						"Enables the multi-report CSV export in the reporting area.", "boolean",
						List.of("reporting", "enterprise"), true, ADMIN, false, 0,
						List.of(new VariationRow("Bulk export on", true, "Export up to 50 reports at once."),
								new VariationRow("Bulk export off", false, "Single report export only."))),
				new FlagRow("two-factor-enforcement", "Enforce two-factor sign-in",
						"Requires two-factor authentication for every member of an account.", "boolean",
						List.of("security"), false, ADMIN, false, 0,
						List.of(new VariationRow("Enforced", true, "Two-factor is required."),
								new VariationRow("Optional", false, "Two-factor is available but not required."))),
				new FlagRow("onboarding-checklist", "Onboarding checklist",
						"Selects which first-run checklist new accounts receive.", "multivariate",
						List.of("onboarding", "growth"), true, GROWTH, false, 0,
						List.of(new VariationRow("Classic", "classic", "Six step checklist."),
								new VariationRow("Guided", "guided", "Three steps with an in-product tour."),
								new VariationRow("Minimal", "minimal", "One step and a dismiss link."))),
				new FlagRow("legacy-report-builder", "Legacy report builder",
						"Kept the old report builder reachable during the reporting migration.", "boolean",
						List.of("reporting"), false, ADMIN, true, 12,
						List.of(new VariationRow("Legacy builder available", true, "Old builder is reachable."),
								new VariationRow("Legacy builder removed", false, "New builder only."))));
	}

	/**
	 * Explicit configuration histories. Any flag and environment pair missing
	 * here gets a single creation state: on in development, off elsewhere.
	 */
	public static List<ConfigRow> configRows() {
		return List.of(
				new ConfigRow("checkout-express-pay", "production", List.of(
						created(34, CHECKOUT, false, "v3", "v1"),
						state(21, CHECKOUT, "toggled", "Canary with the design partners only.", true, "v3", "v2",
								null, List.of(new TargetRow("v1", List.of("user-1001", "user-1034"))), List.of()),
						state(14, CHECKOUT, "rollout_updated", "Starting the ramp at 10 percent.", true, "v3", null,
								List.of(new WeightRow("v1", 10), new WeightRow("v2", 0), new WeightRow("v3", 90)),
								List.of(new TargetRow("v1", List.of("user-1001", "user-1034"))), List.of()),
						state(7, CHECKOUT, "rollout_updated", "Clean canary, moving to 40 percent.", true, "v3", null,
								List.of(new WeightRow("v1", 40), new WeightRow("v2", 0), new WeightRow("v3", 60)),
								List.of(new TargetRow("v1", List.of("user-1001", "user-1034"))), List.of()),
						rolledBack(4, CHECKOUT, "Payment latency regression on the 40 percent arm.", 3, true, "v3",
								null,
								List.of(new WeightRow("v1", 10), new WeightRow("v2", 0), new WeightRow("v3", 90)),
								List.of(new TargetRow("v1", List.of("user-1001", "user-1034"))), List.of()),
						state(1, CHECKOUT, "rollout_updated", "Latency fixed, resuming the ramp at 25 percent.", true,
								"v3", null,
								List.of(new WeightRow("v1", 25), new WeightRow("v2", 0), new WeightRow("v3", 75)),
								List.of(new TargetRow("v1", List.of("user-1001", "user-1034"))), List.of()))),
				new ConfigRow("checkout-express-pay", "staging", List.of(
						created(34, CHECKOUT, false, "v3", "v1"),
						state(30, CHECKOUT, "toggled", "Serving full width for release testing.", true, "v3", "v1",
								null, List.of(), List.of()))),
				new ConfigRow("checkout-express-pay", "development", List.of(
						created(34, CHECKOUT, true, "v3", "v1"),
						state(28, CHECKOUT, "targeting_updated", "Compact for the European checkout work.", true,
								"v3", "v1", null, List.of(),
								List.of(new RuleRow("European customers see the compact button",
										List.of("eu-customers"), List.of(), "v2", null))))),
				new ConfigRow("new-navigation", "production", List.of(
						created(45, ADMIN, false, "v2", "v1"),
						state(26, ADMIN, "targeting_updated", "Internal beta testers first.", false, "v2", "v1", null,
								List.of(),
								List.of(new RuleRow("Beta testers get the new navigation", List.of("beta-testers"),
										List.of(), "v1", null))),
						state(9, ADMIN, "toggled", "Opening the beta rule to live traffic.", true, "v2", "v2", null,
								List.of(),
								List.of(new RuleRow("Beta testers get the new navigation", List.of("beta-testers"),
										List.of(), "v1", null))))),
				new ConfigRow("new-navigation", "staging", List.of(
						created(45, ADMIN, false, "v2", "v1"),
						state(24, ADMIN, "toggled", "On for the whole staging environment.", true, "v2", "v1", null,
								List.of(), List.of()))),
				new ConfigRow("search-ranking-v2", "production", List.of(
						created(29, ADMIN, false, "v1", "v2"),
						state(15, ADMIN, "toggled", "Even split between semantic and baseline.", true, "v1", null,
								List.of(new WeightRow("v1", 50), new WeightRow("v2", 50), new WeightRow("v3", 0)),
								List.of(), List.of()),
						state(3, ADMIN, "rollout_updated", "Hybrid takes a third of traffic from baseline.", true,
								"v1", null,
								List.of(new WeightRow("v1", 34), new WeightRow("v2", 33), new WeightRow("v3", 33)),
								List.of(), List.of()))),
				new ConfigRow("search-ranking-v2", "staging", List.of(
						created(29, ADMIN, true, "v1", "v3"))),
				new ConfigRow("dark-mode", "production", List.of(
						created(60, ADMIN, false, "v2", "v1"),
						state(31, ADMIN, "toggled", "Shipped to everyone after the contrast audit.", true, "v2", "v1",
								null, List.of(), List.of()))),
				new ConfigRow("dark-mode", "staging", List.of(created(60, ADMIN, true, "v2", "v1"))),
				new ConfigRow("annual-billing-discount", "production", List.of(
						created(18, GROWTH, false, "v2", "v1"),
						state(11, GROWTH, "targeting_updated", "Enterprise accounts are priced separately.", true,
								"v2", "v1", null, List.of(),
								List.of(new RuleRow("Enterprise accounts keep negotiated pricing",
										List.of("enterprise-accounts"), List.of(), "v2", null))),
						state(2, GROWTH, "rollout_updated", "Half of the remaining traffic sees the discount.", true,
								"v2", null, List.of(new WeightRow("v1", 50), new WeightRow("v2", 50)), List.of(),
								List.of(new RuleRow("Enterprise accounts keep negotiated pricing",
										List.of("enterprise-accounts"), List.of(), "v2", null))))),
				new ConfigRow("mobile-offline-cart", "production", List.of(
						created(22, MOBILE, false, "v2", "v1"),
						state(6, MOBILE, "targeting_updated",
								"Android first, where the connectivity reports came from.", true, "v2", "v2", null,
								List.of(),
								List.of(new RuleRow("Android devices get the offline cart", List.of(),
										List.of(new ClauseRow("deviceType", "in", List.of("android"), false)), "v1",
										null))))),
				new ConfigRow("support-live-chat", "production", List.of(
						created(40, SUPPORT, false, "v2", "v1"),
						state(19, SUPPORT, "targeting_updated", "Enterprise plans have a staffed chat queue.", true,
								"v2", "v2", null, List.of(),
								List.of(new RuleRow("Enterprise accounts get live chat",
										List.of("enterprise-accounts"), List.of(), "v1", null))))),
				new ConfigRow("recommendation-carousel", "production", List.of(
						created(27, GROWTH, false, "v1", "v3"),
						state(13, GROWTH, "toggled", "Three way split to compare strategies.", true, "v1", null,
								List.of(new WeightRow("v1", 20), new WeightRow("v2", 40), new WeightRow("v3", 40)),
								List.of(), List.of()))),
				new ConfigRow("bulk-csv-export", "production", List.of(
						created(16, ADMIN, false, "v2", "v1"),
						state(5, ADMIN, "targeting_updated", "Named accounts that asked for the export.", true, "v2",
								"v2", null,
								List.of(new TargetRow("v1",
										List.of("user-1006", "user-1021", "user-1030", "user-1037"))),
								List.of()))),
				new ConfigRow("two-factor-enforcement", "production", List.of(
						created(52, ADMIN, false, "v2", "v1"),
						state(33, ADMIN, "targeting_updated", "Required for enterprise accounts under contract.",
								true, "v2", "v2", null, List.of(),
								List.of(new RuleRow("Enterprise accounts must use two-factor",
										List.of("enterprise-accounts"), List.of(), "v1", null))))),
				new ConfigRow("onboarding-checklist", "production", List.of(
						created(25, GROWTH, false, "v1", "v2"),
						state(17, GROWTH, "toggled", "Guided against classic for new accounts.", true, "v1", null,
								List.of(new WeightRow("v1", 50), new WeightRow("v2", 50), new WeightRow("v3", 0)),
								List.of(), List.of()),
						state(8, GROWTH, "rollout_updated", "Adding the minimal arm at 20 percent.", true, "v1", null,
								List.of(new WeightRow("v1", 40), new WeightRow("v2", 40), new WeightRow("v3", 20)),
								List.of(), List.of()))),
				new ConfigRow("legacy-report-builder", "production", List.of(
						created(70, ADMIN, true, "v2", "v1"),
						state(13, ADMIN, "toggled", "Migration finished, turning the old builder off.", false, "v2",
								"v1", null, List.of(), List.of()))));
	}

	private static StateRow created(int daysAgo, String actorEmail, boolean enabled, String offVariationId,
			String fallthroughVariationId) {
		return new StateRow(daysAgo, actorEmail, "flag_created", "", null, enabled, offVariationId,
				fallthroughVariationId, null, List.of(), List.of());
	}

	private static StateRow state(int daysAgo, String actorEmail, String action, String comment, boolean enabled,
			String offVariationId, String fallthroughVariationId, List<WeightRow> fallthroughWeights,
			List<TargetRow> targets, List<RuleRow> rules) {
		return new StateRow(daysAgo, actorEmail, action, comment, null, enabled, offVariationId,
				fallthroughVariationId, fallthroughWeights, targets, rules);
	}

	private static StateRow rolledBack(int daysAgo, String actorEmail, String comment, int restoredFromVersion,
			boolean enabled, String offVariationId, String fallthroughVariationId,
			List<WeightRow> fallthroughWeights, List<TargetRow> targets, List<RuleRow> rules) {
		return new StateRow(daysAgo, actorEmail, "rolled_back", comment, restoredFromVersion, enabled,
				offVariationId, fallthroughVariationId, fallthroughWeights, targets, rules);
	}

	private SeedData() {
	}
}
