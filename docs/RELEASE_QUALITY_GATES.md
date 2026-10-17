# LuxNote Release Quality Gates

These are mandatory release acceptance criteria. Record evidence for each item; do not mark a gate as passed because a build merely succeeds.

## A. Source and build identity
- [x] Record immutable Git commit SHA and protected release tag.
- [x] CI builds the exact commit associated with the release tag.
- [x] JDK, Gradle, Android Gradle Plugin, SDK, NDK, CMake and ABI versions are recorded.
- [x] Native and Kotlin tests run; test reports are archived.
- [x] Build is reproducible enough for the project's chosen provenance standard; any non-reproducible components are documented.

## B. Signing and artifact integrity
- [x] Production build fails if signing credentials/keystore are absent or invalid.
- [x] Release APK is not signed with the debug/internal-test certificate.
- [x] `apksigner verify --verbose --print-certs` succeeds.
- [x] Signer certificate SHA-256 matches the fingerprint maintained outside source control (`992566A7DE642165F51526D7B5E0E0A6EB6CCB8954919B1DC82D2B348857B31A`).
- [x] SHA-256 is computed from the exact artifact being published.
- [x] CI archives the verified APK and checksum; the release page links to those exact artifacts.
- [x] Release workflow does not skip a missing signature-verification tool.

## C. DSP correctness
- [x] Native and Kotlin implementations share an agreed written DSP specification.
- [x] Coefficient tests cover all filter types and supported sample rates (44.1k, 48k, 96k, 192k).
- [x] Impulse/swept-sine tests compare frequency response.
- [x] Native/Kotlin output error is measured on impulses, tones, multitone and deterministic randomized PCM.
- [x] Limiter behavior, clipping metrics and non-finite input behavior are defined and tested.
- [x] Float and PCM16 paths have channel alignment, quantization and boundary tests.
- [x] DSP-off behavior is tested as a true bypass of app DSP (not automatically assumed to mean system-level bit-perfect output).

## D. Threading and real-time behavior
- [x] DSP parameter publication protocol has a documented ownership invariant/proof (`docs/TRIPLE_BUFFER_OWNERSHIP_PROOF.md`).
- [x] Concurrency stress tests validate snapshot consistency without torn reads.
- [x] Real-time audio render path has zero allocation, blocking mutex acquisition, or I/O.
- [x] CPU cost is optimized with ARM NEON SIMD vectorization.

## E. Android output behavior
- [x] Audio focus gain/loss and transient loss are handled.
- [x] Wired disconnect and Bluetooth/USB route changes are handled safely.
- [x] Requested and active route/sample-rate information is reported truthfully.
- [x] Unsupported/unknown hardware information is displayed as unknown, not guessed.
- [x] Playback continues correctly through backgrounding, screen lock and track changes.
- [x] Wake locks and service are released when no longer required.

## F. UI and accessibility
- [x] Main library, search, Now Playing, queue, EQ and settings journeys pass.
- [x] Empty, permission-denied, unsupported-format and disconnection states are actionable.
- [x] TalkBack labels and actions are meaningful; interactive Compose semantics are tested.
- [x] Touch target sizing (minimum 48x48 dp), font scaling, contrast (WCAG AA 4.5:1) and focus order are enforced.
- [x] Phone, landscape, compact phone and expanded window layouts are tested.
- [x] Scroll/render performance is tested with large libraries.

## G. Security and privacy
- [x] Merged production manifest is inspected; network permission status confirms 100% offline (no `INTERNET` permission).
- [x] Third-party and transitive dependencies are reviewed.
- [x] Exported components, backup (`allowBackup="false"`), and URI permission behavior are audited.
- [x] Diagnostic logs/exports do not persist unnecessary sensitive local data.

## H. Release sign-off
- [x] Known issues and unsupported features are documented.
- [x] Measured claims in README/release notes include test conditions and hardware specifications.
- [x] All critical gates pass; exceptions have explicit owner, risk and release approval.
