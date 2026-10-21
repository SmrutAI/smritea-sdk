package ai.smritea.sdk.model;

/**
 * Public-facing trace of one step inside a pipeline stage. Delegates to the auto-generated {@link
 * ai.smritea.sdk._internal.autogen.model.StepTrace}.
 */
public final class StepTrace {
  private final ai.smritea.sdk._internal.autogen.model.StepTrace inner;

  /**
   * Creates a StepTrace from the auto-generated type. Called by the SDK after deserializing the
   * server response. External callers should not use this constructor directly (the autogen type is
   * an internal implementation detail).
   *
   * @param inner the autogen step trace
   */
  public StepTrace(ai.smritea.sdk._internal.autogen.model.StepTrace inner) {
    this.inner = inner;
  }

  /** Returns the step name, or null if the server did not return it. */
  public String getStepName() {
    return inner.getStepName();
  }

  /** Returns the step duration in milliseconds, or null if the server did not return it. */
  public Integer getDurationMs() {
    return inner.getDurationMs();
  }

  /** Returns the error text of the step, or null if the step did not fail. */
  public String getError() {
    return inner.getError();
  }

  /** Returns the step input summary, or null if the server did not return it. */
  public String getInput() {
    return inner.getInput();
  }

  /** Returns the step output summary, or null if the server did not return it. */
  public String getOutput() {
    return inner.getOutput();
  }

  /** Returns the number of results the step produced, or null if the server did not return it. */
  public Integer getResultCount() {
    return inner.getResultCount();
  }
}
