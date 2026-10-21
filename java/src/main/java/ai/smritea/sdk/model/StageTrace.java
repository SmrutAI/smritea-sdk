package ai.smritea.sdk.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Public-facing trace of one pipeline stage. Delegates to the auto-generated {@link
 * ai.smritea.sdk._internal.autogen.model.StageTrace}.
 */
public final class StageTrace {
  private final ai.smritea.sdk._internal.autogen.model.StageTrace inner;

  /**
   * Creates a StageTrace from the auto-generated type. Called by the SDK after deserializing the
   * server response. External callers should not use this constructor directly (the autogen type is
   * an internal implementation detail).
   *
   * @param inner the autogen stage trace
   */
  public StageTrace(ai.smritea.sdk._internal.autogen.model.StageTrace inner) {
    this.inner = inner;
  }

  /** Returns the stage name, or null if the server did not return it. */
  public String getStageName() {
    return inner.getStageName();
  }

  /** Returns the stage duration in milliseconds, or null if the server did not return it. */
  public Integer getDurationMs() {
    return inner.getDurationMs();
  }

  /** Returns the error text of the stage, or null if the stage did not fail. */
  public String getError() {
    return inner.getError();
  }

  /** Returns the stage input summary, or null if the server did not return it. */
  public String getInput() {
    return inner.getInput();
  }

  /** Returns the stage output summary, or null if the server did not return it. */
  public String getOutput() {
    return inner.getOutput();
  }

  /** Returns the number of results the stage produced, or null if the server did not return it. */
  public Integer getResultCount() {
    return inner.getResultCount();
  }

  /** Returns the steps of this stage. Never null: returns an empty list when there are none. */
  public List<StepTrace> getSteps() {
    List<ai.smritea.sdk._internal.autogen.model.StepTrace> raw = inner.getSteps();
    if (raw == null || raw.isEmpty()) {
      return Collections.emptyList();
    }
    List<StepTrace> result = new ArrayList<>(raw.size());
    for (int i = 0; i < raw.size(); i++) {
      result.add(new StepTrace(raw.get(i)));
    }
    return Collections.unmodifiableList(result);
  }
}
