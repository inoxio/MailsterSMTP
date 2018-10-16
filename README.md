MailsterSMTP
============

[ ![Download](https://api.bintray.com/packages/inoxio/maven/mailster-smtp/images/download.svg) ](https://bintray.com/inoxio/maven/mailster-smtp/_latestVersion)

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
compile("de.inoxio:mailster-smtp:1.0.2")
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
## Check for new dependencies

```bash
./gradlew dependencyUpdates -Drevision=release
```

## Release

Change version in `build.gradle.kts`, `README.md` and `SMTPServerConfig.java` and issue:

```bash
./gradlew bintrayUpload
```
