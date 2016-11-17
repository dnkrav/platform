package lsfusion.server.logics.property.actions.importing.csv;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.actions.importing.ImportDataActionProperty;
import lsfusion.server.logics.property.actions.importing.ImportIterator;

import java.util.List;

public class ImportCSVDataActionProperty extends ImportDataActionProperty {
    private String separator;
    private boolean noHeader;
    private String charset;

    public ImportCSVDataActionProperty(ValueClass valueClass, List<String> ids, List<LCP> properties,
                                       String separator, boolean noHeader, String charset, BaseLogicsModule baseLM) {
        super(new ValueClass[] {valueClass}, ids, properties, baseLM);
        this.separator = separator == null ? "|" : separator;
        this.noHeader = noHeader;
        this.charset = charset;
    }

    @Override
    public ImportIterator getIterator(byte[] file) {
        return new ImportCSVIterator(file, getSourceColumns(XLSColumnsMapping), charset, separator, noHeader);
    }
}
