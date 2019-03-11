package lsfusion.http;

import com.google.common.base.Throwables;
import lsfusion.gwt.shared.exceptions.AppServerNotAvailableException;
import lsfusion.http.provider.logics.LogicsProvider;
import lsfusion.http.provider.logics.LogicsProviderImpl;
import lsfusion.http.provider.logics.LogicsRunnable;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

// native interfaces because either we need to use spring, or we can't use gwt
public class LogicsRequestHandler {

    @Autowired
    protected LogicsProvider logicsProvider;

    protected <R> R runRequest(HttpServletRequest request, LogicsRunnable<R> runnable) throws IOException {
        try {
            return LogicsProviderImpl.runRequest(logicsProvider, request, runnable);
        } catch (AppServerNotAvailableException e) {
            throw Throwables.propagate(e);
        }
    }

}
