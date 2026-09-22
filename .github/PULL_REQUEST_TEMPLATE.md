## Description

Please describe the changes proposed in this pull request. Explain the *why*, not just the *what*.

Fixes #(issue number)

---

## Type of Change

- [ ] 🐛 Bug fix (non-breaking change which fixes an issue)
- [ ] ✨ New feature (non-breaking change which adds functionality)
- [ ] 💥 Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] 📝 Documentation update
- [ ] ⚡ Performance improvement / refactoring
- [ ] 🔧 Build / CI / Tooling change

---

## Quality Checklist

- [ ] My code follows the code style and naming conventions of this project.
- [ ] I have run `./gradlew check` and all unit tests pass locally.
- [ ] If changing public declarations, I have documented them with KDoc and maintained explicit visibility.
- [ ] If changing public API signatures, I have run `./gradlew updateKotlinAbi` (or `./gradlew apiDump`) and committed the updated `.api` files.
- [ ] I have added tests in `commonTest` for any new logic or bug fixes.
- [ ] I have updated `CHANGELOG.md` under the `[Unreleased]` section.
- [ ] I have tested this change on Android and/or iOS devices/simulators where applicable.
