# KLibrosa
This library is a Kotlin adoption of [jLibrosa](https://github.com/Subtitle-Synchronizer/jlibrosa) library

# Gradle (Kotlin DSL) setup

#### Add Jitpack to settings.gradle.kts
```kotlin
pluginManagement {
    repositories {
        ...
        maven(url = "https://jitpack.io")
    }
}
```

#### Add the dependency in module's build.gradle.kts 
```kotlin
implementation("com.github.thedroiddiv:KLibrosa:<LATEST_RELEASE>")
```

# FAQs
In case of build failure due to duplicate class under the namespace 'org.hamcrest', exclude it from the dependency
```kotlin
implementation("com.github.thedroiddiv:KLibrosa:v1.1") { exclude(group = "org.hamcrest") }
```
