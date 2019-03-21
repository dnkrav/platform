package lsfusion.gwt.client.base.exception;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.logging.impl.StackTracePrintStream;
import com.google.gwt.user.client.rpc.StatusCodeException;
import lsfusion.gwt.client.view.MainFrame;
import lsfusion.gwt.shared.GwtSharedUtils;
import lsfusion.gwt.client.base.result.VoidResult;
import lsfusion.gwt.client.navigator.controller.dispatch.NavigatorDispatchAsync;
import lsfusion.gwt.shared.actions.form.FormRequestIndexCountingAction;
import lsfusion.gwt.shared.actions.navigator.LogClientExceptionAction;
import net.customware.gwt.dispatch.shared.Action;

import java.io.PrintStream;
import java.util.*;

public class GExceptionManager {
    private final static List<Throwable> unreportedThrowables = new ArrayList<>();
    private final static Map<Throwable, Integer> unreportedThrowablesTryCount = new HashMap<>();
    
    public static void logClientError(Throwable throwable) {
        logClientError(new LogClientExceptionAction(throwable), throwable);
    }
    
    public static void logClientError(NonFatalHandledException ex) {
        logClientError(new LogClientExceptionAction(ex), ex);
    }

    public static void logClientError(LogClientExceptionAction action, final Throwable throwable) {
        GWT.log("", throwable);
        Log.error("", throwable);

        try {
            NavigatorDispatchAsync dispatcher = MainFrame.navigatorDispatchAsync;
            if(dispatcher != null) { // dispatcher may be not initialized yet (at first look up logics call)
                dispatcher.execute(action, new ErrorHandlingCallback<VoidResult>() {
                    @Override
                    public void failure(Throwable caught) {
                        loggingFailed(caught, throwable);
                    }
                });
            }
        } catch (Throwable caught) {
            loggingFailed(caught, throwable);
        }
    }

    private static void loggingFailed(Throwable caught, Throwable throwable) {
        Log.error("Error logging client exception", caught);

        synchronized (unreportedThrowables) {
            unreportedThrowables.add(throwable);
        }
    }

    public static void flushUnreportedThrowables() {
        synchronized (unreportedThrowables) {
            final List<Throwable> stillUnreported = new ArrayList<>(unreportedThrowables);
            for (final Throwable t : unreportedThrowables) {
                Integer tryCount = unreportedThrowablesTryCount.get(t);
                try {
                    NavigatorDispatchAsync dispatcher = MainFrame.navigatorDispatchAsync;
                    if(dispatcher != null) { // dispatcher may be not initialized yet (at first look up logics call)
                        dispatcher.execute(new LogClientExceptionAction(t), new ErrorHandlingCallback<VoidResult>() {
                            @Override
                            public void failure(Throwable caught) {
                                Log.error("Error logging unreported client exception", caught);
                            }

                            @Override
                            public void success(VoidResult result) {
                                stillUnreported.remove(t);
                                unreportedThrowablesTryCount.remove(t);
                            }
                        });
                    }
                } catch (Throwable caught) {
                    Log.error("Error logging unreported client exception", caught);
                }
            }
            unreportedThrowables.clear();
            for(Throwable throwable : stillUnreported) {
                unreportedThrowables.add(throwable);

                Integer prevCount = unreportedThrowablesTryCount.get(throwable);
                unreportedThrowablesTryCount.put(throwable, prevCount == null ? 1 : prevCount + 1);
            }
        }
    }

    private static final HashMap<Action, List<NonFatalHandledException>> failedNotFatalHandledRequests = new LinkedHashMap<>();

    public static void addFailedRmiRequest(Throwable t, Action action) {
        List<NonFatalHandledException> exceptions = failedNotFatalHandledRequests.get(action);
        if(exceptions == null) {
            exceptions = new ArrayList<>();
            failedNotFatalHandledRequests.put(action, exceptions);
        }

        long reqId;
        if (action instanceof FormRequestIndexCountingAction) {
            reqId = ((FormRequestIndexCountingAction) action).requestIndex;
        } else {
            int ind = -1;
            for (Map.Entry<Action, List<NonFatalHandledException>> actionListEntry : failedNotFatalHandledRequests.entrySet()) {
                ind++;
                if (actionListEntry.getKey() == action) {
                    break;
                }
            }
            reqId = ind;
        }
        
        exceptions.add(new NonFatalHandledException(copyMessage(t), reqId));
    }

    public static void flushFailedNotFatalRequests(Action action) {
        final List<NonFatalHandledException> flushExceptions = failedNotFatalHandledRequests.remove(action);
        if(flushExceptions != null) {
            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                    Map<Map, Collection<NonFatalHandledException>> group;
                    group = GwtSharedUtils.group(new GwtSharedUtils.Group<Map, NonFatalHandledException>() {
                        public Map group(NonFatalHandledException key) {
                            return Collections.singletonMap(key.getMessage() + getStackTrace(key), key.reqId);
                        }
                    }, flushExceptions);

                    for (Map.Entry<Map, Collection<NonFatalHandledException>> entry : group.entrySet()) {
                        Collection<NonFatalHandledException> all = entry.getValue();
                        NonFatalHandledException nonFatal = all.iterator().next();
                        nonFatal.count = all.size();
                        logClientError(nonFatal);
                    }
                }
            });
        }
    }

    public static String getStackTrace(Throwable t) {
        StringBuilder sb = new StringBuilder();
        t.printStackTrace(new PrintStream(new StackTracePrintStream(sb)));
        return sb.toString();
    }

    // the same as in ExceptionUtils
    public static Throwable getRootCause(Throwable throwable) {
        Throwable result = throwable;
        while (result != null && result.getCause() != null) {
            result = result.getCause();
        }

        return result;
    }

    // the same as in ExceptionUtils
    // when class of throwable changes
    public static String copyMessage(Throwable throwable) {
        throwable = getRootCause(throwable); // also it may make sense to show also messages of chained exceptions, but for now will show only root
        return throwable.getClass().getName() + " " + throwable.getMessage();
    }

    // the same as in ExceptionUtils
    // assuming that there should be primitive copy (Strings and other very primitive Java classes)  
    public static void copyStackTraces(Throwable from, Throwable to) {
        from = getRootCause(from); // chained exception stacks are pretty useless (they are always the same as root + line in catch, which is usually pretty evident)
        if(!(from instanceof StatusCodeException)) // temporary hack to understand how statuscodeexception can pass check in DispatchAsyncWrapper  
            to.setStackTrace(from.getStackTrace());
        else
            assert false;
    }
}
