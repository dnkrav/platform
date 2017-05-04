package lsfusion.server.logics.property.actions;

import com.google.common.base.Throwables;
import jasperapi.ClientReportData;
import jasperapi.ReportGenerator;
import jasperapi.ReportPropertyData;
import lsfusion.base.IOUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.FormPrintType;
import lsfusion.interop.action.LogMessageClientAction;
import lsfusion.interop.action.ReportClientAction;
import lsfusion.interop.form.ReportGenerationData;
import lsfusion.interop.form.ReportGenerationDataType;
import lsfusion.server.SystemProperties;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.FormSelector;
import lsfusion.server.form.entity.ObjectSelector;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.*;
import net.sf.jasperreports.engine.JRException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class PrintActionProperty<O extends ObjectSelector> extends FormStaticActionProperty<O, FormPrintType> {

    private CalcPropertyInterfaceImplement<ClassPropertyInterface> printerProperty;

    private final LCP formPageCount;

    private final boolean syncType; // static interactive

    public PrintActionProperty(LocalizedString caption,
                               FormSelector<O> form,
                               final List<O> objectsToSet,
                               final List<Boolean> nulls,
                               FormPrintType staticType,
                               boolean syncType,
                               Integer top,
                               LCP formExportFile,
                               CalcPropertyMapImplement printer,
                               ImOrderSet<PropertyInterface> innerInterfaces,
                               LCP formPageCount) {
        super(caption, form, objectsToSet, nulls, staticType, formExportFile, top, printer == null ? null : printer.property);

        this.formPageCount = formPageCount;

        this.syncType = syncType;

        if (printer != null) {
            ImRevMap<PropertyInterface, ClassPropertyInterface> mapInterfaces = getMapInterfaces(innerInterfaces).reverse();
            this.printerProperty = printer.map(mapInterfaces);
        }
    }

    @Override
    protected Map<String, byte[]> exportPlain(ReportGenerationData reportData) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected byte[] exportHierarchical(ReportGenerationData reportData) throws JRException, IOException, ClassNotFoundException {
        return IOUtils.getFileBytes(ReportGenerator.exportToFile(reportData, (FormPrintType) staticType));
    }

    @Override
    protected void exportClient(ExecutionContext<ClassPropertyInterface> context, LocalizedString caption, ReportGenerationData reportData, Map<String, String> reportPath) throws SQLException, SQLHandledException {
        if (staticType == FormPrintType.MESSAGE) {
            printMessage(caption, context, reportData);
        } else {
            String pName = printerProperty == null ? null : (String) printerProperty.read(context, context.getKeys());
            Object pageCount = context.requestUserInteraction(new ReportClientAction(reportPath, syncType, reportData, (FormPrintType) staticType, pName, SystemProperties.isDebug));
            formPageCount.change(pageCount, context);
        }
    }

    private void printMessage(LocalizedString caption, ExecutionContext context, ReportGenerationData reportData) {

        try {

            Map<String, ClientReportData> data = ReportGenerator.retrieveReportSources(reportData, null, ReportGenerationDataType.PRINTMESSAGE).data;
            Map<String, Map<String, String>> propertyCaptionsMap = ReportGenerator.retrievePropertyCaptions(reportData);
            assert data.size() == 1;
            for (Map.Entry<String, ClientReportData> dataEntry : data.entrySet()) {
                String key = dataEntry.getKey();
                ClientReportData clientData = dataEntry.getValue();
                Map<String, ReportPropertyData> properties = clientData.getProperties();
                List<Pair<String, Boolean>> titles = getTitles(properties, key);

                List<String> messages = new ArrayList<>();
                List<List<String>> dataRows = new ArrayList();
                for (HashMap<Integer, Object> keyRow : clientData.getKeyRows()) {
                    Map<ReportPropertyData, Object> row = clientData.getRows().get(keyRow);
                    if (row != null) {
                        List<String> dataRow = new ArrayList<>();
                        List<String> dataPanel = new ArrayList<>();
                        for (int i = 0; i < titles.size(); i++) {
                            Pair<String, Boolean> title = titles.get(i);
                            Object value = row.get(properties.get(title.first));
                            if (title.second)
                                dataRow.add(String.valueOf(value));
                            else if (value != null)
                                dataPanel.add(String.valueOf(value));
                        }
                        dataRows.add(dataRow);
                        if(messages.isEmpty())
                            messages.addAll(dataPanel);
                    }
                }

                LogMessageClientAction action = new LogMessageClientAction(getMessage(caption, messages),
                        getTitleRow(titles, propertyCaptionsMap.get(key)), dataRows, !context.getSession().isNoCancelInTransaction());
                if(syncType)
                    context.requestUserInteraction(action);
                else
                    context.delayUserInteraction(action);

            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private String getMessage(LocalizedString caption, List<String> messages) {
        StringBuilder builder = new StringBuilder(ThreadLocalContext.localize(caption));
        for (String message : messages)
            builder.append(builder.length() == 0 ? "" : "\n").append(message);
        return builder.toString();
    }

    private List<Pair<String, Boolean>> getTitles(Map<String, ReportPropertyData> properties, String toDraw) {
        List<Pair<String, Boolean>> titleRow = new ArrayList<>();
        for(Map.Entry<String, ReportPropertyData> property : properties.entrySet()) {
            ReportPropertyData propertyData = property.getValue();
            if(!propertyData.propertyType.equals("ActionClass"))
                titleRow.add(Pair.create(property.getKey(), propertyData.toDraw.equals(toDraw)));
        }
        return titleRow;
    }

    private List<String> getTitleRow(List<Pair<String, Boolean>> titles, Map<String, String> propertyCaptions) {
        List<String> titleRow = new ArrayList<>();
        for(Pair<String, Boolean> title : titles) {
            if(title.second)
                titleRow.add(propertyCaptions.get(title.first));
        }
        return titleRow;
    }
}
