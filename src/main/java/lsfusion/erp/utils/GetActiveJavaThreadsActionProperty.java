package lsfusion.erp.utils;

import com.google.common.base.Throwables;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.sql.SQLException;

public class GetActiveJavaThreadsActionProperty extends ScriptingActionProperty {

    public GetActiveJavaThreadsActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        try {

            getActiveJavaThreads(context);

        } catch (SQLHandledException e) {
            throw Throwables.propagate(e);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }

    }

    private void getActiveJavaThreads(ExecutionContext context) throws SQLException, SQLHandledException, ScriptingErrorLog.SemanticErrorException {

        DataSession session = context.getSession();
        ThreadMXBean thMxB = ManagementFactory.getThreadMXBean();
        
        ThreadInfo[] threadsInfo = thMxB.dumpAllThreads(true, false);
            Integer previousCount = (Integer) findProperty("previousCountActiveJavaThread").read(session);
        previousCount = previousCount == null ? 0 : previousCount;

        for (int i = 0; i < previousCount; i++) {
            DataObject currentObject = new DataObject(i);
            findProperty("idActiveJavaThread").change((Object) null, session, currentObject);
            findProperty("stackTraceActiveJavaThread").change((Object) null, session, currentObject);
            findProperty("nameActiveJavaThread").change((Object) null, session, currentObject);
            findProperty("statusActiveJavaThread").change((Object) null, session, currentObject);
            findProperty("lockNameActiveJavaThread").change((Object) null, session, currentObject);
        }        
        int max = 0;
        for(ThreadInfo threadInfo : threadsInfo) {
            int id = (int) threadInfo.getThreadId();
            DataObject currentObject = new DataObject(id);

            String status = String.valueOf(threadInfo.getThreadState());
            String stackTrace = stackTraceToString(threadInfo.getStackTrace());
            String name = threadInfo.getThreadName();
            String lockName = threadInfo.getLockName();
            
            findProperty("idActiveJavaThread").change(id, session, currentObject);
            findProperty("stackTraceActiveJavaThread").change(stackTrace, session, currentObject);
            findProperty("nameActiveJavaThread").change(name, session, currentObject);
            findProperty("statusActiveJavaThread").change(status, session, currentObject);
            findProperty("lockNameActiveJavaThread").change(lockName, session, currentObject);
            if(id>max)
                max = id;
        }        
        findProperty("previousCountActiveJavaThread").change(max, session);
    }

    private String stackTraceToString(StackTraceElement[] stackTrace) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : stackTrace) {
            sb.append(element.toString());
            sb.append("\n");
        }
        return sb.toString();
    }
}