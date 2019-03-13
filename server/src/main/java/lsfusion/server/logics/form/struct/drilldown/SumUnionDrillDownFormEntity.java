package lsfusion.server.logics.form.struct.drilldown;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.SumUnionProperty;

public class SumUnionDrillDownFormEntity<I extends PropertyInterface> extends UnionDrillDownFormEntity<SumUnionProperty.Interface, SumUnionProperty> {

    public SumUnionDrillDownFormEntity(String sID, LocalizedString caption, SumUnionProperty property, LogicsModule LM) {
        super(sID, caption, property, LM);
    }
}