# How to contribute

Thank you for your interest in foliate-kmp.

## What you need

- JDK 21
- Xcode with the iOS simulator, for the iOS targets
- A macOS host. The iOS targets do not build on Linux or Windows.

## Build and test

```bash
./gradlew build          # Compile every target and run every check
./gradlew allTests       # Run the tests only
./gradlew check          # Run the tests and the ABI check
```

## Before you open a pull request

1. Run `./gradlew check`. It must pass.
2. If you changed the public API, run `./gradlew updateKotlinAbi` and commit the new
   `api/*.api` and `api/*.klib.api` files. `check` fails without this step.
3. Write KDoc on every new public declaration. Both modules use `explicitApi()`, so
   the compiler asks for an explicit visibility modifier on each one.
4. Add a line to the `Unreleased` section of `CHANGELOG.md`.

## Test the published artifacts

A defect in the packaging does not appear in a source build. Test the real artifacts:

```bash
./gradlew publishToMavenLocal
```

Then add `mavenLocal()` to a sample application and load the library from there. Run
the sample on a real Android device and on a real iOS device. The engine files reach
the application only through the published artifact.

To look inside the Android artifact:

```bash
unzip -l foliate-kmp-core/build/outputs/aar/foliate-kmp-core.aar
```

The listing must show the engine files under
`assets/composeResources/io.github.asadullah012.foliate.resources/files/foliate/`.

## The foliate-js engine

The files in `foliate-kmp-core/src/commonMain/composeResources/files/foliate/` come
from [foliate-js](https://github.com/johnfactotum/foliate-js). Keep local changes to a
minimum, and describe each one in the pull request. Do not copy these files to a
second directory. One copy reaches both platforms.

## Style

The project follows the standard Kotlin coding conventions. Write comments and
documents in simple, direct English.

## License

Your contribution goes out under the MIT license. See [LICENSE](LICENSE).
