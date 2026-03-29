package ai.smritea.sdk.model;

import java.util.Map;

/**
 * Builder-style options for adding a memory.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * new AddOptions()
 *     .withScope(MemoryScope.builder().actorId("alice").actorType("user").build())
 *     .withMetadata(Map.of("key", "value"));
 * }</pre>
 */
public final class AddOptions {
  private MemoryScope scope;
  private Map<String, Object> metadata;

  /** Creates a new AddOptions with all fields unset. */
  public AddOptions() {}

  /**
   * Sets the memory scope (actor and conversation context).
   *
   * @param scope the memory scope
   * @return this instance for chaining
   */
  public AddOptions withScope(MemoryScope scope) {
    this.scope = scope;
    return this;
  }

  /**
   * Sets arbitrary key-value metadata.
   *
   * @param metadata the metadata map
   * @return this instance for chaining
   */
  public AddOptions withMetadata(Map<String, Object> metadata) {
    this.metadata = metadata;
    return this;
  }

  /** Returns the memory scope. */
  public MemoryScope getScope() {
    return scope;
  }

  /** Returns the metadata map. */
  public Map<String, Object> getMetadata() {
    return metadata;
  }
}
