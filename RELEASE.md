# Release Guide for toon4s

This document explains how to release toon4s to Maven Central using the automated CI/CD pipeline.

## Prerequisites

### 1. Sonatype Account Setup

**For 2025+**: Sonatype has migrated from Legacy OSSRH to the new **Central Portal** (https://central.sonatype.com/).

1. **Create Account**: Sign up at https://central.sonatype.com/
   - Recommended: Use "Sign in with GitHub" to automatically get `io.github.<username>` namespace
   - Alternative: Use email signup and request namespace verification

2. **Verify Namespace**:
   - For `io.toonformat`: Domain ownership verification required
   - For `io.github.vim89`: Automatic if signed in via GitHub

3. **Generate User Token** (NOT your login password!):
   - Go to https://central.sonatype.com/account
   - Click "Generate User Token"
   - Save the **token name** and **token password** (you'll need these for GitHub secrets)

### 2. GPG Key Setup

Create a GPG key for signing artifacts:

```bash
# Generate new GPG key (if you don't have one)
gpg --gen-key
# Use: Real name: "Vitthal Mirji", Email: "vitthalmirji@gmail.com"

# List keys to get the key ID
gpg --list-secret-keys --keyid-format LONG

# Should show something like:
# sec   rsa3072/ABC123DEF456 2025-01-01 [SC]
#       1234567890ABCDEF1234567890ABCDEF12345678
# uid   [ultimate] Vitthal Mirji <vitthalmirji@gmail.com>

# The long key ID is: ABC123DEF456 (or the full fingerprint)
```

**Publish your public key** to keyservers (required for Maven Central):

```bash
# Export public key
gpg --keyserver keyserver.ubuntu.com --send-keys ABC123DEF456

# Verify it's published (wait ~5 minutes)
gpg --keyserver keyserver.ubuntu.com --recv-keys ABC123DEF456
```

**Export private key for GitHub**:

```bash
# Export as base64 (CRITICAL: use -w0 flag to avoid line wrapping!)
gpg --armor --export-secret-keys ABC123DEF456 | base64 -w0

# This outputs a SINGLE LINE of base64 text - copy the entire output
```

### 3. GitHub Secrets Configuration

Go to your repository: **Settings → Secrets and variables → Actions → New repository secret**

Add these 4 secrets:

| Secret Name | Value | Notes |
|-------------|-------|-------|
| `SONATYPE_USERNAME` | Your Sonatype **user token name** | NOT your login username! |
| `SONATYPE_PASSWORD` | Your Sonatype **user token password** | NOT your login password! |
| `PGP_SECRET` | Base64-encoded private key | Output from `gpg --armor --export-secret-keys ... \| base64 -w0` |
| `PGP_PASSPHRASE` | Your GPG key passphrase | The password you set when creating the GPG key |

**CRITICAL**:
- `SONATYPE_USERNAME` and `SONATYPE_PASSWORD` must be from the **User Token**, not your login credentials
- `PGP_SECRET` MUST be a single line (no line breaks) - use `base64 -w0` on Linux or `base64` on macOS

## How Releases Work

The release workflow is fully automated using **sbt-ci-release**:

### Snapshot Releases (Automatic)

Every commit pushed to `main` automatically publishes a **snapshot** to Sonatype snapshots:

```bash
git push origin main
# Publishes: io.toonformat:toon4s-core_3:0.1.0+3-a1b2c3d4-SNAPSHOT
```

Snapshot versions are available at:
```scala
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
libraryDependencies += "io.toonformat" %% "toon4s-core" % "0.1.0+3-a1b2c3d4-SNAPSHOT"
```

### Release Versions (Tag-Based)

To publish a **release** to Maven Central:

```bash
# 1. Ensure all changes are committed and pushed
git status

# 2. Create and push a tag (MUST start with 'v')
git tag -a v0.1.0 -m "Release v0.1.0"
git push origin v0.1.0

# 3. CI automatically:
#    - Runs all tests
#    - Publishes to Sonatype (which syncs to Maven Central)
#    - Creates GitHub Release with CLI packages
```

**Version is derived from git tags** by sbt-dynver:
- `v0.1.0` tag → version `0.1.0`
- Between tags → version like `0.1.0+3-a1b2c3d4` (3 commits after v0.1.0, hash a1b2c3d4)

## Release Checklist

Use this checklist when creating a release:

### Pre-Release

- [ ] All CI checks passing on `main` branch
- [ ] All tests passing locally: `sbt +test`
- [ ] Update `CHANGELOG.md` with release notes
- [ ] Update README.md version numbers if applicable
- [ ] Run `sbt scalafmtCheckAll` to verify formatting
- [ ] Verify secrets are configured in GitHub (see above)

### Release

- [ ] Create release tag: `git tag -a v0.1.0 -m "Release v0.1.0"`
- [ ] Push tag: `git push origin v0.1.0`
- [ ] Monitor GitHub Actions: https://github.com/vim89/toon4s/actions
- [ ] Wait for "Release" workflow to complete (~10-15 minutes)

### Post-Release Verification

- [ ] Check GitHub Release created: https://github.com/vim89/toon4s/releases
- [ ] Verify Maven Central (wait 10-30 min for sync): https://central.sonatype.com/artifact/io.toonformat/toon4s-core_3
- [ ] Test installation:
  ```scala
  libraryDependencies += "io.toonformat" %% "toon4s-core" % "0.1.0"
  ```
- [ ] Announce the release (Twitter, Reddit, Scala forums, etc.)

## Troubleshooting

### "base64: invalid input" Error

**Problem**: The `PGP_SECRET` is not properly base64 encoded or contains line breaks.

**Solution**:
```bash
# Linux (GNU base64) - MUST use -w0 flag
gpg --armor --export-secret-keys YOUR_KEY_ID | base64 -w0

# macOS (BSD base64) - no flag needed
gpg --armor --export-secret-keys YOUR_KEY_ID | base64
```

Copy the ENTIRE output (it will be a single very long line) and paste into GitHub secret.

### "gpg: no valid OpenPGP data found"

**Problem**: The base64 decoding failed or the key is corrupted.

**Solution**: Re-export the key using the commands above, ensuring you copy the complete output.

### "unauthorized" or "403" from Sonatype

**Problem**: You're using login credentials instead of user token.

**Solution**:
1. Go to https://central.sonatype.com/account
2. Generate a new **User Token**
3. Update `SONATYPE_USERNAME` and `SONATYPE_PASSWORD` GitHub secrets with the **token credentials**

### "Invalid signature" or "Key not found on keyserver"

**Problem**: Your public key is not published to keyservers.

**Solution**:
```bash
# Publish to multiple keyservers
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
gpg --keyserver keys.openpgp.org --send-keys YOUR_KEY_ID
gpg --keyserver pgp.mit.edu --send-keys YOUR_KEY_ID

# Wait 5-10 minutes for propagation
```

### "Version v0.1.0 already exists"

**Problem**: The tag already exists on Maven Central.

**Solution**: Delete the local tag and create a new version:
```bash
git tag -d v0.1.0
git push origin :refs/tags/v0.1.0  # Delete from remote
git tag -a v0.1.1 -m "Release v0.1.1"
git push origin v0.1.1
```

## Manual Release (Emergency)

If CI fails, you can release manually from your local machine:

```bash
# 1. Ensure you have the secrets in your environment
export PGP_PASSPHRASE="your-passphrase"
export PGP_SECRET="your-base64-secret"
export SONATYPE_USERNAME="your-token-name"
export SONATYPE_PASSWORD="your-token-password"

# 2. Run the release command
sbt ci-release

# 3. For snapshots only (no tag)
sbt +publishSigned
```

## Additional Resources

- [sbt-ci-release documentation](https://github.com/sbt/sbt-ci-release)
- [Sonatype Central Portal](https://central.sonatype.com/)
- [sbt Publishing documentation](https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html)
- [GPG Documentation](https://www.gnupg.org/documentation/)

## Support

If you encounter issues:
1. Check GitHub Actions logs: https://github.com/vim89/toon4s/actions
2. Search existing issues: https://github.com/sbt/sbt-ci-release/issues
3. Verify Sonatype status: https://status.central.sonatype.com/
