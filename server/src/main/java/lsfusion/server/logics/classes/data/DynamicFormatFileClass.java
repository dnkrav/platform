package lsfusion.server.logics.classes.data;

import lsfusion.base.file.FileData;
import lsfusion.interop.form.property.DataType;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.ParseException;
import lsfusion.server.data.type.Type;
import org.apache.commons.net.util.Base64;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

public class DynamicFormatFileClass extends FileClass<FileData> {

    protected String getFileSID() {
        return "FILE"; // для обратной совместимости такое название
    }

    public FileData getDefaultValue() {
        return FileData.EMPTY;
    }

    public Class getReportJavaClass() {
        return FileData.class;
    }

    private static Collection<DynamicFormatFileClass> instances = new ArrayList<>();

    public static DynamicFormatFileClass get() {
        return get(false, false);
    }
    public static DynamicFormatFileClass get(boolean multiple, boolean storeName) {
        for (DynamicFormatFileClass instance : instances)
            if (instance.multiple == multiple && instance.storeName == storeName)
                return instance;

        DynamicFormatFileClass instance = new DynamicFormatFileClass(multiple, storeName);
        instances.add(instance);
        storeClass(instance);
        return instance;
    }

    private DynamicFormatFileClass(boolean multiple, boolean storeName) {
        super(multiple, storeName);
    }

    public DataClass getCompatible(DataClass compClass, boolean or) {
        return compClass instanceof DynamicFormatFileClass ? this : null;
    }

    public byte getTypeID() {
        return DataType.DYNAMICFORMATFILE;
    }

    @Override
    public String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv, Type typeFrom) {
        String extension = null;
        if (typeFrom instanceof StaticFormatFileClass) {
            extension = ((StaticFormatFileClass) typeFrom).getExtension();
        }
        if (extension != null) {
            return "cast_to_custom_file(" + value + ", CAST('" + extension + "' AS VARCHAR))";
        }
        return super.getCast(value, syntax, typeEnv, typeFrom);
    }

    @Override
    protected FileData parseHTTPNotNull(FileData b) {
        return b;
    }

    @Override
    protected FileData formatHTTPNotNull(FileData b) {
        return b;
    }

    public FileData read(Object value) {
        if(value instanceof byte[])
            return new FileData((byte[]) value);
        return (FileData) value;
    }

    public FileData read(ResultSet set, SQLSyntax syntax, String name) throws SQLException {
        byte[] result = set.getBytes(name);
        if(result != null)
            return new FileData(result);
        return null;
    }

    public void writeParam(PreparedStatement statement, int num, Object value, SQLSyntax syntax) throws SQLException {
        statement.setBytes(num, value != null ? ((FileData) value).getBytes() : null);
    }

    @Override
    public int getSize(FileData value) {
        return value.getLength();
    }

    public FileData parseString(String s) throws ParseException {
        return new FileData(Base64.decodeBase64(s));
    }

    public String formatString(FileData value) {
        return value != null ? Base64.encodeBase64String(value.getBytes()) : null;
    }
}