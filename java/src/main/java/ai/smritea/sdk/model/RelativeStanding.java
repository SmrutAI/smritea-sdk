package ai.smritea.sdk.model;

/**
 * Importance and temporal decay configuration for a memory.
 *
 * <p>Controls how the memory's relevance score decays over time in search results. All fields are
 * optional — omitted fields use server defaults (importance=1.0, decay_factor=0.2,
 * decay_function="exponential").
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * new RelativeStanding()
 *     .withImportance(0.9f)
 *     .withDecayFactor(0.5f)
 *     .withDecayFunction("exponential");
 * }</pre>
 */
public final class RelativeStanding {
  private Float importance;
  private Float decayFactor;
  private String decayFunction;

  /** Creates a new RelativeStanding with all fields unset. */
  public RelativeStanding() {}

  /**
   * Sets the importance score (0.0–1.0). Higher = ranks higher in search.
   *
   * @param importance the importance score
   * @return this instance for chaining
   */
  public RelativeStanding withImportance(Float importance) {
    this.importance = importance;
    return this;
  }

  /**
   * Sets the decay factor. 0 = no decay (memory score is pinned permanently). 0.2 = light decay
   * (default). 1.0 = standard. 3.0+ = aggressive.
   *
   * @param decayFactor the decay factor
   * @return this instance for chaining
   */
  public RelativeStanding withDecayFactor(Float decayFactor) {
    this.decayFactor = decayFactor;
    return this;
  }

  /**
   * Sets the decay curve shape. Accepted values: "exponential", "gaussian", "linear".
   *
   * @param decayFunction the decay function name
   * @return this instance for chaining
   */
  public RelativeStanding withDecayFunction(String decayFunction) {
    this.decayFunction = decayFunction;
    return this;
  }

  /** Returns the importance score. */
  public Float getImportance() {
    return importance;
  }

  /** Returns the decay factor. */
  public Float getDecayFactor() {
    return decayFactor;
  }

  /** Returns the decay function name. */
  public String getDecayFunction() {
    return decayFunction;
  }
}
