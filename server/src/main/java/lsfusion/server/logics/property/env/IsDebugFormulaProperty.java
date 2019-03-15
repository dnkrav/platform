package lsfusion.server.logics.property.env;

import lsfusion.server.data.SQLSession;
import lsfusion.server.logics.classes.data.LogicalClass;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class IsDebugFormulaProperty extends CurrentEnvironmentFormulaProperty {

    public final static IsDebugFormulaProperty instance = new IsDebugFormulaProperty();

    private IsDebugFormulaProperty() {
        super(LocalizedString.create("{logics.property.current.isdebug}"), SQLSession.isDebugParam, LogicalClass.instance);

        finalizeInit();
    }
}