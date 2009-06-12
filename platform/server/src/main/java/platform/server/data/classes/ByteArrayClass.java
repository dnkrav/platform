package platform.server.data.classes;

import platform.server.data.sql.SQLSyntax;
import platform.server.logics.DataObject;
import platform.server.session.SQLSession;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.Format;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ByteArrayClass extends DataClass<byte[]> {

    public final static ByteArrayClass instance = new ByteArrayClass();

    public String toString() {
        return "Массив байт";
    }

    public DataClass getCompatible(DataClass compClass) {
        return compClass instanceof ByteArrayClass?this:null;
    }

    public Object getDefaultValue() {
        return new byte[0];
    }

    public DataObject getRandomObject(SQLSession session, Random randomizer) throws SQLException {
        throw new RuntimeException("not supported");
    }

    public List<DataObject> getRandomList(Map<CustomClass, List<DataObject>> objects) {
        throw new RuntimeException("not supported");
    }

    public Class getJavaClass() {
        return new byte[0].getClass();
    }

    public Format getDefaultFormat() {
        throw new RuntimeException("not supported");
    }

    public byte getTypeID() {
        return 8;
    }

    public String getDB(SQLSyntax syntax) {
        return syntax.getByteArrayType();
    }

    public String getEmptyString() {
        throw new RuntimeException("not supported");
    }

    public boolean isSafeString(Object value) {
        return false;
    }

    public String getString(Object value, SQLSyntax syntax) {
        throw new RuntimeException("not supported");
    }

    public byte[] read(Object value) {
        return (byte[])value;
    }

    public void writeParam(PreparedStatement statement, int num, Object value) throws SQLException {
        statement.setBytes(num, (byte[]) value);
    }
}
