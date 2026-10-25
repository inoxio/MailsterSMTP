MailsterSMTP
============

A NIO SMTP server API written in Java

This is a fork from the original (https://github.com/edeoliveira/MailsterSMTP)

## Setup repository

```kotlin
repositories {
    mavenCentral()
} 
```

## Dependency

```kotlin
compile("de.inoxio:mailster-smtp:1.3.0")
```

# Start a server

```jshelllanguage
var server = new SMTPServer(new MessageListenerAdapter() {
    @Override
    public void deliver(SessionContext ctx, String from, String recipient, InputStream data) {
        System.out.println("New message received");
    }
});

```
## Release

Change version in `build.gradle.kts`, `README.md` and `SMTPServerConfig.java` and issue:

```bash
./gradlew clean build publish jreleaserFullRelease
```

Publishing to Maven Central (Central Portal), signing and the GitHub release are done by
[JReleaser](https://jreleaser.org). It expects the following secrets in `~/.jreleaser/config.properties`
or as environment variables:

```properties
JRELEASER_MAVENCENTRAL_USERNAME=<Central Portal user token name>
JRELEASER_MAVENCENTRAL_PASSWORD=<Central Portal user token password>
JRELEASER_GPG_PUBLIC_KEY=<armored public key>
JRELEASER_GPG_SECRET_KEY=<armored secret key>
JRELEASER_GPG_PASSPHRASE=<passphrase>
JRELEASER_GITHUB_TOKEN=<GitHub token>
```

Append `--dryrun` to check the configuration without uploading anything.

## Check Renovate

https://developer.mend.io/github/inoxio/MailsterSMTP
