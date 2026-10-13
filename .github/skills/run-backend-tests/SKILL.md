---
name: run-backend-tests
description: Build and test the website backend locally with Gradle and Testcontainers. Use before reporting any change as done.
---

# Run the vempain-website-backend tests

- Requirements: Java 25, Docker (Testcontainers PostgreSQL), GitHub Packages credentials only if a `vempain-*` artifact is consumed (`gpr.user`/`gpr.token` or
  `GITHUB_ACTOR`/`GITHUB_TOKEN`).
- Iterate with the focused task first (`./gradlew :service:test --tests '*OpenGraph*'`, `./gradlew :api:test` for the contract tests),
  then run `./gradlew clean test` before finishing.
- Suffixes: `UTC` unit, `CTC` controller, `ITC` integration, `JTC` JSON contract in `api/`; helpers carry no suffix.
- Changing deployment behaviour: review `README.md`, `application*.yaml`, the Dockerfiles and `.github/workflows/ci.yaml`
  (`spring-boot-service.yaml` from `vempain-workflows`), and the website stack in `vempain-cluster/website/`.
- Report tests run and failures from the output; results are in `service/build/test-results/test/`.
