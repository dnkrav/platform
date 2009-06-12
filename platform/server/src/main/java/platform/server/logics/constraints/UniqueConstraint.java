package platform.server.logics.constraints;

import platform.interop.Compare;
import platform.server.data.query.JoinQuery;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.wheres.CompareWhere;
import platform.server.logics.properties.DataProperty;
import platform.server.logics.properties.DefaultData;
import platform.server.logics.properties.Property;
import platform.server.logics.properties.PropertyInterface;
import platform.server.session.DataSession;
import platform.server.where.Where;
import platform.server.where.WhereBuilder;
import platform.base.BaseUtils;

import java.sql.SQLException;
import java.util.*;

// >= 0
class UniqueConstraint extends Constraint {

    public <P extends PropertyInterface> String check(DataSession session, Property<P> property, Map<DataProperty, DefaultData> defaultProps, Collection<Property> noUpdateProps) throws SQLException {

        // надо проверить для каждого что старых нету
        // изменения JOIN'им (ст. запрос FULL JOIN новый) ON изм. зн = новому зн. WHERE код изм. = код нов. и ключи не равны и зн. не null

        // ключи на самом деле состоят из 2-х частей - первые измененные (Property), 2-е - старые (из UpdateUnionQuery)
        // соответственно надо создать объекты
        Map<P,Object> mapPrevKeys = new HashMap<P, Object>();
        Map<Object, KeyExpr> mapPrevExprs = new HashMap<Object, KeyExpr>();
        for(P propertyInterface : property.interfaces) {
            Object prevKey = new Object();
            mapPrevKeys.put(propertyInterface, prevKey);
            mapPrevExprs.put(prevKey,new KeyExpr("prev"+propertyInterface));
        }

        Map<P,KeyExpr> mapPropExprs = property.getMapKeys();
        JoinQuery<Object,String> changed = new JoinQuery<Object,String>(BaseUtils.merge(mapPropExprs,mapPrevExprs));

        WhereBuilder changedWhere = new WhereBuilder();
        SourceExpr changedExpr = property.getSourceExpr(mapPropExprs,session.changes,defaultProps, noUpdateProps, changedWhere);

        // равны значения
        changed.and(changedExpr.compare(property.getSourceExpr(BaseUtils.join(mapPrevKeys, mapPrevExprs)),Compare.EQUALS));
        changed.and(changedWhere.toWhere());
        // значения не NULL
        changed.and(changedExpr.getWhere());

        // не равны ключи
        Where orDiffKeys = Where.FALSE;
        for(P propertyInterface : property.interfaces)
            orDiffKeys = orDiffKeys.or(new CompareWhere(changed.mapKeys.get(propertyInterface),changed.mapKeys.get(mapPrevKeys.get(propertyInterface)), Compare.NOT_EQUALS));
        changed.and(orDiffKeys);
        changed.properties.put("value", changedExpr);

        LinkedHashMap<Map<Object, Object>, Map<String, Object>> result = changed.executeSelect(session);
        if(result.size()>0) {
            String resultString = "Уникальное ограничение на св-во "+ property.caption +" нарушено"+'\n';
            for(Map.Entry<Map<Object,Object>,Map<String,Object>> row : result.entrySet()) {
                resultString += "   Объекты (1,2) : ";
                for(P propertyInterface : property.interfaces)
                    resultString += row.getKey().get(propertyInterface)+","+row.getKey().get((mapPrevKeys.get(propertyInterface)))+" ";
                resultString += "Значение : "+row.getValue().get("value")+'\n';
            }

            return resultString;
        } else
            return null;
    }
}
