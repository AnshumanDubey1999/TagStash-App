# Specification: 016-media-preloading

**Status**: DONE

---

## 📋 Q&A & Assumptions
- **Q**: What preloading range should we cover?
  - **A**: 2 next items and 1 previous item.
- **Q**: What is the loading priority order?
  - **A**: First next item (`currentIndex + 1`), then first previous item (`currentIndex - 1`), and then second next item (`currentIndex + 2`).
- **Q**: How do we handle rapid navigation and cancel old preload requests?
  - **A**: We run the preloader inside a `LaunchedEffect` and use Coil's suspending `imageLoader.execute()` instead of `enqueue()`. This ensures that when the user moves to another file, the current preloading coroutine is cancelled, immediately aborting the active image download/decoding.
- **Q**: How do we manage memory and evict old entries?
  - **A**: At the end of the preloader routine, we prune the `dimensionCache` map to retain only the active file and the three preloaded sibling files. Coil's bitmap memory cache uses an LRU policy to clean up old images automatically.

---

## 🛠️ Feature Requirements
- Maintain a local `dimensionCache` map (`mutableStateMapOf<File, ImageDimensions>()`) in `MediaPlayerScreen.kt`.
- Implement a preloading `LaunchedEffect(file, siblingMedia, currentIndex)`:
  - Generate target preloading indices: `[currentIndex + 1, currentIndex - 1, currentIndex + 2]`.
  - Sequentially process target indices:
    - Skip if index is out of bounds of `siblingMedia`.
    - Retrieve `targetFile = siblingMedia[index]`.
    - If `targetFile` is an image:
      - Build Coil `ImageRequest` with hardware acceleration disabled for AVIF files.
      - Call suspending `imageLoader.execute()` inside a `try-catch` block.
      - Catch `CancellationException` and rethrow it to propagate cancellation, while logging/suppressing other exceptions (so one failure doesn't break the loop).
      - If `targetFile` is not in `dimensionCache`, run `getImageDimensions()` on `Dispatchers.IO` and store in the cache.
  - Prune `dimensionCache` after the loop to only keep the active file and the files at the target indices.
- Update the active image dimensions lookup to use the `dimensionCache` as a fast path.

---

## 🎨 UI/UX Changes
- Instant visual transition when loading cached images (no loading spinner).
- Visual layout jumps/snaps are prevented since dimensions are cached.

---

## 🧪 Edge Cases & Tests
- **Index out of bounds**: Handled by validating target indices.
- **Preload Failure**: Network/IO failures for a specific item are caught so preloading continues for subsequent items.
- **Coroutine Cancellation**: Verified that the preloading process cancels cooperatively.
- **Unit Tests**:
  - Add a unit test verifying the priority index selection logic under different edge conditions (e.g. start of list, end of list, empty list).
