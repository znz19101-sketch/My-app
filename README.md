# Guardexa Pack 28 — Complete Build System

Included:
- Gradle settings and root build
- Gradle 8.13 wrapper configuration
- Central Version Catalog
- AGP 8.13.2 and Kotlin 2.1.20 baseline
- Android 10 minimum SDK (API 29)
- Compile/target SDK 36
- App build types: debug, internal, beta, release
- Product flavors: standard and managed
- Java 17
- Compose, Hilt, Room, CameraX, MediaPipe, TensorFlow Lite dependencies
- Release signing through local Gradle properties only
- R8/ProGuard rules
- Lint baseline
- Detekt rules
- GitHub Actions CI
- Verification and release scripts
- Git ignore and local configuration examples

Important:
- Dependency versions are pinned, never dynamic.
- The first laptop build should use the stable AGP 8.x path.
- Do not put signing passwords in Git.
- The AI model files remain intentionally excluded until real validated assets are added.
- Some library versions may need a small compatibility adjustment during the first Gradle sync.
