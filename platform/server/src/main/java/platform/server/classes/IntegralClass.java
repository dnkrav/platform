package platform.server.classes;

import net.sf.jasperreports.engine.JRAlignment;
import platform.server.data.sql.SQLSyntax;
import platform.server.view.form.client.report.ReportDrawField;

import java.text.Format;
import java.text.NumberFormat;

// класс который можно сравнивать
public abstract class IntegralClass<T extends Number> extends DataClass<T> {

    public int getMinimumWidth() { return 45; }
    public int getPreferredWidth() { return 80; }

    public Format getDefaultFormat() {
        return NumberFormat.getInstance();
    }

    public void fillReportDrawField(ReportDrawField reportField) {
        super.fillReportDrawField(reportField);

        reportField.alignment = JRAlignment.HORIZONTAL_ALIGN_RIGHT;
    }

    abstract int getWhole();
    abstract int getPrecision();

    public DataClass getCompatible(DataClass compClass) {
        if(!(compClass instanceof IntegralClass)) return null;

        IntegralClass integralClass = (IntegralClass)compClass;
        if(getWhole()>=integralClass.getWhole() && getPrecision()>=integralClass.getPrecision())
            return this;
        if(getWhole()<=integralClass.getWhole() && getPrecision()<=integralClass.getPrecision())
            return integralClass;
        if(getWhole()>integralClass.getWhole())
            return NumericClass.get(getWhole()+integralClass.getPrecision(), integralClass.getPrecision());
        else
            return NumericClass.get(integralClass.getWhole()+getPrecision(), getPrecision());
    }

    public boolean isSafeString(Object value) {
        return true;
    }
    public String getString(Object value, SQLSyntax syntax) {
        return value.toString();
    }
}
