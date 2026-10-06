# Changelog

All notable changes to this project will be documented in this file.

## [1.2.1] — 2026-10-06

### ♻️ Refactoring

- Enhance plugin loading and shutdown logic — [@nil-malh](https://github.com/nil-malh) ([#64](https://github.com/ktestify/ktestify-core/pull/64))

- Enhance timeout handling and improve documentation in KafkaRecordFetcher — [@nil-malh](https://github.com/nil-malh) ([#64](https://github.com/ktestify/ktestify-core/pull/64))

- Extract null value message to constant in MatchResult — [@nil-malh](https://github.com/nil-malh) ([#64](https://github.com/ktestify/ktestify-core/pull/64))


### ✨ Features

- Add AvroJson and AvroLogicalTypesSerializationTest for JSON conversion of Avro logical types — [@nil-malh](https://github.com/nil-malh) ([#64](https://github.com/ktestify/ktestify-core/pull/64))

- Enhance DynamicVariableFactory for thread-safety and case-insensitivity — [@nil-malh](https://github.com/nil-malh) ([#64](https://github.com/ktestify/ktestify-core/pull/64))

- Added two new MatchResult noRecords and nullValue — [@nil-malh](https://github.com/nil-malh) ([#64](https://github.com/ktestify/ktestify-core/pull/64))

- Introduce FetchTimeoutException for clearer timeout handling — [@nil-malh](https://github.com/nil-malh) ([#64](https://github.com/ktestify/ktestify-core/pull/64))

- Introduce key matching strategies for record assertions (#62) — [@nil-malh](https://github.com/nil-malh) ([#62](https://github.com/ktestify/ktestify-core/pull/62))


### ⬆️ Dependency Updates

- Bump ktestify-parent version from 1.0.4 to 1.0.5 — [@nil-malh](https://github.com/nil-malh) ([#64](https://github.com/ktestify/ktestify-core/pull/64))


### 🐛 Bug Fixes

- Fixed a lot of bugs — [@nil-malh](https://github.com/nil-malh) ([#64](https://github.com/ktestify/ktestify-core/pull/64))

- Fixed an issue where a NullPointerException was thrown on unset environment variables — [@nil-malh](https://github.com/nil-malh) ([#64](https://github.com/ktestify/ktestify-core/pull/64))

- Close URLClassLoader in PluginRegistry to prevent file handle leak — [@nil-malh](https://github.com/nil-malh) ([#64](https://github.com/ktestify/ktestify-core/pull/64))

- Add missing schemas directory default to reference.conf — [@nil-malh](https://github.com/nil-malh) ([#64](https://github.com/ktestify/ktestify-core/pull/64))

- Harden XML parsers against XXE attacks in XMLUtils — [@nil-malh](https://github.com/nil-malh) ([#64](https://github.com/ktestify/ktestify-core/pull/64))

- Enforce error taxonomy and add missing exception constructors — [@nil-malh](https://github.com/nil-malh) ([#64](https://github.com/ktestify/ktestify-core/pull/64))

- Correct over-suppression of CHILD_NODELIST_LENGTH in XMLUtils — [@nil-malh](https://github.com/nil-malh) ([#64](https://github.com/ktestify/ktestify-core/pull/64))

- Fixed an issue where a SchemaRegistryClient was instantiated at each call instead of being cached — [@nil-malh](https://github.com/nil-malh) ([#64](https://github.com/ktestify/ktestify-core/pull/64))

- Improve byte buffer handling in AvroDeserializer — [@nil-malh](https://github.com/nil-malh) ([#64](https://github.com/ktestify/ktestify-core/pull/64))

- Include cause in ConsumerException for better error handling — [@nil-malh](https://github.com/nil-malh) ([#64](https://github.com/ktestify/ktestify-core/pull/64))

- Handle null values in record matchers and improve logging — [@nil-malh](https://github.com/nil-malh) ([#64](https://github.com/ktestify/ktestify-core/pull/64))

- Enhance security in SAXParserFactory and improve documentation — [@nil-malh](https://github.com/nil-malh) ([#64](https://github.com/ktestify/ktestify-core/pull/64))

- Improve handling of empty records in AttributeRecordMatcher — [@nil-malh](https://github.com/nil-malh) ([#64](https://github.com/ktestify/ktestify-core/pull/64))

- Improve getLine method to handle null content and out-of-bounds indices — [@nil-malh](https://github.com/nil-malh) ([#64](https://github.com/ktestify/ktestify-core/pull/64))

- Null pointer exception on empty TimestampVariable when format is null — [@nil-malh](https://github.com/nil-malh) ([#64](https://github.com/ktestify/ktestify-core/pull/64))

- Update dependency review workflow path — [@nil-malh](https://github.com/nil-malh) ([#64](https://github.com/ktestify/ktestify-core/pull/64))

- Add missing TimeUnit import in KafkaRecordFetcher — [@nil-malh](https://github.com/nil-malh) ([#62](https://github.com/ktestify/ktestify-core/pull/62))

- Fixed changelog commit — [@nil-malh](https://github.com/nil-malh)


### 🔧 Miscellaneous

- Fixed some tests — [@nil-malh](https://github.com/nil-malh) ([#64](https://github.com/ktestify/ktestify-core/pull/64))


### 🧪 Tests

- Add ClasspathTestPlugin for lifecycle call assertions — [@nil-malh](https://github.com/nil-malh) ([#64](https://github.com/ktestify/ktestify-core/pull/64))

- Add TombstoneMatcherTest to verify tombstone handling across matchers — [@nil-malh](https://github.com/nil-malh) ([#64](https://github.com/ktestify/ktestify-core/pull/64))

- Rename integration tests with *ITTests — [@nil-malh](https://github.com/nil-malh) ([#64](https://github.com/ktestify/ktestify-core/pull/64))

- Rename integration tests with *ITTests — [@nil-malh](https://github.com/nil-malh) ([#64](https://github.com/ktestify/ktestify-core/pull/64))


## [1.1.3] — 2026-09-20

### ✨ Features

- Feat/multiple match fields (#59) — [@nil-malh](https://github.com/nil-malh) ([#59](https://github.com/ktestify/ktestify-core/pull/59))

- Add multi-field inline matching support in AvroFieldsRecordMatcher — [@nil-malh](https://github.com/nil-malh) ([#59](https://github.com/ktestify/ktestify-core/pull/59))


## [1.1.2] — 2026-08-10

### ♻️ Refactoring

- Improve logging messages for PluginSystem and add PluginVersionResolver class — [@nil-malh](https://github.com/nil-malh)

- Refactored plugin init logs — [@nil-malh](https://github.com/nil-malh)


## [1.1.1] — 2026-08-09

### ✨ Features

- Add attributes support to ConsumedRecord and related matchers — [@nil-malh](https://github.com/nil-malh)

- Reorganize imports for consistency and clarity across multiple files — [@nil-malh](https://github.com/nil-malh)


## [1.1.0] — 2026-08-06

### ♻️ Refactoring

- Remove date content-sniffing and implement type-driven date comparison — [@nil-malh](https://github.com/nil-malh)


### ✨ Features

- Switched to centralised GH Actions — [@nil-malh](https://github.com/nil-malh)

- Enhance deepEquals method to support dot-notation for excluded keys — [@nil-malh](https://github.com/nil-malh)

- Add referenceTimestamp to ConsumerContext to prevent clock drift in fetches — [@nil-malh](https://github.com/nil-malh)

- Update release permissions and add CI workflow for main branch — [@nil-malh](https://github.com/nil-malh)


## [1.0.3] — 2026-07-07

### ✨ Features

- Switch to a parent pom to manage dependencies — [@nil-malh](https://github.com/nil-malh)


### ⬆️ Dependency Updates

- Updated ktestify-parent to 1.0.2 — [@nil-malh](https://github.com/nil-malh)


## [0.1.2] — 2026-07-05

### ✨ Features

- Add JVM wide truststore configuration and tests — [@nil-malh](https://github.com/nil-malh)


### ⬆️ Dependency Updates

- Bump com.typesafe:config from 1.4.8 to 1.4.9 *(deps)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump com.fasterxml.jackson:jackson-bom from 2.21.3 to 2.22.0 *(deps)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump org.sonatype.central:central-publishing-maven-plugin *(deps-dev)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump com.diffplug.spotless:spotless-maven-plugin *(deps-dev)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump the kafka group with 2 updates *(deps)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump the cucumber group with 4 updates *(deps)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump the junit5 group with 3 updates *(deps-dev)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump org.apache.kafka:kafka-clients *(deps)* — [@dependabot[bot]](https://github.com/dependabot[bot])


### 🐛 Bug Fixes

- Fix links in bug report template — [@nil-malh](https://github.com/nil-malh)

- Fixed an issue where KTESTIFY_ROOT_LOG_LEVEL did not set the desired log level — [@nil-malh](https://github.com/nil-malh)


## [0.1.1] — 2026-06-11

### ⬆️ Dependency Updates

- Bump com.diffplug.spotless:spotless-maven-plugin *(deps-dev)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump org.slf4j:slf4j-api in the logging group *(deps)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump the kafka group with 2 updates *(deps)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump the kafka group across 1 directory with 2 updates *(deps)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump the junit5 group across 1 directory with 3 updates *(deps-dev)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump xmlunit.version from 2.11.0 to 2.12.0 *(deps)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump maven.surefire.plugin.version from 3.5.5 to 3.5.6 *(deps-dev)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump com.diffplug.spotless:spotless-maven-plugin *(deps-dev)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump org.jacoco:jacoco-maven-plugin *(deps-dev)* — [@dependabot[bot]](https://github.com/dependabot[bot])


### 🐛 Bug Fixes

- Fixed an issue where a release on GitHub would not have the jars attached — [@nil-malh](https://github.com/nil-malh)


## [0.1.0] — 2026-05-17

### ✨ Features

- Added topic namespace configuration to KafkaConfig and reference.conf — [@nil-malh](https://github.com/nil-malh)

- Migrated to Log4j2 update dependencies — [@nil-malh](https://github.com/nil-malh)

- Enhance logging in AvroKafkaProducer and RawKafkaProducer for better traceability — [@nil-malh](https://github.com/nil-malh)

- Enhance XmlRecordMatcher to support automatic exclusion of elements marked as EXCLUDED in XML templates — [@nil-malh](https://github.com/nil-malh)

- Added report output path in the FrameworkConfig — [@nil-malh](https://github.com/nil-malh)

- Added Log4J config in KTestify Config — [@nil-malh](https://github.com/nil-malh)

- Implement plugin system with KtestifyPlugin interface and PluginRegistry — [@nil-malh](https://github.com/nil-malh)

- Migrated from custom GitHub PAT to GITHUB_TOKEN — [@nil-malh](https://github.com/nil-malh)


### ⬆️ Dependency Updates

- Bump com.typesafe:config from 1.4.6 to 1.4.7 *(deps)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump org.projectlombok:lombok from 1.18.44 to 1.18.46 *(deps)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump the junit5 group with 3 updates *(deps-dev)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump commons-io:commons-io in the commons group *(deps)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump the testcontainers group across 1 directory with 3 updates *(deps-dev)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump com.google.code.gson:gson from 2.13.2 to 2.14.0 *(deps)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump the logging group with 3 updates *(deps)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump com.typesafe:config from 1.4.7 to 1.4.8 *(deps)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump com.fasterxml.jackson:jackson-bom from 2.19.2 to 2.21.3 *(deps)* — [@dependabot[bot]](https://github.com/dependabot[bot])


### 🐛 Bug Fixes

- Update commit message prefixes for Dependabot and CI in configuration files — [@nil-malh](https://github.com/nil-malh)

- Update URLs in .github/ISSUE_TEMPLATE/config.yml for discussions, documentation, and security vulnerability reporting — [@nil-malh](https://github.com/nil-malh)

- Remove security update group from dependabot configuration they are already managed by dependabot — [@nil-malh](https://github.com/nil-malh)

- Update ProducerRecord.buildRecord to use namespaced topic in AbstractKafkaProducer — [@nil-malh](https://github.com/nil-malh)

- Update buildMatchContext to use matchFilePaths and excludedFields — [@nil-malh](https://github.com/nil-malh)

- Refactor ConsumerContext to support multiple match file paths and excluded fields — [@nil-malh](https://github.com/nil-malh)

- Refactor record fetching logic to improve handling of single and batch modes — [@nil-malh](https://github.com/nil-malh)

- Used a try with resources in the AbstractKafkaConsumer.call() — [@nil-malh](https://github.com/nil-malh)

- Fixed 'Unread local variable' in test — [@nil-malh](https://github.com/nil-malh)

- Fix : Fixed an issue with the import of GPG keys for release — [@nil-malh](https://github.com/nil-malh)

- Fixed an issue on changelog.yml — [@nil-malh](https://github.com/nil-malh)


### 🔧 Miscellaneous

- Chore(ci)(deps): bump actions/github-script in the actions-core group — [@dependabot[bot]](https://github.com/dependabot[bot])

- Chore(ci)(deps): bump softprops/action-gh-release from 2 to 3 — [@dependabot[bot]](https://github.com/dependabot[bot])

- Cleanup some unused dependencies — [@nil-malh](https://github.com/nil-malh)

- Removed ConfigConstants.java — [@nil-malh](https://github.com/nil-malh)

- Removed unused components in feature_request.yml & added email in SECURITY.md — [@nil-malh](https://github.com/nil-malh)

- Update license in all files — [@nil-malh](https://github.com/nil-malh)

- Removed sonarscan plugin — [@nil-malh](https://github.com/nil-malh)


### 🎉 New Contributors





---
*Generated by [git-cliff](https://github.com/orhun/git-cliff)*
