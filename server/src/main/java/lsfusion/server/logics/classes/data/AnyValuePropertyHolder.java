package lsfusion.server.logics.classes.data;

import lsfusion.base.Result;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.data.file.*;
import lsfusion.server.logics.classes.data.integral.DoubleClass;
import lsfusion.server.logics.classes.data.integral.IntegerClass;
import lsfusion.server.logics.classes.data.integral.LongClass;
import lsfusion.server.logics.classes.data.integral.NumericClass;
import lsfusion.server.logics.classes.data.link.*;
import lsfusion.server.logics.classes.data.time.*;
import lsfusion.server.logics.property.data.SessionDataProperty;

import java.sql.SQLException;

public class AnyValuePropertyHolder {
    private final LP objectProperty;
    private final LP stringProperty;
    private final LP bpStringProperty;
    private final LP textProperty;
    private final LP intProperty;
    private final LP longProperty;
    private final LP doubleProperty;
    private final LP numericProperty;
    private final LP yearProperty;
    private final LP dateTimeProperty;
    private final LP zDateTimeProperty;
    private final LP logicalProperty;
    private final LP dateProperty;
    private final LP timeProperty;
    private final LP colorProperty;
    private final LP wordFileProperty;
    private final LP imageFileProperty;
    private final LP pdfFileProperty;
    private final LP rawFileProperty;
    private final LP customFileProperty;
    private final LP excelFileProperty;
    private final LP textFileProperty;
    private final LP csvFileProperty;
    private final LP htmlFileProperty;
    private final LP jsonFileProperty;
    private final LP xmlFileProperty;
    private final LP tableFileProperty;
    private final LP wordLinkProperty;
    private final LP imageLinkProperty;
    private final LP pdfLinkProperty;
    private final LP rawLinkProperty;
    private final LP customLinkProperty;
    private final LP excelLinkProperty;
    private final LP textLinkProperty;
    private final LP csvLinkProperty;
    private final LP htmlLinkProperty;
    private final LP jsonLinkProperty;
    private final LP xmlLinkProperty;
    private final LP tableLinkProperty;

    public AnyValuePropertyHolder(LP<?> objectProperty, LP<?> stringProperty, LP<?> bpStringProperty, LP<?> textProperty, LP<?> intProperty, LP<?> longProperty, LP<?> doubleProperty, LP<?> numericProperty, LP<?> yearProperty,
                                  LP<?> dateTimeProperty, LP<?> zDateTimeProperty, LP<?> logicalProperty, LP<?> dateProperty, LP<?> timeProperty, LP<?> colorProperty, LP<?> wordFileProperty, LP<?> imageFileProperty,
                                  LP<?> pdfFileProperty, LP<?> rawFileProperty, LP<?> customFileProperty, LP<?> excelFileProperty,
                                  LP<?> textFileProperty, LP<?> csvFileProperty, LP<?> htmlFileProperty, LP<?> jsonFileProperty, LP<?> xmlFileProperty, LP<?> tableFileProperty,
                                  LP<?> wordLinkProperty, LP<?> imageLinkProperty, LP<?> pdfLinkProperty, LP<?> rawLinkProperty,
                                  LP<?> customLinkProperty, LP<?> excelLinkProperty, LP<?> textLinkProperty, LP<?> csvLinkProperty,
                                  LP<?> htmlLinkProperty, LP<?> jsonLinkProperty, LP<?> xmlLinkProperty, LP<?> tableLinkProperty) {
        assert objectProperty.property.getType() == ObjectType.instance
                && stringProperty.property.getType().getCompatible(StringClass.get(1))!=null
                && bpStringProperty.property.getType().getCompatible(StringClass.get(1))!=null
                && textProperty.property.getType().getCompatible(StringClass.get(1))!=null
                && intProperty.property.getType() == IntegerClass.instance
                && longProperty.property.getType() == LongClass.instance
                && doubleProperty.property.getType() == DoubleClass.instance
                && numericProperty.property.getType().getCompatible(NumericClass.get(0, 0)) != null
                && yearProperty.property.getType() == YearClass.instance
                && dateTimeProperty.property.getType() == DateTimeClass.instance
                && zDateTimeProperty.property.getType() == ZDateTimeClass.instance
                && logicalProperty.property.getType() == LogicalClass.instance
                && dateProperty.property.getType() == DateClass.instance
                && timeProperty.property.getType() == TimeClass.instance
                && colorProperty.property.getType() == ColorClass.instance
                && wordFileProperty.property.getType() == WordClass.get()
                && imageFileProperty.property.getType() == ImageClass.get()
                && pdfFileProperty.property.getType() == PDFClass.get()
                && rawFileProperty.property.getType() == CustomStaticFormatFileClass.get()
                && customFileProperty.property.getType() == DynamicFormatFileClass.get()
                && excelFileProperty.property.getType() == ExcelClass.get()
                && textFileProperty.property.getType() == TXTClass.get()
                && csvFileProperty.property.getType() == CSVClass.get()
                && htmlFileProperty.property.getType() == HTMLClass.get()
                && jsonFileProperty.property.getType() == JSONClass.get()
                && xmlFileProperty.property.getType() == XMLClass.get()
                && tableFileProperty.property.getType() == TableClass.get()
                && wordLinkProperty.property.getType() == WordLinkClass.get(false)
                && imageLinkProperty.property.getType() == ImageLinkClass.get(false)
                && pdfLinkProperty.property.getType() == PDFLinkClass.get(false)
                && rawLinkProperty.property.getType() == CustomStaticFormatLinkClass.get()
                && customLinkProperty.property.getType() == DynamicFormatLinkClass.get(false)
                && excelLinkProperty.property.getType() == ExcelLinkClass.get(false)
                && textLinkProperty.property.getType() == TXTLinkClass.get(false)
                && csvLinkProperty.property.getType() == CSVLinkClass.get(false)
                && htmlLinkProperty.property.getType() == HTMLLinkClass.get(false)
                && jsonLinkProperty.property.getType() == JSONLinkClass.get(false)
                && xmlLinkProperty.property.getType() == XMLLinkClass.get(false)
                && tableLinkProperty.property.getType() == TableLinkClass.get(false)
                ;

        this.objectProperty = objectProperty;
        this.stringProperty = stringProperty;
        this.bpStringProperty = bpStringProperty;
        this.textProperty = textProperty;
        this.intProperty = intProperty;
        this.longProperty = longProperty;
        this.doubleProperty = doubleProperty;
        this.numericProperty = numericProperty;
        this.yearProperty = yearProperty;
        this.dateTimeProperty = dateTimeProperty;
        this.zDateTimeProperty = zDateTimeProperty;
        this.logicalProperty = logicalProperty;
        this.dateProperty = dateProperty;
        this.timeProperty = timeProperty;
        this.colorProperty = colorProperty;
        this.wordFileProperty = wordFileProperty;
        this.imageFileProperty = imageFileProperty;
        this.pdfFileProperty = pdfFileProperty;
        this.rawFileProperty = rawFileProperty;
        this.customFileProperty = customFileProperty;
        this.excelFileProperty = excelFileProperty;
        this.textFileProperty = textFileProperty;
        this.csvFileProperty = csvFileProperty;
        this.htmlFileProperty = htmlFileProperty;
        this.jsonFileProperty = jsonFileProperty;
        this.xmlFileProperty = xmlFileProperty;
        this.tableFileProperty = tableFileProperty;
        this.wordLinkProperty = wordLinkProperty;
        this.imageLinkProperty = imageLinkProperty;
        this.pdfLinkProperty = pdfLinkProperty;
        this.rawLinkProperty = rawLinkProperty;
        this.customLinkProperty = customLinkProperty;
        this.excelLinkProperty = excelLinkProperty;
        this.textLinkProperty = textLinkProperty;
        this.csvLinkProperty = csvLinkProperty;
        this.htmlLinkProperty = htmlLinkProperty;
        this.jsonLinkProperty = jsonLinkProperty;
        this.xmlLinkProperty = xmlLinkProperty;
        this.tableLinkProperty = tableLinkProperty;
    }

    public LP<?> getLCP(Type valueType) {
        if (valueType instanceof ObjectType) {
            return objectProperty;
        } else if (valueType instanceof StringClass) {
            if (((StringClass) valueType).length.isUnlimited()) {
                return textProperty;
            }
            return ((StringClass) valueType).blankPadded ? bpStringProperty : stringProperty;
        } else if (valueType instanceof IntegerClass) {
            if (valueType instanceof YearClass) {
                return yearProperty;
            }
            return intProperty;
        } else if (valueType instanceof LongClass) {
            return longProperty;
        } else if (valueType instanceof DoubleClass) {
            return doubleProperty;
        } else if (valueType instanceof NumericClass) {
            return numericProperty;
        } else if (valueType instanceof DateTimeClass) {
            return dateTimeProperty;
        } else if (valueType instanceof ZDateTimeClass) {
            return zDateTimeProperty;
        } else if (valueType instanceof LogicalClass) {
            return logicalProperty;
        } else if (valueType instanceof DateClass) {
            return dateProperty;
        } else if (valueType instanceof TimeClass) {
            return timeProperty;
        } else if (valueType instanceof ColorClass) {
            return colorProperty;
        } else if (valueType instanceof WordClass) {
            return wordFileProperty;
        } else if (valueType instanceof ImageClass) {
            return imageFileProperty;
        } else if (valueType instanceof PDFClass) {
            return pdfFileProperty;
        } else if (valueType instanceof CustomStaticFormatFileClass) {
            return rawFileProperty;
        } else if (valueType instanceof DynamicFormatFileClass) {
            return customFileProperty;
        } else if (valueType instanceof ExcelClass) {
            return excelFileProperty;
        } else if (valueType instanceof TXTClass) {
            return textFileProperty;
        } else if (valueType instanceof CSVClass) {
            return csvFileProperty;
        } else if (valueType instanceof HTMLClass) {
            return htmlFileProperty;
        } else if (valueType instanceof JSONClass) {
            return jsonFileProperty;
        } else if (valueType instanceof XMLClass) {
            return xmlFileProperty;
        } else if (valueType instanceof TableClass) {
            return tableFileProperty;
        } else if (valueType instanceof StaticFormatFileClass) {
            return customFileProperty;
        } else if (valueType instanceof WordLinkClass) {
            return wordLinkProperty;
        } else if (valueType instanceof ImageLinkClass) {
            return imageLinkProperty;
        } else if (valueType instanceof PDFLinkClass) {
            return pdfLinkProperty;
        } else if (valueType instanceof CustomStaticFormatLinkClass) {
            return rawLinkProperty;
        } else if (valueType instanceof DynamicFormatLinkClass) {
            return customLinkProperty;
        } else if (valueType instanceof ExcelLinkClass) {
            return excelLinkProperty;
        } else if (valueType instanceof TXTLinkClass) {
            return textLinkProperty;
        } else if (valueType instanceof CSVLinkClass) {
            return csvLinkProperty;
        } else if (valueType instanceof HTMLLinkClass) {
            return htmlLinkProperty;
        } else if (valueType instanceof JSONLinkClass) {
            return jsonLinkProperty;
        } else if (valueType instanceof XMLLinkClass) {
            return xmlLinkProperty;
        } else if (valueType instanceof TableLinkClass) {
            return tableLinkProperty;
        } else if (valueType instanceof StaticFormatLinkClass) {
            return customLinkProperty;
        } else {
            throw new IllegalStateException(valueType + " is not supported by AnyValueProperty");
        }
    }
    
    @IdentityLazy
    public ImOrderSet<SessionDataProperty> getProps() {
        return SetFact.toOrderExclSet(
                // files
                customFileProperty, rawFileProperty, wordFileProperty, imageFileProperty, pdfFileProperty, excelFileProperty,
                textFileProperty, csvFileProperty, htmlFileProperty, jsonFileProperty, xmlFileProperty, tableFileProperty,
                // strings
                textProperty, stringProperty, bpStringProperty,
                // numbers
                numericProperty, longProperty, intProperty, doubleProperty,
                // date / times
                dateTimeProperty, zDateTimeProperty, dateProperty, timeProperty, yearProperty,
                // links
                customLinkProperty, rawLinkProperty, wordLinkProperty, imageLinkProperty, pdfLinkProperty,  excelLinkProperty,
                textLinkProperty, csvLinkProperty, htmlLinkProperty, jsonLinkProperty, xmlLinkProperty, tableLinkProperty,
                // others
                logicalProperty, colorProperty, objectProperty 
        ).mapOrderSetValues(value -> (SessionDataProperty) value.property);
    }
        
    public void write(Type valueType, ObjectValue value, ExecutionContext context, DataObject... keys) throws SQLException, SQLHandledException {
        getLCP(valueType).change(value, context, keys);
    }
    
    public void write(Type valueType, ObjectValue value, ExecutionEnvironment env, DataObject... keys) throws SQLException, SQLHandledException {
        getLCP(valueType).change(value, env, keys);
    }

    public ObjectValue read(Type valueType, ExecutionContext context, DataObject... keys) throws SQLException, SQLHandledException {
        return getLCP(valueType).readClasses(context, keys);
    }

    public ObjectValue read(Type valueType, ExecutionEnvironment env, DataObject... keys) throws SQLException, SQLHandledException {
        return getLCP(valueType).readClasses(env, keys);
    }

    private static ObjectValue getFirstChangeProp(ImOrderSet<SessionDataProperty> props, Action<?> action, Result<SessionDataProperty> readedProperty) {
        ImOrderSet<SessionDataProperty> changedProps = SetFact.filterOrderFn(props, action.getChangeExtProps().keys());
        if(changedProps.isEmpty())
            changedProps = props;
        
        readedProperty.set(changedProps.get(0));
        return NullValue.instance;
    }
    
    public ObjectValue readFirstNotNull(ExecutionEnvironment env, Result<SessionDataProperty> readedProperty, Action<?> action) throws SQLException, SQLHandledException { // return, not drop first value
        DataSession session = env.getSession();

        ImOrderSet<SessionDataProperty> props = getProps();
        ImSet<SessionDataProperty> changedProps = SetFact.fromJavaSet(session.getSessionChanges(props.getSet()));

        if(changedProps.isEmpty()) // optimization
            return getFirstChangeProp(props, action, readedProperty);
        if(changedProps.size() == 1) { // optimization
            SessionDataProperty prop = changedProps.single();
            readedProperty.set(prop);
            return prop.readClasses(env);
        }

        for(SessionDataProperty prop : props.filterOrder(changedProps)) {
            ObjectValue changedValue = prop.readClasses(env);
            if (changedValue instanceof DataObject) {
                readedProperty.set(prop);
                return changedValue;
            }
        }
        return getFirstChangeProp(props, action, readedProperty);
    }
}
