package edu.unm.health.biocomp.util.threads;

import java.util.concurrent.*;

/**	Classes implementing this interface are supplied with the Callable
	tasks to ProcessUtils.ExecTasksString() so progress info can be
	reported for various tasks during execution.  For each task, a
	class e.g. OmegaTaskStatus should be created.
*/
public interface TaskStatus
{
  public Callable task=null;
  public String status();
}
