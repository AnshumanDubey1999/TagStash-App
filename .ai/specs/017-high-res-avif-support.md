# Specification: 017-high-res-avif-support

**Status**: IN_PROGRESS

---

## 📋 Q&A & Assumptions
- **Q**: How will high-resolution AVIF images (>1080p / 4K / 8K) be decoded without hitting Android native `libgav1` software decoder caps?
  - **A**: Integrate VideoLAN `dav1d` software decoder via `io.github.awxkee:avif-coder:2.2.0` and a custom Coil 2 `AvifCoderDecoder` component in `MediaPlayerScreen.kt`. `dav1d` handles multi-threaded decoding and downsampling directly in NDK assembly, bypassing Android OS `libgav1` / `MediaCodec` sequence header restrictions.
- **Q**: Where will this decoder be applied?
  - **A**: Registered as a Coil `Decoder.Factory` component in `MediaPlayerScreen.kt`.
- **Assumptions**:
  - `dav1d` NDK decoder handles 4K, 8K, and ultra-high resolution AVIF images natively without memory caps.
  - `-Xskip-metadata-version-check` flag is added to Kotlin options to enable smooth compilation with Kotlin 2.0.21.

---

## 🛠️ Feature Requirements
- Support decoding and displaying high-resolution AVIF images.
- Integrate `io.github.awxkee:avif-coder:2.2.0` NDK decoder into Coil's decoding pipeline.
- Maintain smooth pinch-to-zoom and pan interactions in `MediaPlayerScreen.kt`.

---

## 🎨 UI/UX Changes
- High-resolution `.avif` images load clearly without falling back to error states or native decoder crash screens.
- Smooth loading transition while AVIF images are decoded.

---

## ⚙️ Android-specific & Gradle Requirements
- Add `io.github.awxkee:avif-coder:2.2.0` dependency in `app/build.gradle.kts`.
- Set `-Xskip-metadata-version-check` in `kotlinOptions.freeCompilerArgs`.

---

## 🧪 Edge Cases & Tests
- **High Resolution AVIF (4K, 8K)**: Verify AVIF images up to 8K decode without `OutOfMemoryError` or native `libgav1` codec crashes.
- **Normal Resolution AVIF (<1080p)**: Verify small AVIF images decode at original quality.
- **Unit & Build Tests**:
  - Verify Gradle build (`./gradlew compileDebugKotlin`) succeeds.
  - Verify unit tests (`./gradlew test`) pass.
  - Verify quality check (`./gradlew check`) passes.

---

## 🚀 Expected Code Changes
- Modify [build.gradle.kts](file:///Users/personal/AndroidStudioProjects/TagStash/app/build.gradle.kts)
  - Add `io.github.awxkee:avif-coder:2.2.0` dependency and `-Xskip-metadata-version-check` flag.
- Add [AvifCoderDecoder.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/data/utils/AvifCoderDecoder.kt)
  - Implement Coil 2 `Decoder` using `HeifCoder` with `decodeSampled` and `decode`.
- Modify [MediaPlayerScreen.kt](file:///Users/personal/AndroidStudioProjects/TagStash/app/src/main/java/com/anshuman/tagstash/ui/screens/MediaPlayerScreen.kt)
  - Register `AvifCoderDecoder.Factory()` in Coil's `ImageLoader`.
