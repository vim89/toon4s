# GitHub Workflows Documentation

This document explains how the GitHub Actions workflows work together in this repository.

## Overview

The workflows follow a **simple, proven, boring** approach with clear separation of concerns:

```
Code Push → CI Tests → Auto Tag → Changelog → Release
```

## Workflows

### 1. CI (`ci.yml`)

**Purpose**: Run tests and checks on every push and pull request

**Triggers**:
- Push to `main` branch
- Pull requests
- Manual via `workflow_dispatch`

**What it does**:
- Quick checks: formatting, compilation, binary compatibility
- Tests: runs on multiple OS (Ubuntu, macOS, Windows) and Scala versions (3.3.3, 2.13.14)
- Smoke tests: CLI functionality
- Budget gate: token savings verification
- JMH benchmarks: performance tests

**Skip**: Add `[skip ci]` or `[skip release]` to commit message

---

### 2. Auto Tag (`auto-tag.yml`)

**Purpose**: Automatically create version tags based on conventional commits

**Triggers**:
- Push to `main` branch (excluding `**.md`, `docs/**`, `CHANGELOG.md`)

**What it does**:
1. Analyzes commits since last tag
2. Determines version bump type based on conventional commits
3. Creates and pushes new tag (e.g., `v0.1.0`)
4. Triggers `release.yml` and `changelog.yml` via tag push

**Uses**: [mathieudutour/github-tag-action](https://github.com/mathieudutour/github-tag-action) - proven, reliable

**Conventional Commits**:
- `feat:` → MINOR bump (0.1.0 → 0.2.0)
- `fix:` → PATCH bump (0.1.0 → 0.1.1)
- `feat!:`, `fix!:`, or `BREAKING CHANGE:` → MAJOR bump (0.1.0 → 1.0.0)
- `chore:`, `docs:`, `refactor:`, `test:`, etc. → PATCH bump

**Edge Cases Handled**:
- First tag starts at `v0.1.0`
- No version bump if no relevant commits
- Tag already exists: skips gracefully
- Multiple commits: uses highest bump type
- Concurrent runs: prevented via concurrency group

**Skip**: Add `[skip ci]` or `[skip release]` to commit message

---

### 3. Changelog (`changelog.yml`)

**Purpose**: Generate changelog from conventional commits

**Triggers**:
- Tag creation (`v*`)
- Manual via `workflow_dispatch`

**What it does**:
1. Runs AFTER tag is created
2. Uses `git-cliff` to generate/update `CHANGELOG.md`
3. Commits and pushes back to `main` with `[skip ci]`

**Uses**: [git-cliff](https://git-cliff.org/) - conventional commits → changelog

**Edge Cases Handled**:
- First release: generates full changelog
- No changes: skips commit
- Concurrent runs: prevented via concurrency group
- Skip ci commits: ignored by `cliff.toml` config

**Configuration**: See `cliff.toml` for customization

---

### 4. Release (`release.yml`)

**Purpose**: Publish to Maven Central and create GitHub Release

**Triggers**:
- Tag creation (`v*`)
- Manual via `workflow_dispatch`

**What it does**:
1. Runs tests
2. Publishes to Maven Central using `sbt-ci-release`
3. Packages CLI artifacts (zip, tgz)
4. Creates GitHub Release with artifacts and release notes

**Uses**: `sbt-ci-release` 1.11.0+ (includes sbt-dynver, sbt-pgp, sbt-sonatype, sbt-git)

**Required Secrets**:
- `PGP_PASSPHRASE`: PGP signing key passphrase
- `PGP_SECRET`: Base64 encoded PGP secret key
- `SONATYPE_USERNAME`: Sonatype Central Portal username
- `SONATYPE_PASSWORD`: Sonatype Central Portal password

**Edge Cases Handled**:
- Only runs on version tags (not main branch)
- Tests run before publishing (fail fast)
- GitHub Release created only for version tags

**Important**: Uses Sonatype Central Portal (legacy OSSRH sunset 2025-06-30)

---

## Workflow Synchronization

The workflows are designed to work together **in sequence**:

### Happy Path Flow:

```
1. Developer commits to main with "feat: add new feature"
   ↓
2. CI workflow runs (tests pass) ✓
   ↓
3. Auto Tag workflow runs:
   - Detects "feat:" prefix
   - Creates tag v0.2.0
   - Pushes tag
   ↓
4. Tag push triggers TWO workflows in parallel:
   ├─→ Changelog workflow:
   │   - Generates CHANGELOG.md
   │   - Commits back to main [skip ci]
   │
   └─→ Release workflow:
       - Runs tests
       - Publishes to Maven Central
       - Creates GitHub Release
```

### Edge Case: No Version Bump

```
1. Developer commits to main with "docs: update README [skip ci]"
   ↓
2. Skipped by all workflows (contains [skip ci])
```

### Edge Case: Documentation Change

```
1. Developer commits to main with "docs: update README"
   ↓
2. CI workflow runs (tests pass) ✓
   ↓
3. Auto Tag workflow runs:
   - Detects "docs:" prefix
   - Creates tag v0.1.1 (patch bump)
   - Pushes tag
   ↓
4. Changelog + Release workflows run
```

### Edge Case: Multiple Commits

```
1. Developer pushes 3 commits:
   - "fix: bug 1"
   - "feat: new feature"
   - "chore: cleanup"
   ↓
2. CI workflow runs ✓
   ↓
3. Auto Tag workflow:
   - Analyzes all 3 commits
   - Determines MINOR bump (feat > fix > chore)
   - Creates tag v0.2.0
```

---

## Skip Mechanisms

To prevent infinite loops and unnecessary runs:

1. **[skip ci]**: Skips CI, Auto Tag workflows
2. **[skip release]**: Skips CI, Auto Tag workflows
3. **paths-ignore**: Auto Tag ignores `**.md`, `docs/**`, `CHANGELOG.md`
4. **concurrency groups**: Prevents concurrent runs of same workflow

---

## Manual Triggers

All workflows support manual triggers for testing/debugging:

- **CI**: `gh workflow run ci.yml`
- **Auto Tag**: Not needed (use manual tag instead)
- **Changelog**: `gh workflow run changelog.yml`
- **Release**: `gh workflow run release.yml -f tag=v0.1.0`

---

## Testing the Workflows

### Test Auto Tagging:

```bash
# Make a feature commit
git commit -m "feat: test auto tagging"
git push origin main

# Watch workflows
gh run list --workflow=auto-tag.yml
gh run list --workflow=changelog.yml
gh run list --workflow=release.yml
```

### Test Changelog Only:

```bash
# Create tag manually
git tag v0.1.0
git push origin v0.1.0

# Watch changelog workflow
gh run watch
```

### Test Release Only:

```bash
# Trigger manually with specific tag
gh workflow run release.yml -f tag=v0.1.0
```

---

## Troubleshooting

### Problem: "Snapshot releases must have -SNAPSHOT version number"

**Cause**: Tag format doesn't match expected pattern

**Solution**: Ensure tags start with `v` (e.g., `v0.1.0`, not `0.1.0`)

### Problem: "No tag push, publishing SNAPSHOT"

**Cause**: Workflow triggered on main branch push instead of tag push

**Solution**: This is normal behavior. Only tag pushes create releases.

### Problem: Changelog is empty

**Cause**: No tags exist yet, or `cliff.toml` configuration issue

**Solution**:
1. Check that tags exist: `git tag -l`
2. Run changelog workflow manually to regenerate
3. Verify `cliff.toml` commit parsers

### Problem: Workflows running in infinite loop

**Cause**: Changelog commit doesn't have `[skip ci]`

**Solution**: Verify changelog.yml line 71 has `[skip ci]`

### Problem: Auto Tag not creating tag

**Cause**: No conventional commit prefix, or paths-ignore matched

**Solution**:
1. Use conventional commit format
2. Check if files changed are in paths-ignore
3. Verify commit message doesn't have `[skip ci]`

---

## Conventional Commits Cheat Sheet

```bash
# Feature (minor bump)
git commit -m "feat: add new parser"

# Bug fix (patch bump)
git commit -m "fix: resolve encoding issue"

# Breaking change (major bump)
git commit -m "feat!: change API signature"
# or
git commit -m "feat: change API

BREAKING CHANGE: API signature changed"

# Other types (patch bump)
git commit -m "docs: update README"
git commit -m "chore: update dependencies"
git commit -m "refactor: simplify code"
git commit -m "test: add unit tests"
git commit -m "perf: improve performance"

# Skip CI/release
git commit -m "docs: typo fix [skip ci]"
```

---

## Version History

- **v1.0.0**: Complete rewrite using proven GitHub Actions
  - mathieudutour/github-tag-action for auto-tagging
  - sbt-ci-release 1.11.0 for publishing
  - git-cliff for changelog generation
  - Simple, boring, reliable workflows

---

## References

- [Conventional Commits](https://www.conventionalcommits.org/)
- [sbt-ci-release](https://github.com/sbt/sbt-ci-release)
- [git-cliff](https://git-cliff.org/)
- [mathieudutour/github-tag-action](https://github.com/mathieudutour/github-tag-action)
- [Semantic Versioning](https://semver.org/)
