/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.util.*;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.math.BigDecimal;

// абстрактный класс источников
abstract class Source<K,V> {

    Collection<K> Keys;

    Source(Collection<? extends K> iKeys) {
        Keys=(Collection<K>)iKeys;
    }
    Source() {Keys=new ArrayList();}

    abstract String getSource(SQLSyntax Syntax);

    String getKeyString(K Key, String Alias) {
        return Alias + "." + getKeyName(Key);
    }

    String getPropertyString(V Value, String Alias) {
        return Alias + "." + getPropertyName(Value);
    }

    abstract Collection<V> getProperties();

    abstract String getDBType(V Property);

    // по умолчанию fillSelectString вызывает
    String fillSelect(Map<K, String> KeySelect, Map<V, String> PropertySelect, Collection<String> WhereSelect, SQLSyntax Syntax) {
        return fillSelectString(getSource(Syntax),KeySelect,PropertySelect,WhereSelect);
    }

    // заполняет структуру Select'а из строки Source
    String fillSelectString(String Source,Map<K, String> KeySelect, Map<V, String> PropertySelect, Collection<String> WhereSelect) {

        String Alias = "G";
        for(K Key : Keys)
            KeySelect.put(Key,getKeyString(Key,Alias));
        for(V Property : getProperties())
            PropertySelect.put(Property,getPropertyString(Property,Alias));

        return Source+" "+Alias;
    }

    String getSelect(List<K> KeyOrder, List<V> PropertyOrder, SQLSyntax Syntax) {
        Map<K,String> KeySelect = new HashMap();
        Map<V,String> PropertySelect = new HashMap();
        Collection<String> WhereSelect = new ArrayList();
        String From = fillSelect(KeySelect,PropertySelect,WhereSelect, Syntax);

        return getSelectString(From,KeySelect,PropertySelect,WhereSelect, KeyOrder, PropertyOrder);
    }

    String getSelectString(String From, Map<K, String> KeySelect, Map<V, String> PropertySelect, Collection<String> WhereSelect, List<K> KeyOrder, List<V> PropertyOrder) {
        String ExpressionString = "";
        for(Map.Entry<K,String> Key : KeySelect.entrySet()) {
            ExpressionString = (ExpressionString.length()==0?"":ExpressionString+",") + Key.getValue() + " AS " + getKeyName(Key.getKey());
            KeyOrder.add(Key.getKey());
        }
        for(Map.Entry<V,String> Property : PropertySelect.entrySet()) {
            ExpressionString = (ExpressionString.length()==0?"":ExpressionString+",") + Property.getValue() + " AS " + getPropertyName(Property.getKey());
            PropertyOrder.add(Property.getKey());
        }

        String WhereString = "";
        for(String Where : WhereSelect)
            WhereString = (WhereString.length()==0?"":WhereString+" AND ") + Where;

        return "SELECT " + (ExpressionString.length()==0?"1":ExpressionString) + " FROM " + From + (WhereString.length()==0?"":" WHERE " + WhereString);

    }

    Object convertResult(Object Result) {
        if(Result instanceof BigDecimal)
            return ((BigDecimal)Result).toBigInteger().intValue();
        else
            return Result;
    }

    // из-за templatов сюда кинем
    LinkedHashMap<Map<K,Integer>,Map<V,Object>> executeSelect(DataSession Session) throws SQLException {

        LinkedHashMap<Map<K,Integer>,Map<V,Object>> ExecResult = new LinkedHashMap();

        Statement Statement = Session.Connection.createStatement();

        System.out.println(getSelect(new ArrayList(),new ArrayList(), Session.Syntax));
        try {
            ResultSet Result = Statement.executeQuery(getSelect(new ArrayList(),new ArrayList(),Session.Syntax));
            try {
                while(Result.next()) {
                    Map<K,Integer> RowKeys = new HashMap();
                    for(K Key : Keys)
                        RowKeys.put(Key,(Integer)convertResult(Result.getObject(getKeyName(Key))));
                    Map<V,Object> RowProperties = new HashMap();
                    for(V Property : getProperties())
                        RowProperties.put(Property,convertResult(Result.getObject(getPropertyName(Property))));

                     ExecResult.put(RowKeys,RowProperties);
                }
            } finally {
                Result.close();
            }
        } finally {
            Statement.close();
        }

        return ExecResult;
    }

    void outSelect(DataSession Session) throws SQLException {
        // выведем на экран
        Collection<String> ResultFields = new ArrayList();
//        System.out.println(Select.GetSelect(ResultFields));

        LinkedHashMap<Map<K,Integer>,Map<V,Object>> Result = executeSelect(Session);

        for(Map.Entry<Map<K,Integer>,Map<V,Object>> RowMap : Result.entrySet()) {
            for(K Key : Keys) {
                System.out.print(getKeyName(Key)+"-"+RowMap.getKey().get(Key));
                System.out.print(" ");
            }
            System.out.print("---- ");
            for(V Property : getProperties()) {
                System.out.print(RowMap.getValue().get(Property));
                System.out.print(" ");
            }

            System.out.println("");
        }
    }

    // по сути тоже самое что и InsertSelect
    Table createView(DataSession Session,String ViewName,Map<K,KeyField> MapKeys,Map<V,PropertyField> MapProperties) throws SQLException {

        if(1==1) return null;

        Table View = new Table(ViewName);
        for(K Key : Keys) {
            KeyField KeyField = new KeyField(getKeyName(Key),"integer");
            View.Keys.add(KeyField);
            MapKeys.put(Key,KeyField);
        }
        for(V Property : getProperties()) {
            PropertyField PropertyField = new PropertyField(getPropertyName(Property),getDBType(Property));
            View.Properties.add(PropertyField);
            MapProperties.put(Property,PropertyField);
        }

        Session.Execute("CREATE VIEW " + ViewName + " AS " + getSelect(new ArrayList(),new ArrayList(),Session.Syntax));

        return View;
    }

    abstract Source<K, Object> merge(Source<?, ?> Merge, Map<K, ?> MergeKeys, Map<Object, Object> MergeProps);

    Map<K,String> KeyNames = new HashMap();
    int KeyCount = 0;

    String getKeyName(K Key) {
        String KeyName = KeyNames.get(Key);
        if(KeyName==null) {
            KeyName = "dkey"+(KeyCount++);
            KeyNames.put(Key,KeyName);
        }

        return KeyNames.get(Key);
    }

    // сделаем кэш Value
    Map<V,String> ValueNames = new HashMap();
    int ValueCount = 0;

    String getPropertyName(V Value) {
        String ValueName = ValueNames.get(Value);
        if(ValueName==null) {
            ValueName = "value"+(ValueCount++);
            ValueNames.put(Value,ValueName);
        }
        return ValueName;
    }

    // записывается в Join'ы
    void compileJoins(Join<K, V> Join, Map<SourceExpr, SourceExpr> Translated, Collection<SourceWhere> JoinWheres, Collection<Join> TranslatedJoins, boolean Outer) {
        
        Map<JoinExpr,SourceExpr> JoinTranslated = new HashMap();
        TranslatedJoins.add(Join.translate(Translated, JoinTranslated, Outer));
        Translated.putAll(JoinTranslated);
    }

    public Source<K,V> mergePropertyValue(Map<V, Integer> MergeValues) {
        
        JoinQuery<K,V> Query = new JoinQuery<K,V>(Keys);
        Join<K,V> SourceJoin = new UniJoin<K,V>(this,Query,true);
        Map<V,SourceExpr> DumbMap = new HashMap();
        // раскидываем по dumb'ам или на выход
        for(V Property : getProperties()) {
            SourceExpr PropertyExpr = SourceJoin.Exprs.get(Property);
            if(MergeValues.containsKey(Property))
                DumbMap.put(Property,PropertyExpr);
            else
                Query.Properties.put(Property,PropertyExpr);
        }
        Query.Wheres.add(new JoinWhere(new Join<V,Object>(new DumbSource<V,Object>(MergeValues, new HashMap()),DumbMap, true)));

        Query.compile();

        return Query;
    }
}

// постоянный источник из одной записи
class DumbSource<K,V> extends Source<K,V> {

    Map<K,Integer> ValueKeys;
    Map<V,Object> Values;

    DumbSource(Map<K,Integer> iValueKeys,Map<V,Object> iValues) {
        super(iValueKeys.keySet());
        ValueKeys = iValueKeys;
        Values = iValues;
    }

    String getSource(SQLSyntax Syntax) {
        return "dumb";
    }

    String getKeyString(K Key,String Alias) {
        return ValueKeys.get(Key).toString();
    }

    String getPropertyString(V Property,String Alias) {
        Object Value = Values.get(Property);
        if(Value instanceof String)
            return "'" + Value + "'";
        else
            return Value.toString();

    }

    Collection<V> getProperties() {
        return Values.keySet();
    }

    String getDBType(V Property) {
        Object ObjectValue = Values.get(Property);
        if(ObjectValue==null)
            throw new RuntimeException();

        if(ObjectValue instanceof Integer)
            return "integer";
        else
            return "char(50)";
    }

    String fillSelect(Map<K, String> KeySelect, Map<V, String> PropertySelect, Collection<String> WhereSelect, SQLSyntax Syntax) {
        for(Map.Entry<K,Integer> ValueKey : ValueKeys.entrySet())
            KeySelect.put(ValueKey.getKey(),ValueKey.getValue().toString());
        for(Map.Entry<V,Object> Value : Values.entrySet())
            PropertySelect.put(Value.getKey(),(Value.getValue()==null?"NULL":(Value.getValue() instanceof String?"'"+Value.getValue()+"'":Value.getValue().toString())));

        return "dumb";
    }

    void outSelect(DataSession Session) throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    Source<K, Object> merge(Source<?, ?> Merge, Map<K, ?> MergeKeys, Map<Object, Object> MergeProps) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    void compileJoins(Join<K, V> Join, Map<SourceExpr, SourceExpr> Translated, Collection<SourceWhere> JoinWheres, Collection<Join> TranslatedJoins, boolean Outer) {

        Map<SourceExpr,SourceExpr> JoinTranslated = new HashMap();
        for(Map.Entry<K,Integer> ValueKey : ValueKeys.entrySet()) {
            SourceExpr JoinExpr = Join.Joins.get(ValueKey.getKey()).translate(Translated);
            JoinTranslated.put(JoinExpr,new ValueSourceExpr(ValueKey.getValue()));
            // если св-во также докидываем в фильтр
            if(!(JoinExpr instanceof KeyExpr))
                JoinWheres.add(new FieldExprCompareWhere(JoinExpr,ValueKey.getValue(),0));
        }
        JoinQuery.mergeTranslator(Translated,JoinTranslated);

        // все свои значения также затранслируем
        for(Map.Entry<V,Object> ValueKey : Values.entrySet())
            Translated.put(Join.Exprs.get(ValueKey.getKey()),new ValueSourceExpr(ValueKey.getKey()));



        // после чего дальше даже не компилируемся - proceedCompile также словит транслированные ключи
//        super.compileJoins(Join, Translated, JoinWheres, TranslatedJoins, Outer);    //To change body of overridden methods use File | Settings | File Templates.
    }
}

// таблицы\Views

class Field {
    String Name;
    String Type;

    Field(String iName,String iType) {Name=iName;Type=iType;}

    String GetDeclare(SQLSyntax Syntax) {
        return Name + " " + Syntax.convertType(Type);
    }
}

class KeyField extends Field {
    KeyField(String iName,String iType) {super(iName,iType);}
}

class PropertyField extends Field {
    PropertyField(String iName,String iType) {super(iName,iType);}
}

class Table extends Source<KeyField,PropertyField> {
    String Name;
    Collection<PropertyField> Properties = new ArrayList();

    Table(String iName) {Name=iName;}

    String getSource(SQLSyntax Syntax) {
        return Name;
    }

    Set<List<PropertyField>> Indexes = new HashSet();

    Collection<PropertyField> getProperties() {
        return Properties;
    }

    String getDBType(PropertyField Property) {
        return Property.Type;
    }

    Source<KeyField, Object> merge(Source<?, ?> Merge, Map<KeyField, ?> MergeKeys, Map<Object, Object> MergeProps) {
        if(!(Merge instanceof Table)) return null;

        Table MergeTable = (Table)Merge;
        for(Map.Entry<KeyField,?> MapKey : MergeKeys.entrySet())
            if(!MapKey.getKey().equals(MapKey.getValue()))
                return null;

        for(PropertyField Field : Properties)
            MergeProps.put(Field,Field);

        return (Source<KeyField, Object>)((Source<KeyField,?>)this);
    }

    String getKeyName(KeyField Key) {
        return Key.Name;
    }

    String getPropertyName(PropertyField Property) {
        return Property.Name;
    }
}

// временная таблица на момент сессии
class SessionTable extends Table {

    SessionTable(String iName) {
        super(iName);
    }

    String getSource(SQLSyntax Syntax) {
        return Syntax.getSessionTableName(Name);
    }

}

abstract class Query<K,V> extends Source<K,V> {

    Query(Collection<? extends K> iKeys) {
        super(iKeys);
    }

    Query() {
        super();
    }

    String getSource(SQLSyntax Syntax) {
        return "(" + getSelect(new ArrayList(),new ArrayList(), Syntax) + ")";
    }

    // скомпилирован ли запрос
    boolean Compiled = false;

    void compile() {
        if(!Compiled) {
            proceedCompile();
            Compiled = true;
        }
    }

    // компилирует - запрос - приводит к выполняемой структуре, оптимизирует
    abstract void proceedCompile();
}

class Join<J,U> {
    Source<J,U> Source;
    Map<J,SourceExpr> Joins;

    boolean Inner;

    boolean Exclude = false;

    Map<U,JoinExpr<J,U>> Exprs = new HashMap();

    Join(Source<J,U> iSource,boolean iInner) {
        Source = iSource;
        Inner = iInner;
        for(U Property : Source.getProperties())
            Exprs.put(Property,new JoinExpr<J,U>(this,Property));

        Joins = new HashMap();
    }

    Join(Source<J,U> iSource,Map<J,SourceExpr> iJoins,boolean iInner) {
        Source = iSource;
        for(U Property : Source.getProperties())
            Exprs.put(Property,new JoinExpr<J,U>(this,Property));

        Joins = iJoins;
        Inner = iInner;
    }

    void fillJoins(List<Join> Joins,Set<Where> Wheres) {
        for(SourceExpr Join : this.Joins.values())
            Join.fillJoins(Joins,Wheres);

        if(!Joins.contains(this))
            Joins.add(this);

        if(Inner)
            Wheres.add(new JoinWhere(this));
    }

    String getFrom(Map<Join, String> JoinAlias, Collection<String> WhereSelect, SQLSyntax Syntax) {
        String JoinString = "";
        String Alias = "t"+(JoinAlias.size()+1);
        JoinAlias.put(this,Alias);

        for(Map.Entry<J,SourceExpr> KeyJoin : Joins.entrySet()) {
            String KeyJoinString = KeyJoin.getValue().getJoin(Source.getKeyString(KeyJoin.getKey(),Alias),JoinAlias, Syntax);
            if(KeyJoinString!=null) {
                if(WhereSelect==null)
                    JoinString = (JoinString.length()==0?"":JoinString+" AND ") + KeyJoinString;
                else
                    WhereSelect.add(KeyJoinString);
            }
        }

        return Source.getSource(Syntax) + " " + Alias + (WhereSelect==null?" ON "+(JoinString.length()==0?"1=1":JoinString):"");
    }

    Join<J,U> translate(Map<SourceExpr, SourceExpr> Translated,Map<JoinExpr, SourceExpr> JoinTranslated,boolean ForceOuter) {
        Map<J,SourceExpr> TransMap = new HashMap();
        for(Map.Entry<J,SourceExpr> TransJoin : Joins.entrySet())
            TransMap.put(TransJoin.getKey(),TransJoin.getValue().translate(Translated));

        Join<J,U> TranslatedJoin = new Join<J,U>(Source,TransMap,Inner && !ForceOuter);
        TranslatedJoin.Exclude = Exclude;
        for(Map.Entry<U,JoinExpr<J,U>> MapJoin : Exprs.entrySet())
            JoinTranslated.put(MapJoin.getValue(),TranslatedJoin.Exprs.get(MapJoin.getKey()));
        
        return TranslatedJoin;
    }

    Join<J, Object> merge(Join<?, ?> Merge, Map<SourceExpr, SourceExpr> Translated, Map<SourceExpr, SourceExpr> MergeTranslated, Map<JoinExpr, SourceExpr> JoinTranslated, Map<JoinExpr, SourceExpr> JoinMergeTranslated) {
        // нужно построить карту между Joins'ами (с учетом Translate'ов) (точнее и записать в новый Join Translate'утые выражения

        // проверить что кол-во Keys в Source совпадает

        Collection<Map<J, Object>> MapSet = (new MapBuilder<J, Object>()).buildPairs(Source.Keys, Merge.Source.Keys);
        if(MapSet==null) return null;

        for(Map<J,?> MapKeys : MapSet) {
            Map<J,SourceExpr> MergeExprs = new HashMap();
            for(J Key : Source.Keys) {
                SourceExpr MergeExpr = Joins.get(Key).translate(Translated);
                if(!Merge.Joins.get(MapKeys.get(Key)).translate(MergeTranslated).equals(MergeExpr))
                    break;
                else
                    MergeExprs.put(Key,MergeExpr);
            }

            if(MergeExprs.size()!=Source.Keys.size())
                continue;

            // есть уже карта попробуем merge'уть
            Map<Object, Object> MergeProps = new HashMap();
            Source<J, Object> MergeSource = Source.merge(Merge.Source, MapKeys, MergeProps);
            if(MergeSource!=null) {
                // нужно перетранслировать JoinExpr'ы
                Join<J, Object> MergedJoin = new Join<J, Object>(MergeSource, MergeExprs, Inner || Merge.Inner);

                for(Map.Entry<U,JoinExpr<J,U>> MapJoin : Exprs.entrySet())
                    JoinTranslated.put(MapJoin.getValue(),MergedJoin.Exprs.get(MapJoin.getKey()));

                for(Map.Entry<?,? extends JoinExpr<?,?>> MapJoin : Merge.Exprs.entrySet())
                    JoinMergeTranslated.put(MapJoin.getValue(),MergedJoin.Exprs.get(MergeProps.get(MapJoin.getKey())));

                return MergedJoin;
            }
        }

        return null;
    }
}

class MapJoin<J,U,K> extends Join<J,U> {

    // конструктор когда надо просто ключи протранслировать
    MapJoin(Source<J,U> iSource,Map<J,K> iJoins,JoinQuery<K,?> MapSource,boolean iInner) {
        super(iSource,iInner);

        for(J Implement : Source.Keys)
            Joins.put(Implement,MapSource.MapKeys.get(iJoins.get(Implement)));
    }

    MapJoin(Source<J,U> iSource,JoinQuery<K,?> MapSource,Map<K,J> iJoins,boolean iInner) {
         super(iSource,iInner);

         for(K Implement : MapSource.Keys)
             Joins.put(iJoins.get(Implement),MapSource.MapKeys.get(Implement));
     }

}

class UniJoin<K,U> extends Join<K,U> {
    UniJoin(Source<K,U> iSource,JoinQuery<K,?> MapSource,boolean iInner) {
        super(iSource,iInner);

        for(K Key : Source.Keys)
            Joins.put(Key,MapSource.MapKeys.get(Key));
    }
}

// абстрактный класс выражений
abstract class SourceExpr {

    void fillJoins(List<Join> Joins,Set<Where> Wheres) {}

    abstract String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax);

    String getJoin(String KeySource, Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return KeySource + "=" + getSource(JoinAlias, Syntax);
    }

    abstract String getDBType();

    // транслирует (рекурсивно инстанцирует объекты с новыми входными параметрами)
    SourceExpr translate(Map<SourceExpr, SourceExpr> Translated) {
        SourceExpr Translate = Translated.get(this);
        if(Translate==null) {
            Translate = proceedTranslate(Translated);
//            Translated.put(this,Translate);
//            return Translate;
        }

        return Translate;
    }

    abstract SourceExpr proceedTranslate(Map<SourceExpr, SourceExpr> Translated);
}

abstract class ObjectExpr extends SourceExpr {
}

class KeyExpr<K> extends ObjectExpr {
    K Key;

    KeyExpr(K iKey) {Key=iKey;}

    String Source;
    String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return Source;
    }

    String getJoin(String KeySource, Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        if(Source!=null) return super.getJoin(KeySource,JoinAlias, Syntax);

        Source = KeySource;
        return null;
    }

    String getDBType() {
        return "integer";
    }

    SourceExpr proceedTranslate(Map<SourceExpr, SourceExpr> Translated) {
        return this;
    }
}

class JoinExpr<J,U> extends ObjectExpr {
    U Property;
    Join<J,U> From;

    JoinExpr(Join<J,U> iFrom,U iProperty) {
        From = iFrom;
        Property = iProperty;
    }

    void fillJoins(List<Join> Joins,Set<Where> Wheres) {
        From.fillJoins(Joins,Wheres);
    }

    String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return From.Source.getPropertyString(Property,JoinAlias.get(From));
    }

    String getDBType() {
        return From.Source.getDBType(Property);
    }

    SourceExpr proceedTranslate(Map<SourceExpr, SourceExpr> Translated) {
        return this;
    }
}

// формулы
class ValueSourceExpr extends SourceExpr {

    Object Value;
    ValueSourceExpr(Object iValue) {
        Value=iValue;
        if(Value==null)
            Value = Value;
    }

    String getStringValue() {
        if(Value instanceof String)
            return "'" + Value + "'";
        else
            return Value.toString();
    }

    String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return getStringValue();
    }

    String getDBType() {
        if(Value instanceof Integer)
            return "integer";
        else
            return "char(50)";
    }

    SourceExpr proceedTranslate(Map<SourceExpr, SourceExpr> Translated) {
        return this;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ValueSourceExpr that = (ValueSourceExpr) o;

        if (Value != null ? !Value.equals(that.Value) : that.Value != null) return false;

        return true;
    }

    public int hashCode() {
        return (Value != null ? Value.hashCode() : 0);
    }
}

class StaticNullSourceExpr extends SourceExpr {

    String DBType;
    StaticNullSourceExpr(String iDBType) {
        DBType = iDBType;
    }

    String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return "NULL";
    }

    String getDBType() {
        return DBType;
    }

    SourceExpr proceedTranslate(Map<SourceExpr, SourceExpr> Translated) {
        return this;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StaticNullSourceExpr that = (StaticNullSourceExpr) o;

        if (!DBType.equals(that.DBType)) return false;

        return true;
    }

    public int hashCode() {
        return DBType.hashCode();
    }
}

class UnionSourceExpr extends SourceExpr {

    UnionSourceExpr(int iOperator) {Operator=iOperator;}

    // 0 - MAX
    // 1 - +
    // 2 - ISNULL
    int Operator;
    LinkedHashMap<SourceExpr,Integer> Operands = new LinkedHashMap();

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        LinkedHashMap<String,Integer> StringOperands = new LinkedHashMap<String,Integer>();
        for(Map.Entry<SourceExpr,Integer> Operand : Operands.entrySet())
            StringOperands.put(Operand.getKey().getSource(JoinAlias, Syntax),Operand.getValue());

        return UnionQuery.getExpr(StringOperands,Operator,false, Syntax);
    }

    String getDBType() {
        return Operands.keySet().iterator().next().getDBType();
    }

    SourceExpr proceedTranslate(Map<SourceExpr, SourceExpr> Translated) {
        UnionSourceExpr Translate = new UnionSourceExpr(Operator);
        for(Map.Entry<SourceExpr,Integer> Operand : Operands.entrySet())
            Translate.Operands.put(Operand.getKey().translate(Translated),Operand.getValue());
        return Translate;
    }


    void fillJoins(List<Join> Joins,Set<Where> Wheres) {
        for(SourceExpr Operand : Operands.keySet())
            Operand.fillJoins(Joins,Wheres);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UnionSourceExpr that = (UnionSourceExpr) o;

        if (!Operands.equals(that.Operands)) return false;

        return true;
    }

    public int hashCode() {
        return Operands.hashCode();
    }
}

class NullEmptySourceExpr extends SourceExpr {

    NullEmptySourceExpr(SourceExpr iExpr) {Expr=iExpr;}

    SourceExpr Expr;

    void fillJoins(List<Join> Joins, Set<Where> Wheres) {
        Expr.fillJoins(Joins, Wheres);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return Syntax.isNULL(Expr.getSource(JoinAlias, Syntax),(Expr.getDBType().equals("integer")?"0":"''"), false);
    }

    String getDBType() {
        return Expr.getDBType();
    }

    SourceExpr proceedTranslate(Map<SourceExpr, SourceExpr> Translated) {
        return new NullEmptySourceExpr(Expr.translate(Translated));
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NullEmptySourceExpr that = (NullEmptySourceExpr) o;

        if (!Expr.equals(that.Expr)) return false;

        return true;
    }

    public int hashCode() {
        return Expr.hashCode();
    }
}

class NullSourceExpr extends SourceExpr {

    NullSourceExpr(SourceExpr iExpr) {Expr=iExpr;}

    SourceExpr Expr;

    void fillJoins(List<Join> Joins, Set<Where> Wheres) {
        Expr.fillJoins(Joins, Wheres);
    }

    String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return "NULL";
    }

    String getDBType() {
        return Expr.getDBType();
    }

    SourceExpr proceedTranslate(Map<SourceExpr, SourceExpr> Translated) {
        return new NullSourceExpr(Expr.translate(Translated));
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NullSourceExpr that = (NullSourceExpr) o;

        if (!Expr.equals(that.Expr)) return false;

        return true;
    }

    public int hashCode() {
        return Expr.hashCode();
    }
}

class NullValueSourceExpr extends SourceExpr {

    NullValueSourceExpr(Collection<SourceExpr> iExprs,SourceExpr iTrueExpr,SourceExpr iFalseExpr) {Exprs=iExprs; TrueExpr=iTrueExpr; FalseExpr=iFalseExpr;};
    
    Collection<SourceExpr> Exprs;
    SourceExpr TrueExpr;
    SourceExpr FalseExpr;

    void fillJoins(List<Join> Joins,Set<Where> Wheres) {
        for(SourceExpr Expr : Exprs)
            Expr.fillJoins(Joins,Wheres);

        TrueExpr.fillJoins(Joins,Wheres);
        FalseExpr.fillJoins(Joins,Wheres);
    }

    String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {

        List<String> NullExprs = new ArrayList();
        for(SourceExpr Expr : Exprs)
            NullExprs.add(Expr.getSource(JoinAlias, Syntax));

        return getExpr(NullExprs,TrueExpr.getSource(JoinAlias, Syntax),FalseExpr.getSource(JoinAlias, Syntax),Syntax);
    }

    static String getExpr(Collection<String> NullExprs,String TrueSource,String FalseSource,SQLSyntax Syntax) {
        String Filter = "";
        for(String Expr : NullExprs)
            Filter = (Filter.length()==0?"":Filter+" OR ") + Expr + " IS NULL ";

        if(TrueSource.equals("NULL") && FalseSource.equals("NULL"))
            return "NULL";
        else
            return "(CASE WHEN " + Filter + " THEN " + TrueSource + " ELSE " + FalseSource + " END)";
    }

    String getDBType() {
        return TrueExpr.getDBType();
    }

    SourceExpr proceedTranslate(Map<SourceExpr, SourceExpr> Translated) {

        Collection<SourceExpr> TransExprs = new ArrayList();
        for(SourceExpr Expr : Exprs)
            TransExprs.add(Expr.translate(Translated));

        return new NullValueSourceExpr(TransExprs, TrueExpr.translate(Translated), FalseExpr.translate(Translated));
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NullValueSourceExpr that = (NullValueSourceExpr) o;

        if (!Exprs.equals(that.Exprs)) return false;
        if (!FalseExpr.equals(that.FalseExpr)) return false;
        if (!TrueExpr.equals(that.TrueExpr)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = Exprs.hashCode();
        result = 31 * result + TrueExpr.hashCode();
        result = 31 * result + FalseExpr.hashCode();
        return result;
    }
}

class IsNullSourceExpr extends SourceExpr {

    IsNullSourceExpr(SourceExpr iPrimaryExpr,SourceExpr iSecondaryExpr) {PrimaryExpr=iPrimaryExpr;SecondaryExpr=iSecondaryExpr;};
    
    SourceExpr PrimaryExpr;
    SourceExpr SecondaryExpr;

    void fillJoins(List<Join> Joins, Set<Where> Wheres) {
        PrimaryExpr.fillJoins(Joins,Wheres);
        SecondaryExpr.fillJoins(Joins,Wheres);
    }

    String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return Syntax.isNULL(PrimaryExpr.getSource(JoinAlias, Syntax),SecondaryExpr.getSource(JoinAlias, Syntax), false);
    }

    String getDBType() {
        return PrimaryExpr.getDBType();
    }

    SourceExpr proceedTranslate(Map<SourceExpr, SourceExpr> Translated) {
        return new IsNullSourceExpr(PrimaryExpr.translate(Translated),SecondaryExpr.translate(Translated));
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IsNullSourceExpr that = (IsNullSourceExpr) o;

        if (!PrimaryExpr.equals(that.PrimaryExpr)) return false;
        if (!SecondaryExpr.equals(that.SecondaryExpr)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = PrimaryExpr.hashCode();
        result = 31 * result + SecondaryExpr.hashCode();
        return result;
    }
}

class FormulaSourceExpr extends SourceExpr {

    FormulaSourceExpr(String iFormula) {
        Formula = iFormula;
        Params = new HashMap<String,SourceExpr>();
    }
    
    String Formula;

    Map<String,SourceExpr> Params;

    void fillJoins(List<Join> Joins, Set<Where> Wheres) {
        for(SourceExpr Param : Params.values())
            Param.fillJoins(Joins,Wheres);
    }

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {

        String SourceString = Formula;
        Iterator<String> i = Params.keySet().iterator();

        while (i.hasNext()) {
            String Prm = i.next();
            SourceString = SourceString.replace(Prm,Params.get(Prm).getSource(JoinAlias, Syntax));
        }

         return SourceString;
     }

    String getDBType() {
        return Params.values().iterator().next().getDBType();
    }

    SourceExpr proceedTranslate(Map<SourceExpr, SourceExpr> Translated) {
        FormulaSourceExpr TransExpr = new FormulaSourceExpr(Formula);
        for(Map.Entry<String,SourceExpr> Prm : Params.entrySet())
            TransExpr.Params.put(Prm.getKey(),Prm.getValue().translate(Translated));

        return TransExpr;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FormulaSourceExpr that = (FormulaSourceExpr) o;

        if (!Formula.equals(that.Formula)) return false;
        if (!Params.equals(that.Params)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = Formula.hashCode();
        result = 31 * result + Params.hashCode();
        return result;
    }
}

// SourceExpr возвращаюший 1 если FormulaExpr true и Null в противном случае
class FormulaWhereSourceExpr extends SourceExpr {

    FormulaSourceExpr FormulaExpr;
    boolean NotNull;

    FormulaWhereSourceExpr(FormulaSourceExpr iFormulaExpr,boolean iNotNull) {
        FormulaExpr = iFormulaExpr;
        NotNull = iNotNull;
    }

    void fillDependExprs(LinkedHashSet<SourceExpr> Exprs) {

    }

    void fillJoins(List<Join> Joins,Set<Where> Wheres) {
        FormulaExpr.fillJoins(Joins,Wheres);
        if(NotNull)
            Wheres.add(new SourceExprWhere(FormulaExpr,false));
    }

    String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        // если NotNull нефиг 2 раза проверять
        if(NotNull)
            return "1";
        else
            return "CASE WHEN " + FormulaExpr.getSource(JoinAlias, Syntax) + " THEN 1 ELSE NULL END";
    }

    String getDBType() {
        return FormulaExpr.getDBType();
    }

    SourceExpr proceedTranslate(Map<SourceExpr, SourceExpr> Translated) {
        return new FormulaWhereSourceExpr((FormulaSourceExpr) FormulaExpr.translate(Translated),NotNull);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FormulaWhereSourceExpr that = (FormulaWhereSourceExpr) o;

        if (NotNull != that.NotNull) return false;
        if (!FormulaExpr.equals(that.FormulaExpr)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = FormulaExpr.hashCode();
        result = 31 * result + (NotNull ? 1 : 0);
        return result;
    }
}


// Wheres
abstract class Where {

    abstract void fillJoins(List<Join> Joins,Set<Where> Wheres);

    abstract void fillWheres(Collection<SourceWhere> Wheres);
}

class JoinWhere extends Where {
    Join From;

    JoinWhere(Join iFrom) {From=iFrom;}

    void fillJoins(List<Join> Joins,Set<Where> Wheres) {
        From.fillJoins(Joins,Wheres);
    }

    // по сути разделяет отборы на Inner и прямые
    void fillWheres(Collection<SourceWhere> Wheres) {
        From.Inner = true;
    }
}

abstract class SourceWhere extends Where {

    void fillWheres(Collection<SourceWhere> Wheres) {
        Wheres.add(this);
    }

    abstract String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax);

    abstract SourceWhere translate(Map<SourceExpr, SourceExpr> Translated);
}

class ExcludeJoinWhere extends Where {
    Join From;

    ExcludeJoinWhere(Join iFrom) {From=iFrom;}

    void fillWheres(Collection<SourceWhere> Wheres) {
        From.Inner = false;
        From.Exclude = true;
    }

    void fillJoins(List<Join> Joins,Set<Where> Wheres) {
        From.fillJoins(Joins,Wheres);
    }
}

class FieldExprCompareWhere extends SourceWhere {

    SourceExpr Source;
    Object Value;
    int Compare;

    FieldExprCompareWhere(SourceExpr iSource,Object iValue,int iCompare) {Source=iSource;Value=iValue;Compare=iCompare;}

    public String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return Source.getSource(JoinAlias, Syntax) + (Compare==0?"=":(Compare==1?">":(Compare==2?"<":(Compare==3?">=":(Compare==4?"<=":"<>"))))) + (Value instanceof String?"'"+Value+"'":(Value instanceof SourceExpr?((SourceExpr)Value).getSource(JoinAlias, Syntax):Value.toString()));
    }

    SourceWhere translate(Map<SourceExpr, SourceExpr> Translated) {
        return new FieldExprCompareWhere(Source.translate(Translated),
                (Value instanceof SourceExpr?((SourceExpr)Value).translate(Translated):Value),Compare);
    }

    void fillJoins(List<Join> Joins,Set<Where> Wheres) {
        Source.fillJoins(Joins,Wheres);
        if(Value instanceof SourceExpr)
            ((SourceExpr)Value).fillJoins(Joins,Wheres);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FieldExprCompareWhere that = (FieldExprCompareWhere) o;

        if (Compare != that.Compare) return false;
        if (!Source.equals(that.Source)) return false;
        if (!Value.equals(that.Value)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = Source.hashCode();
        result = 31 * result + Value.hashCode();
        result = 31 * result + Compare;
        return result;
    }
}

class SourceIsNullWhere extends SourceWhere {
    SourceIsNullWhere(SourceExpr iSource,boolean iNot) {Source=iSource; Not=iNot;}

    SourceExpr Source;
    boolean Not;

    String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return (Not?"NOT ":"") + Source.getSource(JoinAlias, Syntax) + " IS NULL";
    }

    SourceWhere translate(Map<SourceExpr, SourceExpr> Translated) {
        return new SourceIsNullWhere(Source.translate(Translated),Not);
    }

    void fillJoins(List<Join> Joins, Set<Where> Wheres) {
        Source.fillJoins(Joins,Wheres);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SourceIsNullWhere that = (SourceIsNullWhere) o;

        if (Not != that.Not) return false;
        if (!Source.equals(that.Source)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = Source.hashCode();
        result = 31 * result + (Not ? 1 : 0);
        return result;
    }
}

class FieldOPWhere extends SourceWhere {
    FieldOPWhere(SourceWhere iOp1,SourceWhere iOp2,boolean iAnd) {Op1=iOp1;Op2=iOp2;And=iAnd;}

    SourceWhere Op1, Op2;
    boolean And;

    void fillJoins(List<Join> Joins, Set<Where> Wheres) {
        Op1.fillJoins(Joins,Wheres);
        Op2.fillJoins(Joins,Wheres);
    }

    String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return "(" + Op1.getSource(JoinAlias, Syntax) + " " + (And?"AND":"OR") + " " + Op2.getSource(JoinAlias, Syntax) + ")";
    }

    SourceWhere translate(Map<SourceExpr, SourceExpr> Translated) {
        return new FieldOPWhere(Op1.translate(Translated),Op2.translate(Translated),And);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FieldOPWhere that = (FieldOPWhere) o;

        if (And != that.And) return false;
        if (!Op1.equals(that.Op1)) return false;
        if (!Op2.equals(that.Op2)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = Op1.hashCode();
        result = 31 * result + Op2.hashCode();
        result = 31 * result + (And ? 1 : 0);
        return result;
    }
}

class SourceExprWhere extends SourceWhere {
    SourceExprWhere(SourceExpr iSource,boolean iNot) {Source=iSource; Not=iNot;}

    SourceExpr Source;
    boolean Not;

    String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        return (Not?"NOT ":"") + Source.getSource(JoinAlias, Syntax);
    }

    SourceWhere translate(Map<SourceExpr, SourceExpr> Translated) {
        return new SourceExprWhere(Source.translate(Translated), Not);
    }

    void fillJoins(List<Join> Joins, Set<Where> Wheres) {
        Source.fillJoins(Joins,Wheres);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SourceExprWhere that = (SourceExprWhere) o;

        if (Not != that.Not) return false;
        if (!Source.equals(that.Source)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = Source.hashCode();
        result = 31 * result + (Not ? 1 : 0);
        return result;
    }
}

class FieldSetValueWhere extends SourceWhere {

    FieldSetValueWhere(SourceExpr iExpr,Collection<Integer> iSetValues) {Expr=iExpr; SetValues=iSetValues;};
    SourceExpr Expr;
    Collection<Integer> SetValues;

    String getSource(Map<Join, String> JoinAlias, SQLSyntax Syntax) {
        String ListString = "";
        for(Integer Value : SetValues)
            ListString = (ListString.length()==0?"":ListString+',') + Value;

        return Expr.getSource(JoinAlias, Syntax) + " IN (" + ListString + ")";
    }

    SourceWhere translate(Map<SourceExpr, SourceExpr> Translated) {
        return new FieldSetValueWhere(Expr.translate(Translated),SetValues);
    }

    void fillJoins(List<Join> Joins, Set<Where> Wheres) {
        Expr.fillJoins(Joins,Wheres);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FieldSetValueWhere that = (FieldSetValueWhere) o;

        if (!Expr.equals(that.Expr)) return false;
        if (!SetValues.equals(that.SetValues)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = Expr.hashCode();
        result = 31 * result + SetValues.hashCode();
        return result;
    }
}

abstract class SelectQuery<K,V> extends Query<K,V> {

    SelectQuery() {super();}
    SelectQuery(Collection<? extends K> iKeys) {super(iKeys);}

}

// запрос Join
class JoinQuery<K,V> extends SelectQuery<K,V> {
    Map<V,SourceExpr> Properties = new HashMap();
    Collection<Where> Wheres = new ArrayList();

    Map<K,SourceExpr> MapKeys = new HashMap();
    
    JoinQuery() {super();}
    JoinQuery(Collection<? extends K> iKeys) {
        super(iKeys);

        for(K Key : Keys)
            MapKeys.put(Key,new KeyExpr<K>(Key));
    }

    KeyExpr<K> addKey(K Key) {
        Keys.add(Key);
        KeyExpr<K> KeyExpr = new KeyExpr<K>(Key);
        MapKeys.put(Key,KeyExpr);
        return KeyExpr;
    }


    Collection<V> getProperties() {
        return Properties.keySet();
    }

    String getDBType(V Property) {
        return Properties.get(Property).getDBType();
    }

    void putDumbJoin(Map<K,Integer> KeyValues) {

        Join<K,Object> DumbJoin = new Join<K,Object>(new DumbSource<K,Object>(KeyValues,new HashMap()),true);
        for(K Object : KeyValues.keySet())
            DumbJoin.Joins.put(Object,MapKeys.get(Object));
        Wheres.add(new JoinWhere(DumbJoin));
    }

    // скомпилированные св-ва
    List<Join> Joins = new ArrayList();
    Collection<SourceWhere> SourceWheres = new ArrayList();

    void proceedCompile() {

        // из всех Properties,Wheres вытягивают Joins, InnerJoins, SourceWhere        
        Set<Where> QueryWheres = new HashSet();
        Joins = new ArrayList();
        SourceWheres = new ArrayList();
        for(SourceExpr PropertyExpr : Properties.values())
            PropertyExpr.fillJoins(Joins,QueryWheres);
        for(Where Where : Wheres) {
            Where.fillJoins(Joins,QueryWheres);
            QueryWheres.add(Where);
        }

        for(Where Where : QueryWheres)
            Where.fillWheres(SourceWheres);

/*        for(Join Join : Joins)
            // все Query компилируем
            if(Join.Source instanceof Query)
                ((Query)Join.Source).compile();
  */
//        if(1==1) return;
        // сначала по всем Inner Dumb'ам пробегаем делаем :
/*        for(Join<?,?> Join : Joins) {
            if(Join.Source instanceof DumbSource) {
                DumbSource<?,?> Dumb = (DumbSource<?,?>)Join.Source;
                Collection<K> RemoveKeys = new ArrayList();
                for(Object Key : Dumb.Keys) {
                    SourceExpr JoinExpr = Join.Joins.get(Key);
                    Object Value = Dumb.Values.get(Key);
                    changeExpr(JoinExpr,new ValueSourceExpr(Value));
                    if(!(JoinExpr instanceof KeyExpr))
                        SourceWheres.add(new FieldExprCompareWhere(JoinExpr,Value,0));
                }
            }
        }
  */

        Map<SourceExpr,SourceExpr> Translated = new HashMap();
        List<Join> TranslatedJoins = new ArrayList();

        for(Join Join : Joins)
            Join.Source.compileJoins(Join, Translated, SourceWheres, TranslatedJoins, false);

        // слитыве операнды
        List<Join> MergedJoins = new ArrayList();

        // если есть 2 (compatible) Join объединяем - заменяем все 1-й и 2-й JoinExpr'ы на JoinExpr'ы результата
        while(TranslatedJoins.size()>0) {
            Iterator<Join> itOperand = TranslatedJoins.iterator();
            // берем первый элемент, ишем с кем слить
            Join ToMerge = itOperand.next();
            itOperand.remove();

            boolean ToMergeTranslated = false;
            while(itOperand.hasNext()) {
                Map<JoinExpr,SourceExpr> JoinTranslated = new HashMap();
                Join Check = itOperand.next();
                Join MergedSource = ToMerge.merge(Check, Translated, Translated, JoinTranslated, JoinTranslated);

                if(MergedSource!=null) {
                    mergeTranslator(Translated,JoinTranslated);

                    ToMergeTranslated = true;
                    itOperand.remove();

                    ToMerge = MergedSource;
                }
            }

            // все равно надо хоть раз протранслировать
            if(!ToMergeTranslated) {
                Map<JoinExpr,SourceExpr> JoinTranslated = new HashMap();
                ToMerge = ToMerge.translate(Translated, JoinTranslated, false);
                mergeTranslator(Translated,JoinTranslated);
            }

            MergedJoins.add(ToMerge);
        }

        Joins = MergedJoins;
        // пока еще раз перетранслируем, потом если надо скинем
        for(Map.Entry<V,SourceExpr> MapProperty : Properties.entrySet())
            MapProperty.setValue(MapProperty.getValue().translate(Translated));
//        TranslatedWheres = new ArrayList();
        Collection<SourceWhere> TranslatedWheres = new ArrayList();
        // перетранслируем
        for(SourceWhere Where : SourceWheres)
            TranslatedWheres.add(Where.translate(Translated));
        SourceWheres = TranslatedWheres;

        // проверим если вдруг Translat'ился ключ закинем сразу в Source         
        for(SourceExpr Key : MapKeys.values()) {
            SourceExpr ValueKey = Key.translate(Translated);
            if(Key!=ValueKey)
                ((KeyExpr<K>)Key).Source = ((ValueSourceExpr)ValueKey).getStringValue();
        }
    }

    static void mergeTranslator(Map<SourceExpr, SourceExpr> Translated, Map<? extends SourceExpr, SourceExpr> MergeTranslated) {
        for(Map.Entry<SourceExpr,SourceExpr> MapTranslate : Translated.entrySet())
            MapTranslate.setValue(MapTranslate.getValue().translate((Map<SourceExpr, SourceExpr>) MergeTranslated));
        Translated.putAll(MergeTranslated);        
    }

    private boolean recMerge(ListIterator<Join> RecJoin, Collection<Join> ToMergeJoins, Map<SourceExpr, SourceExpr> Translated, Map<SourceExpr, SourceExpr> MergeTranslated, Set<Join> MergedJoins, List<Join> TranslatedJoins) {

        if(!RecJoin.hasNext())
            return true;

        Join<?,?> CurrentJoin = RecJoin.next();

        for(Join<?,?> ToMergeJoin : ToMergeJoins)
            if(!MergedJoins.contains(ToMergeJoin) && CurrentJoin.Inner==ToMergeJoin.Inner) {
                // надо бы проверить что Join уже не merge'ут
                Map<JoinExpr,SourceExpr> JoinTranslated = new HashMap();
                Map<JoinExpr,SourceExpr> JoinMergeTranslated = new HashMap();
                Join<?,?> MergedJoin = CurrentJoin.merge(ToMergeJoin, Translated, MergeTranslated, JoinTranslated, JoinMergeTranslated);
                if(MergedJoin!=null) {
                    MergedJoins.add(ToMergeJoin);
                    TranslatedJoins.add(MergedJoin);
                    Translated.putAll(JoinTranslated);
                    MergeTranslated.putAll(JoinMergeTranslated);
                    if(recMerge(RecJoin, ToMergeJoins, Translated, MergeTranslated, MergedJoins, TranslatedJoins))
                        return true;
                    MergedJoins.remove(ToMergeJoin);
                    TranslatedJoins.remove(MergedJoin);
                    MapExtend.removeAll(Translated,JoinTranslated.keySet());
                    MapExtend.removeAll(MergeTranslated,JoinMergeTranslated.keySet());
                }
            }

        RecJoin.previous();

        return false;
    }

    Source<K, Object> merge(Source<?, ?> Merge, Map<K, ?> MergeKeys, Map<Object, Object> MergeProps) {
        // Set<Join> merge'им с Set<Join> если совпадают, докидываем Left Joins (пытаясь попарно слить), проверяем на совпадение SourceWheres
        // для сравнения Join'ов (как и Where) нужны сразу же "перекодированные" выражения									так как сами бежим по Map'ам то перекодируем
        // накапливаем карты слияний , по умолчанию в этот Map закидываем KeyExpr'ы, затем они накапливаются
        // также надо затем слить Property причем сравнив чтобы не дать одинаковые

        if(!(Merge instanceof JoinQuery)) return null;

        JoinQuery<?,?> MergeJoin = (JoinQuery<?,?>) Merge;

        // также надо бы проверить на кол-во Inner Join'ов, а также SourceWherers совпадает
        if(SourceWheres.size()!=MergeJoin.SourceWheres.size()) return null;

        int InnerCount = 0;
        for(Join Join : Joins)
            if(Join.Inner) InnerCount++;
        int InnerMergeCount = 0;
        for(Join Join : MergeJoin.Joins)
            if(Join.Inner) InnerMergeCount++;
        if(InnerCount!=InnerMergeCount) return null;

        JoinQuery<K,Object> Result = new JoinQuery<K,Object>(Keys);
        Result.Compiled = true;

        // попытаемся все ключи помапить друг на друга
        Set<Join> MergedJoins = new HashSet();
        // замапим ключи
        Map<SourceExpr,SourceExpr> Translated = new HashMap();
        Map<SourceExpr,SourceExpr> MergeTranslated = new HashMap();
        for(Map.Entry<K,SourceExpr> MapKey : Result.MapKeys.entrySet()) {
            Translated.put(MapKeys.get(MapKey.getKey()),MapKey.getValue());
            MergeTranslated.put(MergeJoin.MapKeys.get(MergeKeys.get(MapKey.getKey())),MapKey.getValue());
        }
        if(recMerge(Joins.listIterator(), MergeJoin.Joins, Translated, MergeTranslated, MergedJoins, Result.Joins)) {
            // проверим что SourceWheres совпадают (потом может закинем в recMerge)
            for(SourceWhere Where : SourceWheres)
                Result.SourceWheres.add(Where.translate(Translated));

            for(SourceWhere Where : MergeJoin.SourceWheres)
                if(!Result.SourceWheres.contains(Where.translate(MergeTranslated)))
                    return null;

            // сделаем Map в обратную сторону, чтобы не кидать
            Map<SourceExpr,Object> BackProperties = new HashMap();
            for(Map.Entry<V,SourceExpr> MapProp : Properties.entrySet()) {
                SourceExpr TransExpr = MapProp.getValue().translate(Translated);

                Result.Properties.put(MapProp.getKey(),TransExpr);
                BackProperties.put(TransExpr,MapProp.getKey());
            }
            for(Map.Entry<?,SourceExpr> MapProp : MergeJoin.Properties.entrySet()) {
                SourceExpr TransExpr = MapProp.getValue().translate(MergeTranslated);

                Object PropertyObject = BackProperties.get(TransExpr);
                if(PropertyObject==null) {
                    PropertyObject = new Object();
                    Result.Properties.put(PropertyObject,TransExpr);
                }
                MergeProps.put(MapProp.getKey(),PropertyObject);
            }

            return Result;
        }

        return null;
    }

    // перетранслирует
    void compileJoins(Join<K, V> Join, Map<SourceExpr, SourceExpr> Translated, Collection<SourceWhere> JoinWheres, Collection<Join> TranslatedJoins, boolean Outer) {

        compile();

        // если есть JoinQuery (пока если Left без Wheres) то доливаем все Join'ы с сооты. Map'ами, JoinExpr заменяем на SourceExpr'ы с перебитыми KeyExpr'ами и соотв. JoinRxpr'ами
        Outer = Outer || !Join.Inner;
        if(!Join.Exclude && (!Outer || SourceWheres.size()==0)) {
            // закинем перекодирование ключей
            for(Map.Entry<K,SourceExpr> MapKey : MapKeys.entrySet())
                Translated.put(MapKey.getValue(),Join.Joins.get(MapKey.getKey()).translate(Translated));

            // нужно также рекурсию гнать на случай LJ (A J (B FJ C))
            for(Join CompileJoin : Joins)
                CompileJoin.Source.compileJoins(CompileJoin, Translated, JoinWheres, TranslatedJoins, Outer);

            JoinWheres.addAll(SourceWheres);

            // хакинем перекодирование значение
            for(Map.Entry<V,SourceExpr> MapProperty : Properties.entrySet())
                Translated.put(Join.Exprs.get(MapProperty.getKey()),MapProperty.getValue().translate(Translated));
        } else
            super.compileJoins(Join, Translated, JoinWheres, TranslatedJoins, Outer);
    }
    
    String fillSelect(Map<K, String> KeySelect, Map<V, String> PropertySelect, Collection<String> WhereSelect, SQLSyntax Syntax) {

        compile();

        Map<Join,String> JoinAlias = new HashMap();
        String From = "";
        // теперь определим KeyExpr'ы заодно строя Join'ы
        for(Join Join : Joins)
            if(Join.Inner)
                From = (From.length()==0?"":From + " JOIN ") + Join.getFrom(JoinAlias,(From.length()==0?WhereSelect:null), Syntax);

        // проверим что все KeyExpr'ы заполнились
        for(SourceExpr Key : MapKeys.values())
            if(((KeyExpr<K>)Key).Source==null)
                throw new RuntimeException();
        if(From.length()==0)
            From = "dumb";

        for(Join Join : Joins)
            if(!Join.Inner)
                From = From + " LEFT JOIN " + Join.getFrom(JoinAlias,null, Syntax);

        // ключи заполняем
        for(Map.Entry<K,SourceExpr> MapKey : MapKeys.entrySet())
            KeySelect.put(MapKey.getKey(),((KeyExpr<K>)MapKey.getValue()).Source);
        // погнали Properties заполнять
        for(Map.Entry<V,SourceExpr> JoinProp : Properties.entrySet()) {
            String PropertyValue = JoinProp.getValue().getSource(JoinAlias, Syntax);
            if(PropertyValue.equals("NULL"))
                PropertyValue = Syntax.getNullValue(JoinProp.getValue().getDBType()); 
            PropertySelect.put(JoinProp.getKey(),PropertyValue);
        }

        // exclude'ы запишем в Where
        for(Join Join : Joins)
            if(Join.Exclude)
                WhereSelect.add(Join.Source.getKeyString(Join.Source.Keys.iterator().next(),JoinAlias.get(Join)) + " IS NULL");

        for(SourceWhere Where : SourceWheres)
            WhereSelect.add(Where.getSource(JoinAlias, Syntax));

        // Source'ы в KeyExpr надо сбросить (из за compileJoins'а Dumbов не надо)
//        for(SourceExpr Key : MapKeys.values())
//            ((KeyExpr<K>)Key).Source = null;

        return From;
    }

}

class OrderedJoinQuery<K,V> extends JoinQuery<K,V> {

    int Top;

    boolean Up;
    LinkedHashMap<SourceExpr,Boolean> Orders = new LinkedHashMap();

    JoinQuery<K,Object> Query;

    // кривоватая перегрузка но плодить параметры еще хуже
    String getSelect(List<K> KeyOrder, List<V> PropertyOrder, SQLSyntax Syntax) {

        compile();

        Map<K,String> KeySelect = new HashMap();
        Map<Object,String> PropertySelect = new HashMap();
        Collection<String> WhereSelect = new ArrayList();
        // закинем в Properties 
        String From = Query.fillSelect(KeySelect,PropertySelect,WhereSelect,Syntax);

        String ExpressionString = "";
        for(Map.Entry<K,String> Key : KeySelect.entrySet()) {
            ExpressionString = (ExpressionString.length()==0?"":ExpressionString+",") + Key.getValue() + " AS " + getKeyName(Key.getKey());
            KeyOrder.add(Key.getKey());
        }
        for(V Property : Properties.keySet()) {
            ExpressionString = (ExpressionString.length()==0?"":ExpressionString+",") + PropertySelect.get(Property) + " AS " + getPropertyName(Property);
            PropertyOrder.add(Property);
        }

        String WhereString = "";
        for(String Where : WhereSelect)
            WhereString = (WhereString.length()==0?"":WhereString+" AND ") + Where;

        String OrderString = "";
        for(Map.Entry<SourceExpr,Boolean> Order : Orders.entrySet())
            OrderString = (OrderString.length()==0?"":OrderString+",") + PropertySelect.get(Order.getKey())+(Up==Order.getValue()?" ASC":" DESC");

        return "SELECT " + Syntax.getTop(Top, ExpressionString + " FROM " + From,
                (OrderString.length()==0?"":" ORDER BY "+OrderString),WhereString);
    }

    OrderedJoinQuery(Collection<? extends K> iKeys) {super(iKeys);}

    void proceedCompile() {

        Query = new JoinQuery<K,Object>(Keys);
        // изврат конечно но по другому фиг сделаешь
        Query.MapKeys = MapKeys;
        Query.Wheres.addAll(Wheres);
        Query.Properties.putAll(Properties);
        for(SourceExpr Order : Orders.keySet())
            Query.Properties.put(Order,Order);
        Query.compile();
    }
}

abstract class SourceUnion<K> {
    abstract SourceExpr translateExpr(Map<Source<K, Object>, Join<K, ?>> MapJoins, Map<SourceExpr, SourceExpr> Translated);

    SourceUnion translate(Map<PropertyUnion,SourceUnion> ToTranslate,Map<SourceUnion,SourceUnion> Translated) {
        SourceUnion<K> TranslateUnion = Translated.get(this);
        if(TranslateUnion==null) {
            TranslateUnion = proceedTranslate(ToTranslate, Translated);
            Translated.put(this,TranslateUnion);
        }

        return TranslateUnion;
    }

    abstract SourceUnion proceedTranslate(Map<PropertyUnion, SourceUnion> ToTranslate, Map<SourceUnion, SourceUnion> Translated);

    abstract String getString(Map<Source<K, Object>, String> MapAliases, SQLSyntax Syntax);

    // по сути следующие 2 метода для 3-го оператора
    abstract void fillSources(Collection<Source> Sources);
    abstract boolean findOperator(int ToFind);

    abstract public String getDBType();
}

class PropertyUnion<K,V> extends SourceUnion<K> {
    V Property;
    Source<K,V> From;

    PropertyUnion(V iProperty, Source<K, V> iFrom) {
        Property = iProperty;
        From = iFrom;
    }

    SourceExpr translateExpr(Map<Source<K, Object>, Join<K, ?>> MapJoins, Map<SourceExpr, SourceExpr> Translated) {
        return MapJoins.get(From).Exprs.get(Property).translate(Translated);
    }

    SourceUnion proceedTranslate(Map<PropertyUnion, SourceUnion> ToTranslate, Map<SourceUnion, SourceUnion> Translated) {
        SourceUnion TranslateUnion = ToTranslate.get(this);
        if(TranslateUnion==null)
            return this;
        else
            return TranslateUnion.translate(ToTranslate, Translated);
    }

    String getString(Map<Source<K, Object>, String> MapAliases, SQLSyntax Syntax) {
        return MapAliases.get(From)+"."+From.getPropertyName(Property);
    }

    void fillSources(Collection<Source> Sources) {
        Sources.add(From);
    }

    boolean findOperator(int ToFind) {
        return false;
    }

    public String getDBType() {
        return From.getDBType(Property);
    }

}

class OperationUnion<K> extends SourceUnion<K> {
    LinkedHashMap<SourceUnion<K>,Integer> Operands = new LinkedHashMap();
    int Operator;

    OperationUnion(int iOperator) {
        Operator = iOperator;
    }

    SourceExpr translateExpr(Map<Source<K, Object>, Join<K, ?>> MapJoins, Map<SourceExpr, SourceExpr> Translated) {
        UnionSourceExpr UnionExpr = new UnionSourceExpr(Operator);

        for(Map.Entry<SourceUnion<K>,Integer> Operand : Operands.entrySet())
            UnionExpr.Operands.put(Operand.getKey().translateExpr(MapJoins, Translated),Operand.getValue());

        return UnionExpr;
    }

    SourceUnion proceedTranslate(Map<PropertyUnion, SourceUnion> ToTranslate, Map<SourceUnion, SourceUnion> Translated) {
        OperationUnion<K> TranslateUnion = new OperationUnion<K>(Operator);

        for(Map.Entry<SourceUnion<K>,Integer> Operand : Operands.entrySet())
            TranslateUnion.Operands.put(Operand.getKey().translate(ToTranslate, Translated),Operand.getValue());

        return TranslateUnion;
    }

    String getString(Map<Source<K, Object>, String> MapAliases, SQLSyntax Syntax) {
        if(Operator<=2) {
            // обычный оператор
            LinkedHashMap<String,Integer> StringOperands = new LinkedHashMap<String,Integer>();
            for(Map.Entry<SourceUnion<K>,Integer> Operand : Operands.entrySet())
                StringOperands.put(Operand.getKey().getString(MapAliases, Syntax),Operand.getValue());

            return UnionQuery.getExpr(StringOperands,Operator,false,Syntax);
        } else {
            // оператор выбора
            String SourceExpr = null;
            for(Map.Entry<SourceUnion<K>,Integer> Operand : Operands.entrySet()) {
                Integer Coeff = Operand.getValue();
                String PrevExpr = SourceExpr;
                SourceExpr = (Coeff==1?"":(Coeff==-1?"-":Coeff.toString())) + Operand.getKey().getString(MapAliases, Syntax);
                if(PrevExpr!=null) {
                    Collection<Source> Sources = new ArrayList();
                    Operand.getKey().fillSources(Sources);
                    Collection<String> IsNulls = new ArrayList();
                    for(Source Source : Sources)
                        IsNulls.add(Source.getKeyString(Source.Keys.iterator().next(),MapAliases.get(Source)));

                    SourceExpr = NullValueSourceExpr.getExpr(IsNulls, PrevExpr, SourceExpr, Syntax);
                }
            }

            return SourceExpr;
        }
    }

    void fillSources(Collection<Source> Sources) {
        for(SourceUnion<K> Operand : Operands.keySet())
            Operand.fillSources(Sources);
    }

    boolean findOperator(int ToFind) {
        if(Operator==ToFind) return true;

        for(SourceUnion<K> Operand : Operands.keySet())
            if(Operand.findOperator(ToFind))
                return true;

        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getDBType() {
        return Operands.keySet().iterator().next().getDBType();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OperationUnion that = (OperationUnion) o;

        if (Operator != that.Operator) return false;
        if (!Operands.equals(that.Operands)) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = Operands.hashCode();
        result = 31 * result + Operator;
        return result;
    }
}

// пока сделаем так что у UnionQuery одинаковые ключи
class UnionQuery<K,V> extends SelectQuery<K,V> {
    LinkedHashMap<Source<K,V>,Integer> Unions = new LinkedHashMap();
    // как в List 0 - MAX, 1 - SUM, 2 - NVL, плюс 3 - если есть в Source
    int Operator;

    UnionQuery(Collection<? extends K> iKeys,int iOperator) {super(iKeys); Operator=iOperator;}

    // "скомпилированные" св-ва
    Map<Source<K,Object>,Map<Object,PropertyUnion<K,Object>>> Operands = new HashMap();
    Map<V,SourceUnion<K>> Operations = new HashMap();

    Map<Object,PropertyUnion<K,Object>> getOperandMap(Source<K,Object> Operand) {
        Map<Object,PropertyUnion<K,Object>> Properties = new HashMap();
        for(Object Property : Operand.getProperties())
            Properties.put(Property,new PropertyUnion<K, Object>(Property,Operand));
        return Properties;
    }

    private void setOperands(Collection<Source<K,Object>> OperandSet) {
        Operands = new HashMap();
        for(Source<K,Object> Operand : OperandSet)
            Operands.put(Operand,getOperandMap(Operand));
    }

    // скомпилированный конструктор
    UnionQuery(Collection<? extends K> iKeys,Collection<Source<K,Object>> iOperands) {
        super(iKeys);
        setOperands(iOperands);
        Compiled = true;
    }

    // Safe - кдючи чисто никаких чисел типа 1 с которыми проблемы
    static String getExpr(LinkedHashMap<String, Integer> Operands, int Operator, boolean Safe, SQLSyntax Syntax) {
        String Result = "";
        for(Map.Entry<String,Integer> Operand : Operands.entrySet()) {
            Integer Coeff = Operand.getValue();
            if(Coeff==null) Coeff = 1;
            String OperandString = (Coeff==1?"":(Coeff==-1?"-":Coeff.toString()));
            if(Operator==1)
                OperandString += Syntax.isNULL(Operand.getKey(),"0", false);
            else
                OperandString += Operand.getKey();

            if(Result.length()==0) {
                Result = OperandString;
            } else {
                switch(Operator) {
                    case 2:
                        if(!OperandString.equals("NULL")) {
                            if(!Safe && !Syntax.isNullSafe())
                                Result = Syntax.isNULL(OperandString,Result,true);
                            else
                                Result = OperandString + "," + Result;
                        }
                        break;
                    case 1:
                        Result = Result+(Coeff>=0?"+":"") + OperandString;
                        break;
                    case 0:
                        if(!OperandString.equals("NULL"))
                            Result = "CASE WHEN "+OperandString+" IS NULL OR "+Result+">"+OperandString+" THEN "+Result+" ELSE "+OperandString+" END";
                        break;
                }
            }
        }

        if(Operands.size()<=1)
            return Result;
        else
        if((Safe || Syntax.isNullSafe()) && Operator==2)
            return "COALESCE(" + Result + ")";    
        else
            return "("+Result+")";
    }

    String fillSelect(Map<K, String> KeySelect, Map<V, String> PropertySelect, Collection<String> WhereSelect, SQLSyntax Syntax) {

        compile();

        Map<K,LinkedHashMap<String,Integer>> UnionKeys = new HashMap();

        Map<Source<K,Object>,String> MapAliases = new HashMap();

        // сначала заполняем ключи
        int AliasCount = 0;
        String From = "";
        for(Source<K, Object> Source : Operands.keySet()) {
            String Alias = "f"+(AliasCount++);
            MapAliases.put(Source,Alias);

            String FromSource = Source.getSource(Syntax) + " " + Alias;

            if(From.length()==0) {
                // первый Union
                for(K Key : Keys) {
                    LinkedHashMap<String,Integer> JoinKeySource = new LinkedHashMap<String,Integer>();
                    JoinKeySource.put(Source.getKeyString(Key,Alias),1);
                    UnionKeys.put(Key,JoinKeySource);
                }
            } else {
                String JoinOn = "";
                for(K Key : Keys) {
                    String KeySource = Source.getKeyString(Key,Alias);
                    LinkedHashMap<String,Integer> JoinKeySource = UnionKeys.get(Key);
                    JoinOn = (JoinOn.length()==0?"":JoinOn+" AND ") + KeySource + "=" + getExpr(JoinKeySource,2,true, Syntax);
                    JoinKeySource.put(KeySource,1);
                }

                FromSource = " FULL JOIN " + FromSource + " ON " + JoinOn;
            }

            From += FromSource;
        }

        // ключи
        for(K Key : Keys)
            KeySelect.put(Key,getExpr(UnionKeys.get(Key),2,true,Syntax));
        
        // ключи надо по NVL(2) делать, выражения по оператору
        // для Operator <=2

        for(Map.Entry<V,SourceUnion<K>> Property : Operations.entrySet())
            PropertySelect.put(Property.getKey(),Property.getValue().getString(MapAliases, Syntax));

/*            Map<V,LinkedHashMap<String,Integer>> UnionValues = null;

            if(Operator<=2)
                UnionValues = new HashMap();
  */
/*            // при 3 нужно для каждого оператора, получить список Source'ов и поставить на OR не IS NULL
            if(Operator<=2) {
                LinkedHashMap<String,Integer> PropertySource = UnionValues.get(Property);
                if(PropertySource==null) {
                    PropertySource = new LinkedHashMap<String,Integer>();
                    UnionValues.put(Property,PropertySource);
                }

                PropertySource.put(Source.getPropertyString(Property, Alias),Coeff);
            } else {
                String SourceExpr = (Coeff==1?"":(Coeff==-1?"-":Coeff.toString())) + Source.getPropertyString(Property,Alias);
                String PrevExpr = PropertySelect.get(Property);
                if(PrevExpr!=null)
                    SourceExpr = "(CASE WHEN " + Source.getKeyString(Keys.iterator().next(),Alias) + " IS NULL "
                        + " THEN " + PrevExpr + " ELSE " + SourceExpr + " END)";
                PropertySelect.put(Property,SourceExpr);
            }
        }

        From += FromSource;

        // свойства
        if(Operator<=2)
            for(Map.Entry<V,LinkedHashMap<String,Integer>> Property : UnionValues.entrySet())
                PropertySelect.put(Property.getKey(),getExpr(Property.getValue(),Operator,false, Syntax));
                    */
        return From;
    }

    JoinQuery<K,V> newJoinQuery(Integer Coeff) {

        JoinQuery<K,V> Query = new JoinQuery<K,V>(Keys);
        Unions.put(Query,Coeff);

        return Query;
    }

    Collection<V> getProperties() {

        if(Compiled)
            return Operations.keySet(); 
        else {
            Set<V> ValueSet = new HashSet();

            for(Source<K,V> Union : Unions.keySet())
                ValueSet.addAll(Union.getProperties());

            return ValueSet;
        }
    }

    String getDBType(V Property) {
        
        if(Compiled)
            return Operations.get(Property).getDBType();
        else
            for(Source<K,V> Union : Unions.keySet())
                if(Union.getProperties().contains(Property))
                    return Union.getDBType(Property);

        return null;
    }


    void compileJoins(Join<K, V> Join, Map<SourceExpr, SourceExpr> Translated, Collection<SourceWhere> JoinWheres, Collection<Join> TranslatedJoins, boolean Outer) {

        compile();
        
        // не раскрываемся если есть оператор 3
        boolean Has3Operator = false;
        for(SourceUnion<K> Property : Operations.values())
            Has3Operator = Has3Operator || Property.findOperator(3);

        // если есть UnionQuery не в Inner Join'ах или же у UnionQuery один операнд то из операндов делаем Join'ы, JoinExpr'ы заменяем на UnionSourceExpr'ы из UnionQuery с проставленными JoinExpr'ами
        Outer = Outer || !Join.Inner;
        if(!Has3Operator && !Join.Exclude && (Outer || Operands.size()==1)) {
            Map<Source<K,Object>,Join<K,?>> OperandJoins = new HashMap();
            // делаем Join'ы
            for(Source<K,Object> Operand : Operands.keySet()) {
                Join<K,Object> JoinOperand = new Join<K,Object>(Operand,Join.Joins,!Outer);
                OperandJoins.put(Operand,JoinOperand);

                Operand.compileJoins(JoinOperand, Translated, JoinWheres, TranslatedJoins, Outer);
//                TranslatedJoins.add(JoinOperand.translate());
            }

            /// все выражений протранслируем
            for(V Property : getProperties())
                Translated.put(Join.Exprs.get(Property),Operations.get(Property).translateExpr(OperandJoins, Translated));
        } else
            super.compileJoins(Join, Translated, JoinWheres, TranslatedJoins, Outer);
    }

    private boolean recMerge(ListIterator<Source<K, Object>> RecJoin, Collection<Source> ToMergeSources, Map<K, ?> MergeKeys, Set<Source<K, Object>> MergedSources, Map<Source, Source> MapSources, Map<Source, Source> MapMergedSources, Map<Source, Map<Object, Object>> MergedSourceProps) {

        if(!RecJoin.hasNext())
            return true;

        Source<K, Object> CurrentSource = RecJoin.next();

        for(Source ToMergeSource : ToMergeSources) {
            if(!MapMergedSources.containsKey(ToMergeSource)) {
                // надо бы проверить что Join уже не merge'ут
                Map<Object,Object> MergeProps = new HashMap();
                Source<K, Object> MergedSource = CurrentSource.merge(ToMergeSource, MergeKeys, MergeProps);
                if(MergedSource!=null) {
                    MergedSources.add(MergedSource);
                    MapSources.put(CurrentSource, MergedSource);
                    MapMergedSources.put(ToMergeSource, MergedSource);
                    MergedSourceProps.put(ToMergeSource, MergeProps);
                    if(recMerge(RecJoin, ToMergeSources, MergeKeys, MergedSources, MapSources, MapMergedSources, MergedSourceProps))
                        return true;
                    MergedSources.remove(MergedSource);
                    MapSources.remove(CurrentSource);
                    MapMergedSources.remove(ToMergeSource);
                    MergedSourceProps.remove(ToMergeSource);
                }
            }
        }

        RecJoin.previous();

        return false;
    }

    Map<PropertyUnion,SourceUnion> getTranslator(UnionQuery<?,?> Result, Map<Source, Source> MapSources) {

        Map<PropertyUnion,SourceUnion> Translated = new HashMap();
        for(Map.Entry<Source<K,Object>,Map<Object,PropertyUnion<K,Object>>> Operand : Operands.entrySet())
            for(Map.Entry<Object,PropertyUnion<K,Object>> UnionProp : Operand.getValue().entrySet())
                Translated.put(UnionProp.getValue(),Result.Operands.get(MapSources.get(Operand.getKey())).get(UnionProp.getKey()));

        return Translated;
    }

    Source<K, Object> merge(Source<?, ?> Merge, Map<K, ?> MergeKeys, Map<Object, Object> MergeProps) {

        if(!(Merge instanceof UnionQuery)) return null;

        UnionQuery MergeUnion = (UnionQuery)Merge;

        // сливаем все Source
        Set<Source<K,Object>> MergedSources = new HashSet();
        Map<Source,Source> MapSources = new HashMap();
        Map<Source,Source> MapMergedSources = new HashMap();
        Map<Source,Map<Object,Object>> MergedSourceProps = new HashMap();
        if(recMerge((new ArrayList<Source<K,Object>>(Operands.keySet())).listIterator(),MergeUnion.Operands.keySet(),MergeKeys, MergedSources, MapSources,MapMergedSources,MergedSourceProps)) {
            // нашли итерацию нужно построить св-ва
            UnionQuery<K,Object> Result = new UnionQuery<K,Object>(Keys,MergedSources);

            // сделаем Map в обратную сторону
            Map<SourceUnion,Object> BackOperations = new HashMap();

            // транслируем старые PropertyUnion на новые
            Map<PropertyUnion,SourceUnion> ToTranslate = getTranslator(Result,MapSources);
            Map<SourceUnion,SourceUnion> Translated = new HashMap();
            for(Map.Entry<V,SourceUnion<K>> MapProp : Operations.entrySet()) {
                SourceUnion TransOperation = MapProp.getValue().translate(ToTranslate,Translated);

                Result.Operations.put(MapProp.getKey(),TransOperation);
                BackOperations.put(TransOperation,MapProp.getKey());
            }

            ToTranslate = MergeUnion.getTranslator(Result,MapMergedSources);
            Translated = new HashMap();
            for(Map.Entry<Object,SourceUnion<Object>> MapProp : ((UnionQuery<Object,Object>)MergeUnion).Operations.entrySet()) {
                SourceUnion TransOperation = MapProp.getValue().translate(ToTranslate,Translated);

                Object PropertyObject = BackOperations.get(TransOperation);
                if(PropertyObject==null) {
                    PropertyObject = new Object();
                    Result.Operations.put(PropertyObject,TransOperation);
                }
                MergeProps.put(MapProp.getKey(),PropertyObject);
            }

            return Result;
        }

        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    void proceedCompile() {
        // нужно заполнить Operands, Operations
        // придется вручную перекастить
        Collection<Source<K,Object>> CastList = new ArrayList<Source<K,Object>>();
        for(Source<K, V> Source : Unions.keySet())
            CastList.add((Source<K,Object>) Source);

        setOperands(CastList);
        Operations = new HashMap();
        for(V Property: getProperties()) {
            OperationUnion<K> Operation = new OperationUnion<K>(Operator);
            // нужно в порядке Unions бежать
            for(Map.Entry<Source<K,V>,Integer> MapUnion : Unions.entrySet()) {
                if(MapUnion.getKey().getProperties().contains(Property))
                    Operation.Operands.put(Operands.get(MapUnion.getKey()).get(Property), MapUnion.getValue());
            }
            Operations.put(Property,Operation);
        }

        // компилируем все Query
        for(Source<K, Object> Operand : Operands.keySet())
            // все SelectQuery компилируем
            if(Operand instanceof Query)
                ((Query)Operand).compile();

//        if(1==1) return;

        // по всем операндам (SelectQuery) компилируем их
        // если есть UnionQuery то доливаем все операнды туда где был старый Source
        Map<PropertyUnion,SourceUnion> ToTranslate = new HashMap();
        for(Source<K, Object> Operand : new ArrayList<Source<K, Object>>(Operands.keySet())) {
            if(Operand instanceof UnionQuery) {
                Map<Object, PropertyUnion<K, Object>> Properties = Operands.remove(Operand);

                UnionQuery<K,Object> UnionOperand = (UnionQuery<K,Object>)Operand;
                Operands.putAll(UnionOperand.Operands);
                // закинем перекодирования (Map'а св-в)
                for(Map.Entry<Object,PropertyUnion<K,Object>> MapProperty : Properties.entrySet())
                    ToTranslate.put(MapProperty.getValue(),UnionOperand.Operations.get(MapProperty.getKey()));
            }
        }

        Map<K,K> MapKeys = new HashMap();
        for(K Key : Keys)
            MapKeys.put(Key,Key);

        // для всех пар JoinQuery если Set<Join'ов> включает (compatible) Set<Join> другого, "доливаем" 1-й Join во 2-й - списки Join'ов объединяем, делаем Include, также заменяем PropExpr'ы и 1-го и 2-го JoinQuery на новые PropExprы
        // пока просто если есть 2 (compatible) Source объединяем заменяем PropExpr'ы

        // слитыве операнды
        Map<Source<K,Object>,Map<Object,PropertyUnion<K,Object>>> MergedOperands = new HashMap();

        while(Operands.size()>0) {
            Iterator<Map.Entry<Source<K, Object>, Map<Object, PropertyUnion<K, Object>>>> itOperand = Operands.entrySet().iterator();
            // берем первый элемент, ишем с кем слить
            Map.Entry<Source<K, Object>, Map<Object, PropertyUnion<K, Object>>> ToMerge = itOperand.next();
            Source<K,Object> ToMergeSource = ToMerge.getKey();
            Map<Object, PropertyUnion<K, Object>> ToMergeProps = ToMerge.getValue();
            itOperand.remove();
        
            while(itOperand.hasNext()) {
                Map<Object,Object> MergeProps = new HashMap();
                Map.Entry<Source<K, Object>, Map<Object, PropertyUnion<K, Object>>> Check = itOperand.next();
                Source<K, Object> MergedSource = ToMergeSource.merge(Check.getKey(), MapKeys, MergeProps);
                if(MergedSource!=null) {
                    Map<Object, PropertyUnion<K, Object>> MergedProperties = getOperandMap(MergedSource);
                    for(Map.Entry<Object,PropertyUnion<K,Object>> MapProperty : ToMergeProps.entrySet())
                        ToTranslate.put(MapProperty.getValue(),MergedProperties.get(MapProperty.getKey()));
                    for(Map.Entry<Object,PropertyUnion<K,Object>> MapProperty : Check.getValue().entrySet())
                        ToTranslate.put(MapProperty.getValue(),MergedProperties.get(MergeProps.get(MapProperty.getKey())));
                    itOperand.remove();

                    ToMergeSource = MergedSource;
                    ToMergeProps = MergedProperties;
                }
            }

            MergedOperands.put(ToMergeSource,ToMergeProps);
        }

        Operands = MergedOperands;
        // транслируем все Operations
        Map<SourceUnion,SourceUnion> Translated = new HashMap();
        for(Map.Entry<V,SourceUnion<K>> Operation : Operations.entrySet())
            Operation.setValue(Operation.getValue().translate(ToTranslate,Translated));
    }
}

// с GroupQuery пока неясно
class GroupQuery<B,K extends B,V extends B> extends Query<K,V> {
    Source<?,B> From; // вообще должен быть или K или V
    Collection<V> Properties = new ArrayList();
    int Operator;

    String getSelect(List<K> KeyOrder, List<V> PropertyOrder, SQLSyntax Syntax) {

        compile();
        
        // ключи не колышат
        Map<B,String> FromPropertySelect = new HashMap();
        Collection<String> WhereSelect = new ArrayList();
        String FromSelect = From.fillSelect(new HashMap(),FromPropertySelect,WhereSelect, Syntax);

        String GroupBy = "";
        Map<K,String> KeySelect = new HashMap();
        Map<V,String> PropertySelect = new HashMap();
        for(K Key : Keys) {
            String KeyExpr = FromPropertySelect.get(Key);
            KeySelect.put(Key,KeyExpr);
            WhereSelect.add("NOT "+KeyExpr+" IS NULL");
            GroupBy = (GroupBy.length()==0?"":GroupBy+",") + KeyExpr;
        }
        for(V Property : Properties)
            PropertySelect.put(Property,(Operator==0?"MAX":"SUM")+"("+FromPropertySelect.get(Property)+")");

        return getSelectString(FromSelect,KeySelect,PropertySelect,WhereSelect,KeyOrder,PropertyOrder) + (GroupBy.length()==0?"":" GROUP BY "+GroupBy);
    }

    Source<K, Object> merge(Source<?, ?> Merge, Map<K, ?> MergeKeys, Map<Object, Object> MergeProps) {
        // если Merge'ся From'ы
        if(!(Merge instanceof GroupQuery)) return null;

        if(1==1) return null;

        GroupQuery MergeGroup = (GroupQuery)Merge;
        if(Keys.size()!=MergeGroup.Keys.size()) return null;
        if(Operator!=MergeGroup.Operator) return null;

        // попробуем смерджить со всеми мапами пока не получим нужный набор ключей
        Collection<Map<Object, Object>> MapSet = (new MapBuilder<Object, Object>()).buildPairs(From.Keys, MergeGroup.From.Keys);
        if(MapSet==null) return null;

        for(Map<Object,Object> MapKeys : MapSet) {
            Map<Object,Object> FromMergeProps = new HashMap();
            Source<Object, Object> MergedFrom = ((Source<Object, Object>) From).merge(MergeGroup.From, MapKeys, FromMergeProps);
            if(MergedFrom!=null) {
                // проверим что ключи совпали
                boolean KeyMatch = true;
                for(Object Key : MergeGroup.Keys)
                    KeyMatch = KeyMatch && Keys.contains(FromMergeProps.get(Key));

                if(KeyMatch) {
                    Collection<Object> MergedProperties = new ArrayList(Properties);
                    for(Object MergeProp : MergeGroup.Properties)
                        MergedProperties.add(FromMergeProps.get(MergeProp));

                    GroupQuery<Object, K, Object> Result = new GroupQuery<Object, K, Object>(Keys, MergedFrom, MergedProperties, Operator);
                    Result.Compiled = true;
                    return Result;
                }
            }
        }

        return null;
    }

    GroupQuery(Collection<? extends K> iKeys,Source<?,B> iFrom,V Property,int iOperator) {
        super(iKeys);
        From = iFrom;
        Properties.add(Property);
        Operator = iOperator;
    }

    GroupQuery(Collection<? extends K> iKeys,Source<?,B> iFrom,Collection<V> iProperties,int iOperator) {
        super(iKeys);
        From = iFrom;
        Properties = iProperties;
        Operator = iOperator;
    }
    
    Collection<V> getProperties() {
        return Properties;
    }

    String getDBType(V Property) {
        return From.getDBType(Property);
    }

    void compileJoins(Join<K, V> Join, Map<SourceExpr, SourceExpr> Translated, Collection<SourceWhere> JoinWheres, Collection<Join> TranslatedJoins, boolean Outer) {

        compile();

        Map<B,Integer> MergeValues = new HashMap();
        Collection<K> CompileKeys = new ArrayList();

        // заполняем статичные значения
        for(K Key : Keys) {
            SourceExpr JoinExpr = Join.Joins.get(Key).translate(Translated);
            if(JoinExpr instanceof ValueSourceExpr && ((ValueSourceExpr)JoinExpr).Value instanceof Integer) {
                MergeValues.put(Key, (Integer) ((ValueSourceExpr)JoinExpr).Value);
                Join.Joins.remove(Key);
            } else
                CompileKeys.add(Key);
        }

        if(MergeValues.size() > 0)
            Join.Source =  new GroupQuery<B,K,V>(CompileKeys,From.mergePropertyValue(MergeValues),Properties,Operator);

        // перетранслируем Joins
        super.compileJoins(Join, Translated, JoinWheres, TranslatedJoins, Outer);
    }

    void proceedCompile() {
        if(From instanceof Query)
            ((Query)From).compile();
    }
}

class ModifyQuery {
    Table Table;
    Source<KeyField,PropertyField> Change;

    ModifyQuery(Table iTable,Source<KeyField,PropertyField> iChange) {
        Table = iTable;
        Change = iChange;
    }

    String getUpdate(SQLSyntax Syntax) {

        Map<KeyField,String> KeySelect = new HashMap();
        Map<PropertyField,String> PropertySelect = new HashMap();
        Collection<String> WhereSelect = new ArrayList();
        String FromSelect = Change.fillSelect(KeySelect,PropertySelect,WhereSelect, Syntax);

        for(KeyField Key : Table.Keys)
            WhereSelect.add(Table.getSource(Syntax)+"."+Key.Name+"="+KeySelect.get(Key));

        if(Syntax.isSelectUpdate()) {
            List<KeyField> KeyOrder = new ArrayList();
            List<PropertyField> PropertyOrder = new ArrayList();
            String SelectString = Change.getSelectString(FromSelect,KeySelect,PropertySelect,WhereSelect,KeyOrder,PropertyOrder);

            String SetString = "";
            for(KeyField Field : KeyOrder) 
                SetString = (SetString.length()==0?"":SetString+",") + Field.Name;
            for(PropertyField Field : PropertyOrder) 
                SetString = (SetString.length()==0?"":SetString+",") + Field.Name;

            return "UPDATE " + Table.getSource(Syntax) + " SET ("+SetString+") = ("+SelectString+") WHERE EXISTS ("+SelectString+")";
        } else {
            String WhereString = "";
            for(String Where : WhereSelect)
                WhereString = (WhereString.length()==0?"":WhereString+" AND ") + Where;

            String SetString = "";
            for(Map.Entry<PropertyField,String> SetProperty : PropertySelect.entrySet())
                SetString = (SetString.length()==0?"":SetString+",") + SetProperty.getKey().Name + "=" + SetProperty.getValue();

            return "UPDATE " + Syntax.getUpdate(Table.getSource(Syntax)," SET "+SetString,FromSelect,(WhereString.length()==0?"":" WHERE "+WhereString));
        }
    }

    String getInsertLeftKeys(SQLSyntax Syntax) {

        // делаем для этого еще один запрос
        JoinQuery<KeyField,PropertyField> LeftKeysQuery = new JoinQuery<KeyField,PropertyField>(Table.Keys);
        // при Join'им ModifyQuery
        LeftKeysQuery.Wheres.add(new JoinWhere(new UniJoin<KeyField,PropertyField>(Change,LeftKeysQuery,true)));
        // исключим ключи которые есть
        LeftKeysQuery.Wheres.add(new ExcludeJoinWhere(new UniJoin<KeyField,PropertyField>(Table,LeftKeysQuery,false)));

        return (new ModifyQuery(Table,LeftKeysQuery)).getInsertSelect(Syntax);
    }

    String getInsertSelect(SQLSyntax Syntax) {

        List<KeyField> KeyOrder = new ArrayList();
        List<PropertyField> PropertyOrder = new ArrayList();
        String SelectString = Change.getSelect(KeyOrder,PropertyOrder,Syntax);

        String InsertString = "";
        for(KeyField KeyField : KeyOrder)
            InsertString = (InsertString.length()==0?"":InsertString+",") + KeyField.Name;
        for(PropertyField PropertyField : PropertyOrder)
            InsertString = (InsertString.length()==0?"":InsertString+",") + PropertyField.Name;

        return "INSERT INTO " + Table.getSource(Syntax) + " (" + InsertString + ") " + SelectString;
    }

    void outSelect(DataSession Session) throws SQLException {
        System.out.println("Table");
        Table.outSelect(Session);
        System.out.println("Source");
        Change.outSelect(Session);
    }
}

