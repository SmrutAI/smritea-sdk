package ai.smritea.sdk.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Public-facing explain trace of a request: the pipeline stages that ran, with timings. Delegates
 * to the auto-generated {@link ai.smritea.sdk._internal.autogen.model.Trace}.
 */
public final class Trace {
  private final ai.smritea.sdk._internal.autogen.model.Trace inner;

  /**
   * Creates a Trace from the auto-generated type. Called by the SDK after deserializing the server
   * response. External callers should not use this constructor directly (the autogen type is an
   * internal implementation detail).
   *
   * @param inner the autogen trace
   */
  public Trace(ai.smritea.sdk._internal.autogen.model.Trace inner) {
    this.inner = inner;
  }

  /**
   * Returns the pipeline stages that ran. Never null: returns an empty list when there are none.
   */
  public List<StageTrace> getStages() {
    List<ai.smritea.sdk._internal.autogen.model.StageTrace> raw = inner.getStages();
    if (raw == null || raw.isEmpty()) {
      return Collections.emptyList();
    }
    List<StageTrace> result = new ArrayList<>(raw.size());
    for (int i = 0; i < raw.size(); i++) {
      result.add(new StageTrace(raw.get(i)));
    }
    return Collections.unmodifiableList(result);
  }

  /** Returns the total duration in milliseconds, or null if the server did not return it. */
  public Integer getTotalMs() {
    return inner.getTotalMs();
  }
}
