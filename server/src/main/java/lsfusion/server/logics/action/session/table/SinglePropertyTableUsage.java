package lsfusion.server.logics.action.session.table;

import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.query.build.Join;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.sql.SQLException;

public class SinglePropertyTableUsage<K> extends SessionTableUsage<K, String> {

    public SinglePropertyTableUsage(String debugInfo, ImOrderSet<K> keys, Type.Getter<K> keyType, final Type propertyType) {
        super(debugInfo, keys, SetFact.singletonOrder("value"), keyType, key -> propertyType);
    }

    public void updateAdded(SQLSession sql, BaseClass baseClass, Pair<Long, Long>[] shifts, OperationOwner owner) throws SQLException, SQLHandledException {
        updateAdded(sql, baseClass, "value", shifts, owner);
    }

    public void writeRows(ImMap<ImMap<K, DataObject>, ObjectValue> writeRows, SQLSession session, OperationOwner opOwner) throws SQLException, SQLHandledException {
        writeRows(session, writeRows.mapValues(value -> MapFact.singleton("value", value)), opOwner);
    }

    public static <P extends PropertyInterface> PropertyChange<P> getChange(SinglePropertyTableUsage<P> table) {
        ImRevMap<P, KeyExpr> mapKeys = table.getMapKeys();
        Join<String> join = table.join(mapKeys);
        return new PropertyChange<>(mapKeys, join.getExpr("value"), join.getWhere());
    }
}
