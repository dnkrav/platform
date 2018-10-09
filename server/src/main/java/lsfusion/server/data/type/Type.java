package lsfusion.server.data.type;

import com.hexiong.jdbf.JDBFException;
import lsfusion.base.ExtInt;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.classes.BaseClass;
import lsfusion.server.classes.ConcreteClass;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.form.view.report.ReportDrawField;
import lsfusion.server.logics.property.actions.integration.exporting.plain.dbf.OverJDBField;
import net.iryndin.jdbf.core.DbfRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface Type<T> extends ClassReader<T>, FunctionType {

    boolean useIndexedJoin();

    interface Getter<K> {
        Type getType(K key);
    }

    Object getInfiniteValue(boolean min);

    String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv); // как правило нужен, чтобы указать СУБД класс, а не реально прокастить 
    String getCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv, Type typeFrom);
    String getSafeCast(String value, SQLSyntax syntax, TypeEnvironment typeEnv, Type typeFrom);

    String getDB(SQLSyntax syntax, TypeEnvironment typeEnv);
    String getDotNetType(SQLSyntax syntax, TypeEnvironment typeEnv); // for ms sql
    String getDotNetRead(String reader); // for ms sql
    String getDotNetWrite(String writer, String value); // for ms sql
    int getDotNetSize(); // for ms sql
    int getSQL(SQLSyntax syntax);

    boolean isSafeString(Object value);
    boolean isSafeType();
    String getString(Object value, SQLSyntax syntax);

    void writeParam(PreparedStatement statement, SQLSession.ParamNum num, Object value, SQLSyntax syntax) throws SQLException;
    void writeNullParam(PreparedStatement statement, SQLSession.ParamNum num, SQLSyntax syntax) throws SQLException;

    int getReportMinimumWidth();
    int getReportPreferredWidth();

    void fillReportDrawField(ReportDrawField reportField);
    
    boolean isFlex();

    Type getCompatible(Type type);

    ConcreteClass getDataClass(Object value, SQLSession session, AndClassSet classSet, BaseClass baseClass, OperationOwner owner) throws SQLException, SQLHandledException;

    ImList<AndClassSet> getUniversal(BaseClass baseClass);

    Stat getTypeStat(boolean forJoin);

    ExtInt getCharLength();

    T parseDBF(DbfRecord dbfRecord, String fieldName, String charset) throws ParseException, java.text.ParseException;
    T parseJSON(JSONObject object, String key) throws ParseException, JSONException;
    T parseCSV(String value) throws ParseException;
    T parseXML(String value) throws ParseException;
    T parseXLS(Cell cell, CellValue formulaValue) throws ParseException;

    OverJDBField formatDBF(String fieldName) throws JDBFException;
    Object formatJSON(T object);
    String formatCSV(T object);
    String formatXML(T object);

    T parseNullableString(String s, boolean emptyIsNull) throws ParseException; // s - not null (файлы decode'ся base64)
    
    T parseString(String s) throws ParseException; // s - not null (файлы decode'ся base64)

    String formatString(T value); // возвращает null если передали null (файлы encode'ся base64)

    T parseHTTP(Object o, Charset charset) throws ParseException; // o - String или byte[], o - not null, null'ы decode'ся в зависимости от типа

    Object formatHTTP(T value, Charset charset); // возвращает String или byte[] (не null), null'ы encode'ит в зависимости от типа

    AndClassSet getBaseClassSet(BaseClass baseClass);

    String getSID();
}
