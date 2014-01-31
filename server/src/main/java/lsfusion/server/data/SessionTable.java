package lsfusion.server.data;

import lsfusion.base.*;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.add.MAddSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.caches.AbstractValuesContext;
import lsfusion.server.caches.ManualLazy;
import lsfusion.server.caches.ValuesContext;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.caches.hash.HashValues;
import lsfusion.server.classes.BaseClass;
import lsfusion.server.classes.ConcreteClass;
import lsfusion.server.classes.ConcreteObjectClass;
import lsfusion.server.classes.DataClass;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.ValueExpr;
import lsfusion.server.data.expr.formula.FormulaExpr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.expr.query.PropStat;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.query.IQuery;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.data.query.stat.StatKeys;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.MapValuesTranslate;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.ParseInterface;
import lsfusion.server.data.type.StringParseInterface;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.DBManager;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;

import static lsfusion.base.BaseUtils.hashEquals;

public class SessionTable extends Table implements ValuesContext<SessionTable>, Value {// в явную хранимые ряды

    public final int count; // вообще должен быть точным, или как минимум пессимистичным, чтобы в addObjects учитываться

    public StatKeys<KeyField> getStatKeys() {
        return getStatKeys(this, count);
    }

    public Value removeBig(MAddSet<Value> usedValues) {
        return null;
    }

    public ImMap<PropertyField,PropStat> getStatProps() {
        return getStatProps(this, count);
    }

    // предполагается вызов через SQLSession
    public SessionTable(String name, ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, ClassWhere<KeyField> classes, ImMap<PropertyField, ClassWhere<Field>> propertyClasses, int count) {
        super(name, keys, properties, classes, propertyClasses);
        this.count = count;
    }

    // создает таблицу batch'ем
    public static SessionTable create(final SQLSession session, final ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, final ImMap<ImMap<KeyField, DataObject>, ImMap<PropertyField, ObjectValue>> rows, Object owner) throws SQLException, SQLHandledException {
        // прочитаем классы
        return session.createTemporaryTable(keys, properties, rows.size(), new FillTemporaryTable() {
            public Integer fill(String name) throws SQLException {
                session.insertBatchRecords(name, keys, rows);
                return null;
            }
        }, SessionRows.getClasses(properties, rows), owner);
    }

    public SessionTable(String name, ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, Integer count, ClassWhere<KeyField> classes, ImMap<PropertyField, ClassWhere<Field>> propertyClasses) {
        super(name, keys, properties, classes, propertyClasses);

        this.count = count;
    }
    public SessionTable(String name, ImOrderSet<KeyField> keys, ImSet<PropertyField> properties, Integer count, Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> tableClasses) {
        this(name, keys, properties, count, tableClasses.first, tableClasses.second);
    }

    public ImSet<PropertyField> getProperties() {
        return properties;
    }

    @Override
    public String getName(SQLSyntax syntax) {
        return syntax.getSessionTableName(name);
    }

    @Override
    public String getQueryName(CompileSource source) {
        assert source.params.containsKey(this);
        return source.params.get(this);
    }

    protected Table translate(MapTranslate translator) {
        return translateValues(translator.mapValues());
    }

    protected int hash(HashContext hashContext) {
        return hashValues(hashContext.values);
    }

    public ImSet<Value> getValues() {
        return getContextValues();
    }

    public SessionTable translateValues(MapValuesTranslate mapValues) {
        return mapValues.translate(this);
    }

    public SessionTable translateRemoveValues(MapValuesTranslate translate) {
        return translateValues(translate);
    }

    public int hashValues(HashValues hashValues) {
        return hashValues.hash(this);
    }

    public ImSet<Value> getContextValues() {
        return SetFact.<Value>singleton(this);
    }

    public ParseInterface getParseInterface() {
        return new StringParseInterface() {
            public String getString(SQLSyntax syntax) {
                return getName(syntax);
            }

            @Override
            public void checkSessionTable(SQLSession sql) {
                sql.checkSessionTable(SessionTable.this);
            }
        };
    }

    // теоретически достаточно только
    private static class Struct extends TwinImmutableObject implements GlobalObject {

        public final ImOrderSet<KeyField> keys; // List потому как в таком порядке индексы будут строиться
        public final ImCol<PropertyField> properties;
        protected final ClassWhere<KeyField> classes; // по сути условия на null'ы в том числе
        protected final ImMap<PropertyField, ClassWhere<Field>> propertyClasses;

        protected final StatKeys<KeyField> statKeys;
        protected final ImMap<PropertyField, PropStat> statProps;

        private Struct(ImOrderSet<KeyField> keys, ImCol<PropertyField> properties, ClassWhere<KeyField> classes, ImMap<PropertyField, ClassWhere<Field>> propertyClasses, StatKeys<KeyField> statKeys, ImMap<PropertyField, PropStat> statProps) {
            this.keys = keys;
            this.properties = properties;
            this.classes = classes;
            this.propertyClasses = propertyClasses;

            this.statKeys = statKeys;
            this.statProps = statProps;
        }

        public boolean twins(TwinImmutableObject o) {
            return classes.equals(((Struct) o).classes) && keys.equals(((Struct) o).keys) && properties.equals(((Struct) o).properties) && propertyClasses.equals(((Struct) o).propertyClasses) && statKeys.equals(((Struct)o).statKeys) && statProps.equals(((Struct)o).statProps);
        }

        public int immutableHashCode() {
            return 31 * (31 * (31 * (31 * (31 * keys.hashCode() + properties.hashCode()) + classes.hashCode()) + propertyClasses.hashCode()) + statKeys.hashCode()) + statProps.hashCode();
        }
    }

    private Struct struct = null;

    @ManualLazy
    public Struct getValueClass() {
        if (struct == null) {
            struct = ThreadLocalContext.getLogicsInstance().twinObject(new Struct(keys, properties, classes, propertyClasses, getStatKeys(), getStatProps()));
        }
        return struct;
    }

    public boolean twins(TwinImmutableObject o) {
        return name.equals(((SessionTable) o).name) && getValueClass().equals(((SessionTable) o).getValueClass());
    }

    public int immutableHashCode() {
        return name.hashCode() * 31 + getValueClass().hashCode();
    }

    public static Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> orFieldsClassWheres(ClassWhere<KeyField> classes, ImMap<PropertyField, ClassWhere<Field>> propertyClasses, Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> orClasses) {
        ImMap<PropertyField, ClassWhere<Field>> orPropertyClasses = propertyClasses.merge(orClasses.second, ClassWhere.<PropertyField, Field>addOr());
        return new Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>>(classes.or(orClasses.first), orPropertyClasses);
    }

    public static Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> orFieldsClassWheres(ClassWhere<KeyField> classes, final ImMap<PropertyField, ClassWhere<Field>> propertyClasses, ImMap<KeyField, DataObject> keyFields, final ImMap<PropertyField, ObjectValue> propFields) {
        return orFieldsClassWheres(DataObject.getMapDataClasses(keyFields), ObjectValue.getMapClasses(propFields), classes, propertyClasses);
    }

    public static Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> orFieldsClassWheres(final ImMap<KeyField, ConcreteClass> keyFields, final ImMap<PropertyField, ConcreteClass> propFields, ClassWhere<KeyField> classes, final ImMap<PropertyField, ClassWhere<Field>> propertyClasses) {

        assert propertyClasses.keys().containsAll(propFields.keys());
        ImMap<PropertyField, ClassWhere<Field>> orPropertyClasses = propertyClasses.mapValues(new GetKeyValue<ClassWhere<Field>, PropertyField, ClassWhere<Field>>() {
            public ClassWhere<Field> getMapValue(PropertyField propField, ClassWhere<Field> existedPropertyClasses) {
                ConcreteClass propClass = propFields.get(propField);
                if (propClass != null)
                    existedPropertyClasses = existedPropertyClasses.or(new ClassWhere<Field>(
                            MapFact.addExcl(keyFields, propField, propClass)));
                return existedPropertyClasses;
            }});
        return new Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>>(
                classes.or(new ClassWhere<KeyField>(keyFields)), orPropertyClasses);
    }

    public static Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> andFieldsClassWheres(ClassWhere<KeyField> classes, ImMap<PropertyField, ClassWhere<Field>> propertyClasses, ImMap<KeyField, DataObject> keyFields, ImMap<PropertyField, ObjectValue> propFields) {
        // определяем новые классы чтобы создать таблицу
        final ClassWhere<KeyField> addKeyClasses = new ClassWhere<KeyField>(DataObject.getMapDataClasses(keyFields));

        final ClassWhere<KeyField> andKeyClasses = classes.and(addKeyClasses);

        ImMap<PropertyField, ClassWhere<Field>> andPropertyClasses = propertyClasses.mapValues(new GetValue<ClassWhere<Field>, ClassWhere<Field>>() {
            public ClassWhere<Field> getMapValue(ClassWhere<Field> value) {
                return value.and(BaseUtils.<ClassWhere<Field>>immutableCast(addKeyClasses));
            }}).addExcl(
                propFields.mapValues(new GetKeyValue<ClassWhere<Field>, PropertyField, ObjectValue>() {
                    public ClassWhere<Field> getMapValue(PropertyField key, ObjectValue value) {
                        return !(value instanceof DataObject)?ClassWhere.<Field>FALSE():
                                new ClassWhere<Field>(MapFact.<Field, ConcreteClass>singleton(key, ((DataObject) value).objectClass)).
                                        and(BaseUtils.<ClassWhere<Field>>immutableCast(andKeyClasses));
                    }}));
        return new Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>>(andKeyClasses, andPropertyClasses);
    }

    public static Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> removeFieldsClassWheres(ClassWhere<KeyField> classes, ImMap<PropertyField, ClassWhere<Field>> propertyClasses, final ImSet<KeyField> keyFields, ImSet<PropertyField> propFields) {
        if(keyFields.isEmpty())
            return new Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>>(classes, propertyClasses.remove(propFields));
        else {
            ImMap<PropertyField, ClassWhere<Field>> removePropClasses = propertyClasses.remove(propFields).mapValues(new GetValue<ClassWhere<Field>, ClassWhere<Field>>() {
                public ClassWhere<Field> getMapValue(ClassWhere<Field> value) {
                    return value.remove(keyFields);
                }});
            return new Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>>(classes.remove(keyFields), removePropClasses);
        }
    }

    public static Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>> getFieldsClassWheres(ImMap<ImMap<KeyField, DataObject>, ImMap<PropertyField, ObjectValue>> data) {
        ClassWhere<KeyField> keysClassWhere = ClassWhere.<KeyField>FALSE();
        ImMap<PropertyField, ClassWhere<Field>> propertiesClassWheres = null;
        for (int i=0,size=data.size();i<size;) {
            final ImMap<KeyField, ConcreteClass> rowKeyClasses = DataObject.getMapDataClasses(data.getKey(i));

            keysClassWhere = keysClassWhere.or(new ClassWhere(rowKeyClasses));

            ImMap<PropertyField, ClassWhere<Field>> rowClasses = data.getValue(i).mapValues(new GetKeyValue<ClassWhere<Field>, PropertyField, ObjectValue>() {
                public ClassWhere<Field> getMapValue(PropertyField key, ObjectValue value) {
                    return new ClassWhere<Field>(MapFact.addExcl(rowKeyClasses, key, ((DataObject) value).objectClass));
                }
            });

            if(propertiesClassWheres==null)
                propertiesClassWheres = rowClasses;
            else
                propertiesClassWheres = propertiesClassWheres.mapAddValues(rowClasses, ClassWhere.<PropertyField, Field>addOr());
        }
        return new Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>>(keysClassWhere, propertiesClassWheres);
    }

    public SessionTable modifyRecord(final SQLSession session, ImMap<KeyField, DataObject> keyFields, ImMap<PropertyField, ObjectValue> propFields, Modify type, final Object owner, Result<Boolean> changed) throws SQLException, SQLHandledException {

        if(type==Modify.DELETE) { // статистику пока не учитываем
            int proceeded = deleteRecords(session, keyFields);
            if(proceeded == 0)
                return this;
            changed.set(true);
            return new SessionTable(name, keys, properties, count - proceeded, classes, propertyClasses).
                    updateStatistics(session, count, owner).checkClasses(session, null);
        }
        if (type == Modify.LEFT && session.isRecord(this, keyFields))
            return this;

        boolean update = (type==Modify.UPDATE || type==Modify.MODIFY);

        if(update) {
            if(session.updateRecordsCount(this, keyFields, propFields)==0) { // запись не найдена
                if(type==Modify.UPDATE)
                    return this;
                else
                    update = false;
            }
        }
        if(!update)
            session.insertRecord(this, keyFields, propFields);

        changed.set(true);
        return new SessionTable(name, keys, properties, count + (update?0:1),
                        orFieldsClassWheres(classes, propertyClasses, keyFields, propFields)).
                            updateStatistics(session, count, owner).checkClasses(session, null);
    }

    public SessionTable modifyRows(SQLSession session, IQuery<KeyField, PropertyField> query, Modify type, QueryEnvironment env, Object owner, Result<Boolean> changed) throws SQLException, SQLHandledException {

        if(query.isEmpty()) // оптимизация
            return this;

        ModifyQuery modify = new ModifyQuery(this, query, env);
        int inserted, proceeded;
        switch (type) {
            case MODIFY:
                Result<Integer> modifyProceeded = new Result<Integer>();
                inserted = session.modifyRecords(modify, modifyProceeded);
                proceeded = modifyProceeded.result;
                break;
            case LEFT:
                proceeded = session.insertLeftSelect(modify, true, false);
                inserted = proceeded;
                break;
            case ADD:
                proceeded = session.insertSelect(modify);
                inserted = proceeded;
                break;
            case UPDATE:
                proceeded = session.updateRecords(modify);
                inserted = 0;
                break;
            case DELETE:
                proceeded = session.deleteRecords(modify);
                if(proceeded==0)
                    return this;

                return new SessionTable(name, keys, properties, count - proceeded, classes, propertyClasses).
                        updateStatistics(session, count, owner).checkClasses(session, null);
            default:
                throw new RuntimeException("should not be");
        }
        if(proceeded==0)
            return this;

        changed.set(true);
        return new SessionTable(name, keys, properties, count + inserted,
                        orFieldsClassWheres(classes, propertyClasses, SessionData.getQueryClasses(query))).
                            updateStatistics(session, count, owner).checkClasses(session, null);
    }
    public void updateAdded(SQLSession session, BaseClass baseClass, PropertyField field, Pair<Integer, Integer>[] shifts) throws SQLException, SQLHandledException {
        QueryBuilder<KeyField, PropertyField> query = new QueryBuilder<KeyField, PropertyField>(this);
        lsfusion.server.data.query.Join<PropertyField> join = join(query.getMapExprs());

        String formula = ""; String aggsh = "";
        MExclMap<String, Expr> mParams = MapFact.mExclMap(1 + 2 * shifts.length);
        mParams.exclAdd("prm1", join.getExpr(field));
        for(int i=0;i<shifts.length;i++) {
            String idsh = "prm" + (2*i+2);
            String countsh = "prm" + (2*i+3);

            if(i==0) {
                formula = idsh;
                aggsh = countsh;
            } else {
                formula = "WHEN prm1 > (" + aggsh + ") THEN " + idsh + " - (" + aggsh + ") " + (i==1?"ELSE ":"") + formula;
                aggsh += "+" + countsh;
            }
            mParams.exclAdd(idsh, new ValueExpr(shifts[i].first, ObjectType.idClass));
            if(i!=shifts.length-1) // последний параметр не нужен
                mParams.exclAdd(countsh, new ValueExpr(shifts[i].second, ObjectType.idClass));
        }
        if(shifts.length > 1)
            formula = "CASE " + formula + " END";

        query.addProperty(field, FormulaExpr.createCustomFormula("prm1+" + formula, baseClass.unknown, mParams.immutable()));
        query.and(join.getWhere());
        session.updateRecords(new ModifyQuery(this, query.getQuery()));
    }

    public SessionTable updateCurrentClasses(DataSession session) throws SQLException, SQLHandledException {
        final ImRevMap<KeyField, KeyExpr> mapKeys = getMapKeys();
        lsfusion.server.data.query.Join<PropertyField> join = join(mapKeys);

        MExclMap<Field, Expr> mMapExprs = MapFact.mExclMapMax(keys.size()+properties.size());
        MExclMap<Field, DataClass> mMapData = MapFact.mExclMapMax(keys.size()+properties.size());
        ClassWhere<KeyField> updatedClasses = ClassWhere.FALSE();
        for(KeyField key : keys)
            if(key.type instanceof ObjectType)
                mMapExprs.exclAdd(key, mapKeys.get(key));
            else
                mMapData.exclAdd(key, (DataClass) key.type);
        for(PropertyField property : properties)
            if(property.type instanceof ObjectType)
                mMapExprs.exclAdd(property, join.getExpr(property));
            else
                mMapData.exclAdd(property, (DataClass) property.type);
        ImMap<Field, Expr> mapExprs = mMapExprs.immutable();
        ImMap<Field, DataClass> mapData = mMapData.immutable();
        ImMap<PropertyField, ClassWhere<Field>> updatedPropertyClasses = properties.toMap(ClassWhere.<Field>FALSE());

        // пока исходим из assertion'а что не null, потом надо будет разные делать
        for(ImMap<Field, ConcreteObjectClass> diffClasses : session.readDiffClasses(join.getWhere(), MapFact.<Field, Expr>EMPTY(), mapExprs)) {
            final ImMap<Field, ConcreteClass> result = MapFact.addExcl(diffClasses, mapData);
            updatedClasses = updatedClasses.or(new ClassWhere<KeyField>(result.filterIncl(getTableKeys())));
            
            updatedPropertyClasses = updatedPropertyClasses.mapValues(new GetKeyValue<ClassWhere<Field>, PropertyField, ClassWhere<Field>>() {
                public ClassWhere<Field> getMapValue(PropertyField key, ClassWhere<Field> value) {
                    return value.or(new ClassWhere<Field>(result.filterIncl(SetFact.addExcl(getTableKeys(), key))));
                }});
        }
        return new SessionTable(name, keys, properties, count, updatedClasses, updatedPropertyClasses).checkClasses(session.sql, null);
    }

    public SessionTable updateStatistics(final SQLSession session, int prevCount, final Object owner) throws SQLException, SQLHandledException {
        if(!SQLTemporaryPool.getDBStatistics(count).equals(SQLTemporaryPool.getDBStatistics(prevCount))) {
            return session.createTemporaryTable(keys, properties, count, new FillTemporaryTable() {
                public Integer fill(String name) throws SQLException, SQLHandledException {
                    QueryBuilder<KeyField, PropertyField> moveData = new QueryBuilder<KeyField, PropertyField>(SessionTable.this);
                    lsfusion.server.data.query.Join<PropertyField> prevJoin = join(moveData.getMapExprs());
                    moveData.and(prevJoin.getWhere());
                    moveData.addProperties(prevJoin.getExprs());
                    session.insertSessionSelect(name, moveData.getQuery(), QueryEnvironment.empty);
                    session.returnTemporaryTable(SessionTable.this, owner);
                    return null;
                }
            },new Pair<ClassWhere<KeyField>, ImMap<PropertyField, ClassWhere<Field>>>(classes, propertyClasses), owner);
        }
        return this;
    }

    public int deleteRecords(SQLSession session, ImMap<KeyField, DataObject> keys) throws SQLException {
        return session.deleteKeyRecords(this, DataObject.getMapDataValues(keys));
    }


    public SessionTable addFields(final SQLSession session, final ImOrderSet<KeyField> keys, final ImMap<KeyField, DataObject> addKeys, final ImMap<PropertyField, ObjectValue> addProps, final Object owner) throws SQLException, SQLHandledException {
        if(addKeys.isEmpty() && addProps.isEmpty())
            return this;

        return session.createTemporaryTable(keys, properties.addExcl(addProps.keys()), count, new FillTemporaryTable() {
            public Integer fill(String name) throws SQLException, SQLHandledException {
                // записать в эту таблицу insertSessionSelect из текущей + default поля
                ImSet<KeyField> tableKeys = getTableKeys();
                QueryBuilder<KeyField, PropertyField> moveData = new QueryBuilder<KeyField, PropertyField>(tableKeys.addExcl(addKeys.keys()), addKeys);
                lsfusion.server.data.query.Join<PropertyField> prevJoin = join(moveData.getMapExprs().filterIncl(tableKeys));
                moveData.and(prevJoin.getWhere());
                moveData.addProperties(prevJoin.getExprs());
                moveData.addProperties(DataObject.getMapExprs(addProps));
                session.insertSessionSelect(name, moveData.getQuery(), QueryEnvironment.empty);
                session.returnTemporaryTable(SessionTable.this, owner);
                return null;
            }
        }, andFieldsClassWheres(classes, propertyClasses, addKeys, addProps), owner);
    }

    public SessionTable removeFields(final SQLSession session, ImSet<KeyField> removeKeys, ImSet<PropertyField> removeProps, final Object owner) throws SQLException, SQLHandledException {
        if(removeKeys.isEmpty() && removeProps.isEmpty())
            return this;

        // assert что удаляемые ключи с одинаковыми значениями, но вообще может использоваться как слияние
        final ImOrderSet<KeyField> remainOrderKeys = keys.removeOrder(removeKeys);
        final ImSet<KeyField> remainKeys = remainOrderKeys.getSet();
        final ImSet<PropertyField> remainProps = properties.remove(removeProps);
        return session.createTemporaryTable(remainOrderKeys, remainProps, count, new FillTemporaryTable() {
            public Integer fill(String name) throws SQLException, SQLHandledException {
                // записать в эту таблицу insertSessionSelect из текущей + default поля
                QueryBuilder<KeyField, PropertyField> moveData = new QueryBuilder<KeyField, PropertyField>(remainKeys);

                if (remainKeys.size() == keys.size()) { // для оптимизации
                    lsfusion.server.data.query.Join<PropertyField> prevJoin = join(moveData.getMapExprs());
                    moveData.and(prevJoin.getWhere());
                    moveData.addProperties(prevJoin.getExprs().filterIncl(remainProps));
                } else {
                    ImRevMap<KeyField, KeyExpr> tableKeys = getMapKeys();
                    lsfusion.server.data.query.Join<PropertyField> prevJoin = join(tableKeys);
                    ImRevMap<KeyField, KeyExpr> groupKeys = tableKeys.filterInclRev(remainKeys);
                    moveData.and(GroupExpr.create(groupKeys, prevJoin.getWhere(), moveData.getMapExprs()).getWhere());
                    for (PropertyField prop : remainProps)
                        moveData.addProperty(prop, GroupExpr.create(groupKeys, prevJoin.getExpr(prop), GroupType.ANY, moveData.getMapExprs()));
                }
                session.insertSessionSelect(name, moveData.getQuery(), QueryEnvironment.empty);
                session.returnTemporaryTable(SessionTable.this, owner);
                return null;
            }
        }, removeFieldsClassWheres(classes, propertyClasses, removeKeys, removeProps), owner);
    }

    private BaseUtils.HashComponents<Value> components = null;
    @ManualLazy
    public BaseUtils.HashComponents<Value> getValueComponents() {
        if (components == null)
            components = AbstractValuesContext.getComponents(this);
        return components;
    }

    public void drop(SQLSession session, Object owner) throws SQLException {
        session.returnTemporaryTable(this, owner);
    }
    public void rollDrop(SQLSession session, Object owner) throws SQLException {
        session.rollReturnTemporaryTable(this, owner);
    }

    // см. usage
    public SessionTable fixKeyClasses(ClassWhere<KeyField> fixClasses) {
        assert propertyClasses.isEmpty();
        ClassWhere<KeyField> fixedClasses = classes.and(fixClasses);
        if(hashEquals(fixedClasses, classes))
            return this;
        else
            return new SessionTable(name, keys, properties, count, fixedClasses, propertyClasses);
    }

    // для проверки общей целостности есть специальные административные процедуры
    private boolean assertCheckClasses(SQLSession session, BaseClass baseClass) throws SQLException, SQLHandledException {
        if(1==1 || session.inconsistent)
            return true;

        if(baseClass==null)
            baseClass = ThreadLocalContext.getBusinessLogics().LM.baseClass;

        final Pair<ClassWhere<KeyField>,ImMap<PropertyField,ClassWhere<Field>>> readClasses;
        ImMap<ImMap<KeyField, ConcreteClass>, ImMap<PropertyField, ConcreteClass>> readData = readClasses(session, baseClass); // теоретически может очень долго работать, когда много колонок, из-за большого количества case'ов по которым надо группировать
        if(readData!=null)
            readClasses = SessionRows.getClasses(readData,  properties);
        else
            readClasses = SessionRows.getClasses(properties, read(session, baseClass).getMap());

        // похоже все же не имеет смысла пока
/*        if(!classes.means(readClasses.first, true))
            classes = readClasses.first;
        propertyClasses = propertyClasses.mapValues(new GetKeyValue<ClassWhere<Field>, PropertyField, ClassWhere<Field>>() {
            public ClassWhere<Field> getMapValue(PropertyField key, ClassWhere<Field> value) {
                ClassWhere<Field> readWhere = readClasses.second.get(key);
                if(!value.means(readWhere, true))
                    return readWhere;
                return value;
            }});*/

        if(!readClasses.first.means(classes, true))
            return false;

        for(PropertyField property : properties)
            if(!readClasses.second.get(property).means(propertyClasses.get(property), true))
                return false;

        return true;
    }

    public SessionTable checkClasses(SQLSession session, BaseClass baseClass) throws SQLException, SQLHandledException {
        assert assertCheckClasses(session, baseClass);

        return this;
    }
    
    public void saveToDBForDebug(SQLSession sql) throws SQLException, IllegalAccessException, InstantiationException, ClassNotFoundException, SQLHandledException {
        SQLSession dbSql = ThreadLocalContext.getDbManager().createSQL();
        
        dbSql.startTransaction(DBManager.DEBUG_TIL);
        dbSql.ensureTable(this);
        dbSql.insertBatchRecords(name, keys, read(sql, ThreadLocalContext.getBusinessLogics().LM.baseClass).getMap());
        dbSql.commitTransaction();
        
        dbSql.close();
    }
    
    public static void saveToDBForDebug(ImSet<? extends Value> values, SQLSession sql) throws SQLException, IllegalAccessException, ClassNotFoundException, InstantiationException, SQLHandledException {
        for(Value value : values)
            if(value instanceof SessionTable)
                ((SessionTable)value).saveToDBForDebug(sql);
    }
}
