package lsfusion.server.form.entity;

import lsfusion.interop.ClassViewType;
import lsfusion.server.classes.DateClass;
import lsfusion.server.form.navigator.NavigatorElement;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;

public class DateIntervalFormEntity<T extends BusinessLogics<T>> extends FormEntity<T> {

    public ObjectEntity objDateFrom;
    public ObjectEntity objDateTo;

    public DateIntervalFormEntity(BaseLogicsModule<?> LM, NavigatorElement<T> parent, String sID, String caption) {
        super(parent, sID, caption, null);

        addIntervalGroupObject(LM);
    }

    private void addIntervalGroupObject(BaseLogicsModule<?> LM) {
        GroupObjectEntity gobjDates = new GroupObjectEntity(1, "date");
        objDateFrom = new ObjectEntity(2, "dateFrom", DateClass.instance, "Дата (с)");
        objDateTo = new ObjectEntity(3, "dateTo", DateClass.instance, "Дата (по)");
        gobjDates.add(objDateFrom);
        gobjDates.add(objDateTo);

        addGroupObject(gobjDates);
        gobjDates.setSingleClassView(ClassViewType.PANEL);

        addPropertyDraw(objDateFrom, LM.objectValue);
        addPropertyDraw(objDateTo, LM.objectValue);
    }
}
