# vempain-website-backend — Agent Guide

## Scope

This repository is the current Spring Boot/Java 25 implementation of the
public website backend. The former PHP backend is discarded and must not be
treated as a supported implementation.

## Layout

- `api/`: REST interfaces and DTOs.
- `service/`: Spring Boot application, controllers, security, services,
  repositories, migrations, and tests.
- Public runtime paths include `/api`, `/file`, and `/health`.

## Conventions

- Preserve snake_case JSON contracts.
- Prefer Lombok annotations for applicable Java constructors, accessors,
  builders, and logging unless they obscure behavior or conflict with Spring.
- Keep controllers thin and place business rules in services.
- Preserve the repository's tab-indented Java formatting.
- Test suffixes are meaningful: `UTC`, `CTC`, and `ITC`.

## Validation

Run `./gradlew clean test` for backend changes. Use the focused service or API
test task first when iterating. Review `README.md`, `application*.yaml`,
Dockerfiles, and CI configuration when changing deployment behavior.

## Tag ACL rule

Tags are metadata, not ACL-bearing resources. Tag entities have no ACL information, so tag list, search, and mutation endpoints must not perform ACL checks on tags. ACL checks apply only to resources that explicitly carry an ACL.
