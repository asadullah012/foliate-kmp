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

## Branching Policy & Workflow

1. **`main` is protected**: Never push directly to `main`. All changes must arrive through a Pull Request.
2. **Branch naming**:
   - Bug fixes: `fix/<issue-number>-<short-description>` (e.g., `fix/12-brightness-scroll`)
   - Features: `feat/<issue-number>-<short-description>` (e.g., `feat/15-custom-themes`)
   - Documentation & Tooling: `docs/<short-description>` or `ci/<short-description>`
3. **Always branch off the latest `main`**:
   ```bash
   git checkout main
   git pull origin main
   git checkout -b fix/12-brightness-scroll
   ```

## Before you open a pull request

1. Run `./gradlew check`. All tests and ABI validation checks must pass.
2. If you modified public declarations, run `./gradlew updateKotlinAbi` (or `./gradlew apiDump`) and commit the updated `api/*.api` and `api/*.klib.api` files.
3. Write KDoc on every new public declaration. Both modules enforce `explicitApi()`.
4. Add an entry to the `[Unreleased]` section of `CHANGELOG.md` following [Keep a Changelog](https://keepachangelog.com/).
5. Fill out the Pull Request template and link the relevant issue (`Fixes #12`).

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

## Code of Conduct

Please review and adhere to our [Code of Conduct](CODE_OF_CONDUCT.md) in all project interactions.

## License

Your contribution goes out under the MIT license. See [LICENSE](LICENSE).

