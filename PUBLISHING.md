# Publishing

All six library modules (`rtf-core`, `rtf-reader`, `rtf-writer`, `rtf-io-kotlinx`, `rtf-io-okio`,
`rtf-compose`) publish to **Maven Central** under the group **`com.darkrockstudios`**, via the
[vanniktech Maven Publish plugin](https://github.com/vanniktech/gradle-maven-publish-plugin) targeting
the **Sonatype Central Portal** (`automaticRelease = true`). Each module applies the `rtf.publish`
convention plugin (`build-logic/src/main/kotlin/rtf.publish.gradle.kts`). `sample-cli` is not published.

## One-time setup (repository owner)

1. **Verify the `com.darkrockstudios` namespace** in the [Central Portal](https://central.sonatype.com/).
   (Likely already done — the same group is used by PlatformSpellCheckerKt.)
2. Add these **GitHub Actions secrets** to the repo (Settings → Secrets and variables → Actions):

   | Secret | Value |
   |---|---|
   | `OSSRH_USERNAME` | Central Portal **token** username |
   | `OSSRH_PASSWORD` | Central Portal **token** password |
   | `SIGNING_KEY` | base64-encoded GPG **private** key: `gpg --export-secret-keys <KEY_ID> \| base64` |
   | `SIGNING_PASSWORD` | the GPG key passphrase |

   They are mapped in `.github/workflows/publish.yml` to vanniktech's
   `ORG_GRADLE_PROJECT_mavenCentralUsername` / `…Password` / `signingInMemoryKey` / `…KeyPassword`.

## Releasing

The version is read from the `library.version` property in `gradle.properties` (default
`0.1.0-SNAPSHOT`). The publish workflow overrides it from the pushed tag.

```bash
# Tag the release (vX.Y.Z) and push it — the workflow publishes all modules.
git tag v0.1.0
git push origin v0.1.0
```

`.github/workflows/publish.yml` runs on `macos-latest` (the only host that can cross-compile every
target — Apple, JVM, JS, Wasm, Android, Linux, mingw) and invokes:

```
./gradlew publishToMavenCentral --no-daemon --stacktrace
```

With `automaticRelease = true` the deployment is released without a manual click. A `workflow_dispatch`
trigger with an optional `version` input is also available for manual runs.

> If the first multi-module release produces separate Central Portal deployments rather than one
> bundle, switch the task to an aggregated invocation per the vanniktech docs — the rest of the config
> is unaffected.

## Local

```bash
./gradlew publishToMavenLocal      # SNAPSHOT, unsigned — for testing consumers
```
