package edu.unm.health.biocomp.util.threads;

import java.io.*;
import java.util.*;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.*;
import java.lang.Thread;

/**	Static methods for thread and task control.  For all Threads,
	external Processes or internal.
	@author Jeremy J Yang
*/
public class TaskUtils
{
  /**	Execute task; indicate progress to stdout.
  */
  public static <T> T ExecTask(ExecutorService exec,Callable<T> task,
	String name,int tpoll)
  {
    T val=null;
    Future<T> fut = exec.submit(task);
    try {
      while (!fut.isDone())
      {
        System.out.println(name+"...");
        Thread.currentThread().sleep(tpoll);
      }
      Thread.currentThread().sleep(500); //kludge; wait for process exit
      val=fut.get(); // get() blocks until completion
    } catch (InterruptedException e) {
      System.out.println(e);
    } catch (CancellationException e) {
      System.out.println(e);
    } catch (ExecutionException e) {
      System.out.println(e);
    } finally {
      exec.shutdown();
    }
    return val;
  }
  /**	Execute task; indicate progress, as reported by
	TaskStatus object, to stdout.
  */
  public static <T> T ExecTask(ExecutorService exec, Callable<T> task,
		TaskStatus tst,
		String name,int tpoll)
  {
    T val=null;
    Future<T> fut= exec.submit(task);
    try {
      while (!fut.isDone())
      {
        System.out.println(name+": "+tst.status()+"...");
        Thread.currentThread().sleep(tpoll);
      }
      Thread.currentThread().sleep(500); //kludge; wait for process exit
      val=fut.get(); // get() blocks until completion
    } catch (InterruptedException e) {
      System.out.println(e);
    } catch (CancellationException e) {
      System.out.println(e);
    } catch (ExecutionException e) {
      System.out.println(e);
    } finally {
      exec.shutdown();
    }
    return val;
  }
  /**	Execute task; indicate progress, as reported by
	TaskStatus object, to PrintWriter object, and 
	HttpServletResponse object, to named browser DOM window.
	Exceptions logged to stderr.
	Is there a way to send error txt to application?  Throw Exception ?  ExecutionException ?
  */
  public static <T> T ExecTaskWeb(ExecutorService exec, Callable<T> task,
		TaskStatus tst,
		String name,int tpoll,
		PrintWriter out,
		HttpServletResponse response,
		String progresswin)
	throws IOException
  {
    out.println(
"<SCRIPT>\n"+
"var pwin=window.open('','"+progresswin+"');\n"+
"pwin.document.writeln('"+name+": task launching...<BR>');\n"+
"</SCRIPT>");
    out.flush();
    response.flushBuffer();

    T val=null;
    Future<T> fut = exec.submit(task);
    try {
      while (!fut.isDone())
      {
        out.println(
"<SCRIPT>\n"+
"pwin.document.writeln('"+name+": "+tst.status()+" ...<BR>');\n"+
"if (navigator.appName.match('Explorer')) pwin.scrollTo(0,99999);\n"+
"else pwin.scrollTo(0, pwin.document.body.offsetHeight);\n"+
"</SCRIPT>");
        out.flush();
        response.flushBuffer();
        Thread.currentThread().sleep(tpoll);
      }
      Thread.currentThread().sleep(500); //kludge; wait for process exit
      val=fut.get(); // get() blocks until completion
    } catch (InterruptedException e) {
      System.err.println(e);
    } catch (CancellationException e) {
      System.err.println(e);
    } catch (ExecutionException e) {
      System.err.println(e);
    } finally {
      exec.shutdown();
    }
    out.println(
"<SCRIPT>\n"+
"pwin.document.writeln('"+name+": "+tst.status()+" (done).<BR>');\n"+
"if (navigator.appName.match('Explorer')) pwin.scrollTo(0,99999);\n"+
"else pwin.scrollTo(0, pwin.document.body.offsetHeight);\n"+
"</SCRIPT>");
    out.flush();
    response.flushBuffer();
    return val;
  }

  /**	Execute multiple tasks; indicate progress, as reported by
	TaskStatus objects, to stdout.
  */
  public static <T> ArrayList<T> ExecTasks(ExecutorService exec,
		ArrayList<Callable<T>> tasks,
		ArrayList<TaskStatus> taskstatuses,
		String name,int tpoll)
  {
    ArrayList<Future<T>> futs = new ArrayList<Future<T>>();
    ArrayList<T> results = new ArrayList<T>();

    Future<T> fut = null;
    TaskStatus tst = null;
    HashMap<Integer,Boolean> is_done = new HashMap<Integer,Boolean>();
    for (int i=0;i<tasks.size();++i)
    {
      fut=exec.submit(tasks.get(i));
      futs.add(fut);
      is_done.put(i,false);
    }
    int n_done=0;
    for (int i=0;n_done<tasks.size();++i)
    {
      i%=tasks.size();
      if (is_done.get(i)) continue;
      fut=futs.get(i);
      tst=taskstatuses.get(i);
      try
      {
        if (fut.isDone())
        {
          ++n_done;
          is_done.put(i,true);
          Thread.currentThread().sleep(500); //kludge; wait for process exit
          T val=fut.get(); // get() blocks until completion
          results.add(val);
          System.out.print(name+" ["+(i+1)+"]:");
          System.out.println(" ("+tst.status()+" ) done.");
        }
        else if (fut.isCancelled())
        {
          ++n_done;
          is_done.put(i,true);
        }
        else
        {
          String statmsg=tst.status();
          System.out.print(name+" ["+(i+1)+"]:");
          if (!statmsg.isEmpty())
            System.out.print(" ("+statmsg+" )");
          System.out.println(" ...");
          Thread.currentThread().sleep(tpoll);
        }
      } catch (InterruptedException e) {
        System.out.println(e);
      } catch (CancellationException e) {
        System.out.println(e);
      } catch (ExecutionException e) {
        System.out.println(e);
      } finally {
        exec.shutdown();
      }
    }
    return results;
  }
}
