package ai.smritea.sdk.model;

import ai.smritea.sdk._internal.autogen.model.MemoryCreateMemoryResponse;
import ai.smritea.sdk._internal.autogen.model.MemoryMemoryResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Response from {@link ai.smritea.sdk.SmriteaClient#add}. Contains all memories created from the
 * extracted facts, plus extraction metadata.
 *
 * <p>A single {@code add()} call may produce multiple memories when the content contains several
 * distinct facts. Iterate {@link #getMemories()} to access each one:
 *
 * <pre>{@code
 * MemoryCreationResult result = client.add("Alice works at Acme and likes hiking", opts);
 * for (Memory mem : result.getMemories()) {
 *     System.out.println(mem.getContent());
 * }
 * System.out.println("facts extracted: " + result.getFactsExtracted());
 * }</pre>
 */
public final class MemoryCreationResult {
  private final MemoryCreateMemoryResponse inner;

  /**
   * Creates a MemoryCreationResult from the auto-generated response type. Called by SmriteaClient
   * after deserializing the server response. External callers should not use this constructor
   * directly (the autogen type is an internal implementation detail).
   *
   * @param inner the autogen response object
   */
  public MemoryCreationResult(MemoryCreateMemoryResponse inner) {
    this.inner = inner;
  }

  /**
   * Returns all memories created from the extracted facts. May contain multiple entries when the
   * content yielded several distinct facts. Never null — returns an empty list when no memories
   * were created.
   */
  public List<Memory> getMemories() {
    List<MemoryMemoryResponse> raw = inner.getMemories();
    if (raw == null || raw.isEmpty()) {
      return Collections.emptyList();
    }
    List<Memory> result = new ArrayList<>(raw.size());
    for (int i = 0; i < raw.size(); i++) {
      result.add(new Memory(raw.get(i)));
    }
    return Collections.unmodifiableList(result);
  }

  /**
   * Returns the number of facts the LLM extracted from the content, or null if the server did not
   * return this field.
   */
  public Integer getFactsExtracted() {
    return inner.getFactsExtracted();
  }

  /**
   * Returns the LLM's confidence score for the extraction (0.0–1.0), or null if the server did not
   * return this field.
   */
  public BigDecimal getExtractionConfidence() {
    return inner.getExtractionConfidence();
  }

  /**
   * Returns the number of candidate facts that were skipped (e.g. duplicates or below threshold),
   * or null if the server did not return this field.
   */
  public Integer getSkippedCount() {
    return inner.getSkippedCount();
  }

  /**
   * Returns the number of existing memories that were updated rather than created as new entries,
   * or null if the server did not return this field.
   */
  public Integer getUpdatedCount() {
    return inner.getUpdatedCount();
  }
}
