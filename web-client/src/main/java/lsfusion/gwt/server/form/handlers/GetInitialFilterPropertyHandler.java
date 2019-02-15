package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.shared.result.NumberResult;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.gwt.server.form.FormActionHandler;
import lsfusion.gwt.shared.actions.form.GetInitialFilterProperty;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class GetInitialFilterPropertyHandler extends FormActionHandler<GetInitialFilterProperty, NumberResult> {
    public GetInitialFilterPropertyHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public NumberResult executeEx(GetInitialFilterProperty action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return form.remoteForm == null ? null : new NumberResult(form.remoteForm.getInitFilterPropertyDraw());
    }
}
