package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.FormActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;
import lsfusion.gwt.shared.base.result.VoidResult;
import lsfusion.gwt.shared.actions.form.FormHidden;

public class FormHiddenHandler extends FormActionHandler<FormHidden, VoidResult> {
    public FormHiddenHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public VoidResult executeEx(FormHidden action, ExecutionContext context) {
        removeFormSessionObject(action.formSessionID);
        return new VoidResult();
    }
}
