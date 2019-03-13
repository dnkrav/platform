package lsfusion.server.logics.tasks;

import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.DBManager;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;

public abstract class BLTask extends PublicTask {

    private BusinessLogics BL;

    public BusinessLogics getBL() {
        return BL;
    }

    public void setBL(BusinessLogics BL) {
        this.BL = BL;
    }

    protected DBManager getDbManager() {
        return getBL().getDbManager();
    }

    protected DataSession createSession() throws SQLException {
        return getDbManager().createSession();
    }
}