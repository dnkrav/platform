package lsfusion.http.provider.session;

import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.session.RemoteSessionInterface;

public class SessionSessionObject {

    public final RemoteSessionInterface remoteSession;

    public SessionSessionObject(RemoteSessionInterface remoteSession) {
        this.remoteSession = remoteSession;
    }
}
