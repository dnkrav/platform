package platform.server.logics.service;

import platform.server.classes.ValueClass;
import platform.server.logics.ServiceLogicsModule;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;

import java.sql.SQLException;

public class ToggleExplainAnalyzeModeActionProperty extends ScriptingActionProperty {

    public ToggleExplainAnalyzeModeActionProperty(ServiceLogicsModule LM) {
        super(LM, new ValueClass[]{});
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        context.getSession().sql.toggleExplainAnalyzeMode();
    }
}
