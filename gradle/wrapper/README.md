# Gradle Wrapper Notes

This folder is prepared for Gradle wrapper-style configuration.

Current state:

- The repository root contains a custom `gradlew` shell script.
- The official `gradle-wrapper.jar` is not included yet.
- Local Gradle zip files can be placed in `gradle/distributions/`.

Recommended local zip file:

```text
gradle/distributions/gradle-8.10.2-all.zip
```

Alternative local zip file:

```text
gradle/distributions/gradle-9.0-all.zip
```

The custom root `gradlew` script automatically searches these locations.
