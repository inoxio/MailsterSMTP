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
compile("de.inoxio:mailster-smtp:1.1.0")
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
./gradlew clean test publishToSonatype closeAndReleaseSonatypeStagingRepository githubRelease
```

## Check Renovate

https://developer.mend.io/github/inoxio/MailsterSMTP
