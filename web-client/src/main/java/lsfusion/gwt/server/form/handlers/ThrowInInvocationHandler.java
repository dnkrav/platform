package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.gwt.shared.actions.form.ServerResponseResult;
import lsfusion.gwt.shared.actions.form.ThrowInInvocation;

import java.io.IOException;

public class ThrowInInvocationHandler extends FormServerResponseActionHandler<ThrowInInvocation> {
    public ThrowInInvocationHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ThrowInInvocation action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return getServerResponseResult(form, form.remoteForm.throwInServerInvocation(-1, defaultLastReceivedRequestIndex, -1, action.throwable) );
    }
}
