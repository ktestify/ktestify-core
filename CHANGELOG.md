# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]
### ⬆️ Dependency Updates

- Bump com.typesafe:config from 1.4.8 to 1.4.9 *(deps)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump com.fasterxml.jackson:jackson-bom from 2.21.3 to 2.22.0 *(deps)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump org.sonatype.central:central-publishing-maven-plugin *(deps-dev)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump com.diffplug.spotless:spotless-maven-plugin *(deps-dev)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump the kafka group with 2 updates *(deps)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump the cucumber group with 4 updates *(deps)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump the junit5 group with 3 updates *(deps-dev)* — [@dependabot[bot]](https://github.com/dependabot[bot])

- Bump org.apache.kafka:kafka-clients *(deps)* — [@dependabot[bot]](https://github.com/dependabot[bot])


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
