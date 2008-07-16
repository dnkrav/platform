/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platformlocal;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Random;
import java.util.Set;

class TableImplement extends ArrayList<DataPropertyInterface> {
    // заполняются пока автоматически
    Table Table;
    Map<DataPropertyInterface,KeyField> MapFields;

    TableImplement() {
        Childs = new HashSet<TableImplement>();
        Parents = new HashSet<TableImplement>();
    }
    
    // кэшированный граф
    Set<TableImplement> Childs;
    Set<TableImplement> Parents;

    // Operation на что сравниваем
    // 0 - не ToParent
    // 1 - ToParent
    // 2 - равно
    
    boolean RecCompare(int Operation,Collection<DataPropertyInterface> ToCompare,ListIterator<DataPropertyInterface> iRec,Map<DataPropertyInterface,DataPropertyInterface> MapTo) {
        if(!iRec.hasNext()) return true;

        DataPropertyInterface ProceedItem = iRec.next();
        for(DataPropertyInterface PairItem : ToCompare) {
            if((Operation==1 && ProceedItem.Class.IsParent(PairItem.Class) || (Operation==0 && PairItem.Class.IsParent(ProceedItem.Class))) || (Operation==2 && PairItem.Class == ProceedItem.Class)) {
                if(!MapTo.containsKey(PairItem)) {
                    // если parent - есть связь и нету ключа, гоним рекурсию дальше
                    MapTo.put(PairItem, ProceedItem);
                    // если нашли карту выходим
                    if(RecCompare(Operation,ToCompare,iRec,MapTo)) return true;
                    MapTo.remove(PairItem);
                }
            }
        }

        iRec.previous();
        return false;
    }
    // 0 никак не связаны, 1 - параметр снизу в дереве, 2 - параметр сверху в дереве, 3 - равно
    // также возвращает карту если 2
    int Compare(Collection<DataPropertyInterface> ToCompare,Map<KeyField,DataPropertyInterface> MapTo) {
        
        if(ToCompare.size() != size()) return 0;

        // перебором и не будем страдать фигней
        // сначала что не 1 проверим
    
        HashMap<DataPropertyInterface,DataPropertyInterface> MapProceed = new HashMap();
        
        ListIterator<DataPropertyInterface> iRec = (new ArrayList<DataPropertyInterface>(this)).listIterator();
        int Relation = 0;
        if(RecCompare(2,ToCompare,iRec,MapProceed)) Relation = 3;
        if(Relation==0 && RecCompare(0,ToCompare,iRec,MapProceed)) Relation = 2;
        if(Relation>0) {
            if(MapTo!=null) {
                MapTo.clear();
                for(DataPropertyInterface DataInterface : ToCompare)
                    MapTo.put(MapFields.get(MapProceed.get(DataInterface)),DataInterface);
            }
            
            return Relation;
        }

        // MapProceed и так чистый и iRec также в начале
        if(RecCompare(1,ToCompare,iRec,MapProceed)) Relation = 1;
        
        // !!!! должна заполнять MapTo только если уже нашла
        return Relation;
    }

    void RecIncludeIntoGraph(TableImplement IncludeItem,boolean ToAdd,Set<TableImplement> Checks) {
        
        if(Checks.contains(this)) return;
        Checks.add(this);
        
        Iterator<TableImplement> i = Parents.iterator();
        while(i.hasNext()) {
            TableImplement Item = i.next();
            Integer Relation = Item.Compare(IncludeItem,null);
            if(Relation==1) {
                // снизу в дереве
                // добавляем ее как промежуточную
                Item.Childs.add(IncludeItem);
                IncludeItem.Parents.add(Item);
                if(ToAdd) {
                    Item.Childs.remove(this);
                    i.remove();
                }
            } else {
                // сверху в дереве или никак не связаны
                // передаем дальше
                if(Relation!=3) Item.RecIncludeIntoGraph(IncludeItem,Relation==2,Checks);
                if(Relation==2 || Relation==3) ToAdd = false;
            }
        }
        
        // если снизу добавляем Childs
        if(ToAdd) {
            IncludeItem.Childs.add(this);
            Parents.add(IncludeItem);
        }
    }

    Table GetTable(Collection<DataPropertyInterface> FindItem,Map<KeyField,DataPropertyInterface> MapTo) {
        for(TableImplement Item : Parents) {
            int Relation = Item.Compare(FindItem,MapTo);
            if(Relation==2 || Relation==3)
                return Item.GetTable(FindItem,MapTo);
        }
        
        return Table;
    }
    
    void FillSet(Set<TableImplement> TableImplements) {
        if(!TableImplements.add(this)) return;
        for(TableImplement Parent : Parents) Parent.FillSet(TableImplements);
    }

    void OutClasses() {
        for(DataPropertyInterface Interface : this)
            System.out.print(Interface.Class.ID.toString()+" ");
    }
    void Out() {
        //выводим себя
        System.out.print("NODE - ");
        OutClasses();
        System.out.println("");
        
        for(TableImplement Child : Childs) {
            System.out.print("childs - ");
            Child.OutClasses();
            System.out.println();
        }

        for(TableImplement Parent : Parents) {
            System.out.print("parents - ");
            Parent.OutClasses();
            System.out.println();
        }
        
        for(TableImplement Parent : Parents) Parent.Out();
    }
}

// таблица в которой лежат объекты
class ObjectTable extends Table {
    
    KeyField Key;
    Field Class;
    
    ObjectTable() {
        super("objects");
        Key = new KeyField("object","integer");
        KeyFields.add(Key);
        Class = new Field("class","integer");
        PropFields.add(Class);
    };
    
    FromTable ClassSelect(Class ToSelect) {
        Collection<Integer> SetID = new HashSet<Integer>();
        ToSelect.FillSetID(SetID);
        
        FromTable ClassTable = new FromTable(Name);
        ClassTable.Wheres.add(new FieldSetValueWhere(SetID,Class.Name));
        
        return ClassTable;
    }
    
    FromTable ClassJoinSelect(Class ToSelect,SourceExpr JoinImplement) {
        FromTable JoinTable = ClassSelect(ToSelect);
        JoinTable.Wheres.add(new FieldWhere(JoinImplement,Key.Name));
        return JoinTable;
    }
    
    Integer GetClassID(DataAdapter Adapter,Integer idObject) throws SQLException {
        SelectQuery Query = new SelectQuery(new FromTable(Name));
        Query.From.Wheres.add(new FieldValueWhere(idObject,Key.Name));
        Query.Expressions.put("classid",new FieldSourceExpr(Query.From,Class.Name));
        return (Integer)Adapter.ExecuteSelect(Query).get(0).get("classid");
    }
  
}

// таблица счетчика ID
class IDTable extends Table {
    KeyField Key;
    
    IDTable() {
        super("idtable");
        Key = new KeyField("lastid","integer");
        KeyFields.add(Key);
    }
    
    Integer GenerateID(DataAdapter Adapter) throws SQLException { 
        FromTable From = new FromTable(Name);
        SelectQuery SelectID = new SelectQuery(From);
        // читаем
        SelectID.Expressions.put("lastid",new FieldSourceExpr(From,Key.Name));
        Integer FreeID = (Integer)Adapter.ExecuteSelect(SelectID).get(0).get("lastid");
        // замещаем
        SelectID.Expressions.put("lastid",new ValueSourceExpr(FreeID+1));
        Adapter.UpdateRecords(SelectID);
        return FreeID;
    }
}

// таблица куда виды складывают свои объекты
class ViewTable extends Table {
    ViewTable(Integer iObjects) {
        super("viewtable"+iObjects.toString());
        Objects = new ArrayList();
        for(Integer i=0;i<iObjects;i++) {
            KeyField ObjKeyField = new KeyField("object"+i,"integer");
            Objects.add(ObjKeyField);
            KeyFields.add(ObjKeyField);
        }
        
        View = new KeyField("viewid","integer");
        KeyFields.add(View);
    }
            
    List<KeyField> Objects;
    KeyField View;
    
    void DropViewID(DataAdapter Adapter,Integer ViewID) throws SQLException {
        FromTable Delete = new FromTable(Name);
        Delete.Wheres.add(new FieldValueWhere(ViewID,View.Name));
        Adapter.DeleteRecords(Delete);
    }
}

class ChangeTable extends Table {

    Collection<KeyField> Objects;
    KeyField Session;
    KeyField Property;
    Field Value;
    Field PrevValue;
    // системное поля, по сути для MaxGroupProperty
    Field SysValue;
    
    ChangeTable(Integer iObjects,Integer iDBType,List<String> DBTypes) {
        super("changetable"+iObjects+"t"+iDBType);

        Objects = new ArrayList();
        for(Integer i=0;i<iObjects;i++) {
            KeyField ObjKeyField = new KeyField("object"+i,"integer");
            Objects.add(ObjKeyField);
            KeyFields.add(ObjKeyField);
        }
        
        Session = new KeyField("session","integer");
        KeyFields.add(Session);
        
        Property = new KeyField("property","integer");
        KeyFields.add(Property);
        
        Value = new Field("value",DBTypes.get(iDBType));
        PropFields.add(Value);

        PrevValue = new Field("prevvalue",DBTypes.get(iDBType));
        PropFields.add(PrevValue);

        SysValue = new Field("sysvalue",DBTypes.get(iDBType));
        PropFields.add(SysValue);
    }
}

class TableFactory extends TableImplement{
    
    ObjectTable ObjectTable;
    IDTable IDTable;
    List<ViewTable> ViewTables;
    List<List<ChangeTable>> ChangeTables;
    
    // для отладки
    boolean ReCalculateAggr = false;
    
    ChangeTable GetChangeTable(Integer Objects, String DBType) {
        return ChangeTables.get(Objects-1).get(DBTypes.indexOf(DBType));
    }

    List<String> DBTypes;
    
    TableFactory() {
        ObjectTable = new ObjectTable();
        IDTable = new IDTable();
        ViewTables = new ArrayList();
        ChangeTables = new ArrayList();
        
        for(int i=1;i<5;i++)
            ViewTables.add(new ViewTable(i));

        DBTypes = new ArrayList();
        DBTypes.add("integer");
        DBTypes.add("char(50)");
        
        for(int i=1;i<5;i++) {
            List<ChangeTable> ObjChangeTables = new ArrayList();
            ChangeTables.add(ObjChangeTables);
            for(int j=0;j<DBTypes.size();j++)
                ObjChangeTables.add(new ChangeTable(i,j,DBTypes));
        }
    }

    void IncludeIntoGraph(TableImplement IncludeItem) {
        Set<TableImplement> Checks = new HashSet<TableImplement>();
        RecIncludeIntoGraph(IncludeItem,true,Checks);
    }
    
    void FillDB(DataAdapter Adapter) throws SQLException {
        Integer TableNum = 0;
        Set<TableImplement> TableImplements = new HashSet<TableImplement>();
        FillSet(TableImplements);
        
        for(TableImplement Node : TableImplements) {
            TableNum++;
            Node.Table = new Table("table"+TableNum.toString());
            Node.MapFields = new HashMap<DataPropertyInterface,KeyField>();
            Integer FieldNum = 0;
            for(DataPropertyInterface Interface : Node) {
                FieldNum++;
                KeyField Field = new KeyField("key"+FieldNum.toString(),"integer");
                Node.Table.KeyFields.add(Field);
                Node.MapFields.put(Interface,Field);
            }
        }
        
        Adapter.CreateTable(ObjectTable);
        Adapter.CreateTable(IDTable);
        for(ViewTable ViewTable : ViewTables) Adapter.CreateTable(ViewTable);

        Iterator<List<ChangeTable>> ilc = ChangeTables.iterator();
        for(List<ChangeTable> ListTables : ChangeTables)
            for(ChangeTable ChangeTable : ListTables) Adapter.CreateTable(ChangeTable);

        // закинем одну запись
        Map<KeyField,Integer> InsertKeys = new HashMap<KeyField,Integer>();
        InsertKeys.put(IDTable.Key, 0);
        Adapter.InsertRecord(IDTable,InsertKeys,new HashMap<Field,Object>());
    }
}

class BusinessLogics {
    
    BusinessLogics() {
        BaseClass = new ObjectClass(0);
        TableFactory = new TableFactory();
        Properties = new ArrayList();
        PersistentProperties = new HashSet();
        
        BaseClass = new ObjectClass(0);
        
        StringClass = new StringClass(1);
//        StringClass.AddParent(BaseClass);
        IntegerClass = new QuantityClass(2);
        IntegerClass.AddParent(BaseClass);
    }
    
    void AddDataProperty(DataProperty Property) {
        Properties.add(Property);
    }

    // получает класс по ID объекта
    Class GetClass(DataAdapter Adapter,Integer idObject) throws SQLException {
        // сначала получаем idClass
        return BaseClass.FindClassID(TableFactory.ObjectTable.GetClassID(Adapter,idObject));
    }

    Class BaseClass;
    Class StringClass;
    IntegralClass IntegerClass;
    
    TableFactory TableFactory;
    Collection<Property> Properties;
    
    Set<AggregateProperty> PersistentProperties;
    
        
    void UpdateAggregations(DataAdapter Adapter,Collection<AggregateProperty> Properties, ChangesSession Session) throws SQLException {
        // мн-во св-в constraints/persistent или все св-ва формы (то есть произвольное)
        
        // нужно из графа зависимостей выделить направленный список аггрегированных св-в (здесь из предположения что список запрашиваемых аггрегаций меньше общего во много раз)
        List<AggregateProperty> UpdateList = new ArrayList();
        for(AggregateProperty Property : Properties) Property.FillAggregateList(UpdateList,Session.Properties);
        
        // здесь бежим слева направо определяем изм. InterfaceClassSet (в DataProperty они первичны) - удаляем сразу те у кого null (правда это может убить всю ветку)
        // потом реализуем

        // пробежим вперед пометим свойства которые изменились, но неясно на что
        ListIterator<AggregateProperty> il = UpdateList.listIterator();
        AggregateProperty Property = null;
        while(il.hasNext()) { 
            Property = il.next();
            Property.SessionChanged.put(Session,null);
        }
        // пробежим по которым надо поставим 0
        for(AggregateProperty AggrProperty : Properties) AggrProperty.SetChangeType(Session,0);
        // прогоним DataProperty также им 0 поставим чтобы 1 не появлялись
        for(DataProperty DataProperty : Session.Properties) DataProperty.SetChangeType(Session,0);

        // бежим по списку (в обратном порядке) заполняем требования, 
        while(Property!=null) {
            Property.FillRequiredChanges(Session);

            if(il.hasPrevious())
                Property = il.previous();
            else
                Property = null;
        }
        
        // прогоним DataProperty предыдущие значения докинуть
        for(DataProperty DataProperty : Session.Properties) DataProperty.UpdateIncrementChanges(Adapter,Session);
        
        // запускаем IncrementChanges для этого списка
        for(AggregateProperty AggrProperty : UpdateList) AggrProperty.IncrementChanges(Adapter,Session);
        
        // дропнем изменения (пока, потом для FormBean'ов понадобится по другому)
        for(AggregateProperty AggrProperty : UpdateList) AggrProperty.SessionChanged.remove(Session);
    }
    
    // сохраняет из Changes в базу
    void SaveProperties(DataAdapter Adapter,Collection<? extends ObjectProperty> Properties, ChangesSession Session) throws SQLException {
        for(ObjectProperty Property : Properties) Property.SaveChanges(Adapter, Session);
    }
    
    boolean Apply(DataAdapter Adapter,ChangesSession Session) throws SQLException {
        // делается UpdateAggregations (для мн-ва persistent+constraints)
        UpdateAggregations(Adapter,PersistentProperties,Session);

        // записываем Data, затем PersistentProperties в таблицы из сессии
        SaveProperties(Adapter,PersistentProperties,Session);
        SaveProperties(Adapter,Session.Properties,Session);
        
        return true;
    }

    void FillDB(DataAdapter Adapter) throws SQLException {
        // инициализируем таблицы
        TableFactory.FillDB(Adapter);

        // запишем ID'ки
        int IDPropNum = 0;
        for(Property Property : Properties)
            Property.ID = IDPropNum++;
        
        Set<DataProperty> DataProperties = new HashSet();
        Collection<AggregateProperty> AggrProperties = new ArrayList();
        Map<Table,Integer> Tables = new HashMap<Table,Integer>();
        // закинем в таблицы(создав там все что надо) св-ва
        for(Property Property : Properties) {
            // ChangeTable'ы заполним
            if(Property instanceof ObjectProperty)
                ((ObjectProperty)Property).FillChangeTable();;

            if(Property instanceof DataProperty)
                DataProperties.add((DataProperty)Property);
            
            if(Property instanceof AggregateProperty)
                AggrProperties.add((AggregateProperty)Property);

            if(Property instanceof DataProperty || (Property instanceof AggregateProperty && PersistentProperties.contains(Property))) {
                Table Table = ((ObjectProperty)Property).GetTable(null);
  
                Integer PropNum = Tables.get(Table);
                if(PropNum==null) PropNum = 1;
                PropNum = PropNum + 1;
                Tables.put(Table, PropNum);

                Field PropField = new Field("prop"+PropNum.toString(),Property.GetDBType());
                Table.PropFields.add(PropField);
                ((ObjectProperty)Property).Field = PropField;
            }
        }

        for(Table Table : Tables.keySet()) Adapter.CreateTable(Table);

        // построим в нужном порядке AggregateProperty и будем заполнять их
        List<AggregateProperty> UpdateList = new ArrayList();
        for(AggregateProperty Property : AggrProperties) Property.FillAggregateList(UpdateList,DataProperties);
        Integer ViewNum = 0;
        for(AggregateProperty Property : UpdateList) {
            if(Property instanceof GroupProperty)
                ((GroupProperty)Property).FillDB(Adapter,ViewNum++);
        }
        
        // создадим dumb
        Table DumbTable = new Table("dumb");
        DumbTable.KeyFields.add(new KeyField("dumb","integer"));
        Adapter.CreateTable(DumbTable);
        Adapter.Execute("INSERT INTO dumb (dumb) VALUES (1)");
    }
    
    void CheckPersistent(DataAdapter Adapter) throws SQLException {
        for(AggregateProperty Property : PersistentProperties)
            Property.CheckAggregation(Adapter,Property.OutName);
    }

    // функционал по заполнению св-в по номерам, нужен для BL
    
    LDP AddDProp(Class Value,Class ...Params) {
        DataProperty Property = new DataProperty(TableFactory,Value);
        LDP ListProperty = new LDP(Property);
        for(Class Int : Params) {
            ListProperty.AddInterface(Int);
        }
        AddDataProperty(Property);
        return ListProperty;
    }

    LSFP AddSFProp(String Formula,Integer Params) {
        StringFormulaProperty Property = new StringFormulaProperty(Formula);
        LSFP ListProperty = new LSFP(Property,IntegerClass,Params);
        Properties.add(Property);
        return ListProperty;
    }

    LMFP AddMFProp(Integer Params) {
        MultiplyFormulaProperty Property = new MultiplyFormulaProperty();
        LMFP ListProperty = new LMFP(Property,IntegerClass,Params);
        Properties.add(Property);
        return ListProperty;
    }

    
    List<PropertyInterfaceImplement> ReadPropImpl(LP MainProp,Object ...Params) {
        List<PropertyInterfaceImplement> Result = new ArrayList<PropertyInterfaceImplement>();
        int WaitInterfaces = 0, MainInt = 0;
        PropertyMapImplement MapRead = null;
        LP PropRead = null;
        for(Object P : Params) {
            if(P instanceof Integer) {
                // число может быть как ссылкой на родной интерфейс так и 
                PropertyInterface PropInt = MainProp.ListInterfaces.get((Integer)P-1);
                if(WaitInterfaces==0) {
                    // родную берем 
                    Result.add(PropInt);
                } else {
                    // докидываем в маппинг
                    MapRead.Mapping.put(PropRead.ListInterfaces.get(PropRead.ListInterfaces.size()-WaitInterfaces), PropInt);
                    WaitInterfaces--;
                }
            } else {
               // имплементация, типа LP
               PropRead = (LP)P;
               MapRead = new PropertyMapImplement((ObjectProperty)PropRead.Property);
               WaitInterfaces = PropRead.ListInterfaces.size();
               Result.add(MapRead);
            }
        }
        
        return Result;
    }

    LRP AddRProp(LP MainProp, int IntNum, Object ...Params) {
        RelationProperty Property = new RelationProperty(TableFactory,MainProp.Property);
        LRP ListProperty = new LRP(Property,IntNum);
        int MainInt = 0;
        List<PropertyInterfaceImplement> PropImpl = ReadPropImpl(ListProperty,Params);
        for(PropertyInterfaceImplement Implement : PropImpl) {
            Property.Implements.Mapping.put(MainProp.ListInterfaces.get(MainInt),Implement);
            MainInt++;
        }
        Properties.add(Property);

        return ListProperty;
    }
    
    LGP AddGProp(LP GroupProp,boolean Sum,Object ...Params) {
        GroupProperty Property = null;
        if(Sum)
            Property = new SumGroupProperty(TableFactory,(ObjectProperty)GroupProp.Property);
        else
            Property = new MaxGroupProperty(TableFactory,(ObjectProperty)GroupProp.Property);
        LGP ListProperty = new LGP(Property,GroupProp);
        List<PropertyInterfaceImplement> PropImpl = ReadPropImpl(GroupProp,Params);
        for(PropertyInterfaceImplement Implement : PropImpl) ListProperty.AddInterface(Implement);
        Properties.add(Property);
        
        return ListProperty;
    }

    LRP AddLProp(int ListType, int IntNum, Object ...Params) {
        ListProperty Property = null;
        switch(ListType) {
            case 0:
                Property = new MaxListProperty(TableFactory);
                break;
            case 1:
                Property = new SumListProperty(TableFactory);
                break;
            case 2:
                Property = new OverrideListProperty(TableFactory);
                break;
        }
        
        LRP ListProperty = new LRP(Property,IntNum);

        for(int i=0;i<IntNum;i++) {
            Integer Offs = i*(IntNum+2);
            LP OpImplement = (LP)Params[Offs+1];
            PropertyMapImplement Operand = new PropertyMapImplement((ObjectProperty)OpImplement.Property);
            for(int j=0;j<IntNum;j++)
                Operand.Mapping.put(OpImplement.ListInterfaces.get(((Integer)Params[Offs+2+j])-1),ListProperty.ListInterfaces.get(j));
            Property.Operands.add(Operand);
            Property.Coeffs.put(Operand,(Integer)Params[Offs]);
        }
        Properties.add(Property);

        return ListProperty;
    }

    void RegGClass(LGP GroupProp,Object ...iParams) {
        int iInt=0;
        boolean bInt=true;
        for(Object i : iParams) {
            if(bInt) {
                iInt = (Integer)i-1;
                bInt = false;
            } else {
                ((GroupProperty)GroupProp.Property).ToClasses.put(GroupProp.GroupProperty.ListInterfaces.get(iInt),(Class)i);
                bInt = true;
            }
        }        
    }
    
    int MaxInterface = 4;
    
    // генерирует белую БЛ
    void OpenTest(boolean Properties,boolean Implements,boolean Persistent) {

        Class Base = new ObjectClass(3);
        Base.AddParent(BaseClass);
        Class Article = new ObjectClass(4);
        Article.AddParent(Base);
        Class Store = new ObjectClass(5);
        Store.AddParent(Base);
        Class Document = new ObjectClass(6);
        Document.AddParent(Base);
        Class PrihDocument = new ObjectClass(7);
        PrihDocument.AddParent(Document);
        Class RashDocument = new ObjectClass(8);
        RashDocument.AddParent(Document);
        Class ArticleGroup = new ObjectClass(9);
        ArticleGroup.AddParent(Base);
        Class Supplier = new ObjectClass(10);
        Supplier.AddParent(Base);
        
        if(Properties) {
            LDP Name = AddDProp(StringClass,Base);
            LDP DocStore = AddDProp(Store,Document);
            LDP Quantity = AddDProp(IntegerClass,Document,Article);
            LDP PrihQuantity = AddDProp(IntegerClass,PrihDocument,Article);
            LDP RashQuantity = AddDProp(IntegerClass,RashDocument,Article);
            LDP ArtToGroup = AddDProp(ArticleGroup,Article);
            LDP DocDate = AddDProp(IntegerClass,Document);
            LDP ArtSupplier = AddDProp(Supplier,Article,Store);
            LDP PriceSupp = AddDProp(IntegerClass,Article,Supplier);

            LSFP Dirihle = AddSFProp("(CASE WHEN prm1<prm2 THEN 1 ELSE 0 END)",2);
            LMFP Multiply = AddMFProp(2);

            Name.Property.OutName = "имя";
            DocStore.Property.OutName = "склад";
            Quantity.Property.OutName = "кол-во";
            PrihQuantity.Property.OutName = "кол-во прих.";
            RashQuantity.Property.OutName = "кол-во расх.";
            ArtToGroup.Property.OutName = "гр. тов";
            DocDate.Property.OutName = "дата док.";
            ArtSupplier.Property.OutName = "тек. пост.";
            PriceSupp.Property.OutName = "цена пост.";

            LRP OstPrice = AddRProp(PriceSupp,2,1,ArtSupplier,1,2);
            OstPrice.Property.OutName = "цена на складе";

            LRP StoreName = AddRProp(Name,1,DocStore,1);
            StoreName.Property.OutName = "имя склада";
            LRP ArtGroupName = AddRProp(Name,1,ArtToGroup,1);
            ArtGroupName.Property.OutName = "имя гр. тов.";

            LRP DDep = AddRProp(Dirihle,2,DocDate,1,DocDate,2);
            DDep.Property.OutName = "предш. док.";

            LRP QDep = AddRProp(Multiply,3,DDep,1,2,Quantity,1,3);
            QDep.Property.OutName = "изм. баланса";

            LGP GSum = AddGProp(QDep,true,2,3);
            GSum.Property.OutName = "остаток до операции";

            LGP GP = AddGProp(Quantity,true,DocStore,1,2);
            GP.Property.OutName = "сумм кол-во док. тов.";
            LGP GAP = AddGProp(GP,true,2);
            GAP.Property.OutName = "сумм кол-во тов.";
            LGP G2P = AddGProp(Quantity,true,DocStore,1,ArtToGroup,2);
            G2P.Property.OutName = "скл-гр. тов";

            LGP PrihArtStore = AddGProp(PrihQuantity,true,DocStore,1,2);
            PrihArtStore.Property.OutName = "приход по складу";

            LGP RashArtStore = AddGProp(RashQuantity,true,DocStore,1,2);
            RashArtStore.Property.OutName = "расход по складу";

            LRP OstArtStore = AddLProp(1,2,1,PrihArtStore,1,2,-1,RashArtStore,1,2);
            OstArtStore.Property.OutName = "остаток по складу";

            LGP OstArt = AddGProp(OstArtStore,true,2);
            OstArt.Property.OutName = "остаток по товару";
            
            LGP MaxPrih = AddGProp(PrihQuantity,false,DocStore,1,ArtToGroup,2);
            MaxPrih.Property.OutName = "макс. приход по гр. тов.";

            LRP MaxOpStore = AddLProp(0,2,1,PrihArtStore,1,2,1,RashArtStore,1,2);
            MaxOpStore.Property.OutName = "макс. операция";
        
            LGP SumMaxArt = AddGProp(MaxOpStore,true,2);
            SumMaxArt.Property.OutName = "сумма макс. операция";
            
            if(Persistent) {
/*                PersistentProperties.add((AggregateProperty)GP.Property);
                PersistentProperties.add((AggregateProperty)GAP.Property);
                PersistentProperties.add((AggregateProperty)G2P.Property);
                PersistentProperties.add((AggregateProperty)GSum.Property);
                PersistentProperties.add((AggregateProperty)OstArtStore.Property);
                PersistentProperties.add((AggregateProperty)OstArt.Property);
                PersistentProperties.add((AggregateProperty)MaxPrih.Property);
                PersistentProperties.add((AggregateProperty)MaxOpStore.Property);
                PersistentProperties.add((AggregateProperty)SumMaxArt.Property);*/
                PersistentProperties.add((AggregateProperty)OstPrice.Property);
            }
        }
        
        TableImplement Include;
        for(int i=0;i<MaxInterface;i++) {
            Include = new TableImplement();
            for(int j=0;j<=i;j++)
                Include.add(new DataPropertyInterface(BaseClass));
            TableFactory.IncludeIntoGraph(Include);
        }            

        if(Implements) {
            Include = new TableImplement();
            Include.add(new DataPropertyInterface(Article));
            TableFactory.IncludeIntoGraph(Include);
            Include = new TableImplement();
            Include.add(new DataPropertyInterface(Store));
            TableFactory.IncludeIntoGraph(Include);
            Include = new TableImplement();
            Include.add(new DataPropertyInterface(ArticleGroup));
            TableFactory.IncludeIntoGraph(Include);
            Include = new TableImplement();
            Include.add(new DataPropertyInterface(Article));
            Include.add(new DataPropertyInterface(Document));
            TableFactory.IncludeIntoGraph(Include);
            Include = new TableImplement();
            Include.add(new DataPropertyInterface(Article));
            Include.add(new DataPropertyInterface(Store));
            TableFactory.IncludeIntoGraph(Include);
        }
    }
    
    // случайным образом генерирует классы
    void RandomClasses(Random Randomizer) {
        int CustomClasses = 1;//
    }

    // случайным образом генерирует св-ва
    void RandomProperties(Random Randomizer) {
        
        List<Class> Classes = new ArrayList();
        BaseClass.FillClassList(Classes);
        
        List<Property> RandProps = new ArrayList();
        List<ObjectProperty> RandObjProps = new ArrayList();
        
        StringFormulaProperty Dirihle = new StringFormulaProperty("(CASE WHEN prm1<prm2 THEN 1 ELSE 0 END)");
        Dirihle.Interfaces.add(new StringFormulaPropertyInterface(BaseClass,"prm1"));
        Dirihle.Interfaces.add(new StringFormulaPropertyInterface(BaseClass,"prm2"));
        RandProps.add(Dirihle);

        MultiplyFormulaProperty Multiply = new MultiplyFormulaProperty();
        Multiply.Interfaces.add(new FormulaPropertyInterface(BaseClass));
        Multiply.Interfaces.add(new FormulaPropertyInterface(BaseClass));
        RandProps.add(Multiply);

        int DataPropCount = Randomizer.nextInt(20)+1;
        for(int i=0;i<DataPropCount;i++) {
            // DataProperty
            DataProperty DataProp = new DataProperty(TableFactory,Classes.get(Randomizer.nextInt(Classes.size())));
            // генерируем классы
            int IntCount = Randomizer.nextInt(MaxInterface)+1;
            for(int j=0;j<IntCount;j++)
                DataProp.Interfaces.add(new DataPropertyInterface(Classes.get(Randomizer.nextInt(Classes.size()))));

            RandProps.add(DataProp);
            RandObjProps.add(DataProp);
        }

        System.out.print("Создание аггрег. св-в ");
                
        int PropCount = Randomizer.nextInt(1000)+1;
        for(int i=0;i<PropCount;i++) {
//            int RandClass = Randomizer.nextInt(10);
//            int PropClass = (RandClass>7?0:(RandClass==8?1:2));
            int PropClass = Randomizer.nextInt(6);
//            int PropClass = 5;
            ObjectProperty GenProp = null;
            String ResType = "";
            if(PropClass==0) {
                // RelationProperty
                RelationProperty RelProp = new RelationProperty(TableFactory,RandProps.get(Randomizer.nextInt(RandProps.size())));
                
                // генерируем случайно кол-во интерфейсов
                List<PropertyInterface> RelPropInt = new ArrayList();
                int IntCount = Randomizer.nextInt(MaxInterface)+1;
                for(int j=0;j<IntCount;j++) {
                    PropertyInterface Interface = new PropertyInterface();
                    RelProp.Interfaces.add(Interface);
                    RelPropInt.add(Interface);
                }
                
                // чтобы 2 раза на одну и ту же ветку не натыкаться
                List<PropertyInterface> AvailRelInt = new ArrayList(RelPropInt);
                boolean Correct = true;
                
                for(PropertyInterface Interface : (Collection<PropertyInterface>)RelProp.Implements.Property.Interfaces) {
                    // генерируем случайно map'ы на эти интерфейсы
                    if(RelProp.Implements.Property instanceof ObjectProperty && Randomizer.nextBoolean()) {
                        if(AvailRelInt.size()==0) {
                            Correct = false;
                            break;
                        }
                        PropertyInterface MapInterface = AvailRelInt.get(Randomizer.nextInt(AvailRelInt.size()));
                        RelProp.Implements.Mapping.put(Interface,MapInterface);
                        AvailRelInt.remove(MapInterface);
                    } else {
                        // другое property пока сгенерим на 1
                        PropertyMapImplement ImpProp = new PropertyMapImplement(RandObjProps.get(Randomizer.nextInt(RandObjProps.size())));
                        if(ImpProp.Property.Interfaces.size()>RelPropInt.size()) {
                            Correct = false;
                            break;
                        }
                        
                        List<PropertyInterface> MapRelInt = new ArrayList(RelPropInt);
                        for(PropertyInterface ImpInterface : (Collection<PropertyInterface>)ImpProp.Property.Interfaces) {
                            PropertyInterface MapInterface = MapRelInt.get(Randomizer.nextInt(MapRelInt.size()));
                            ImpProp.Mapping.put(ImpInterface,MapInterface);
                            MapRelInt.remove(MapInterface);
                        }
                        RelProp.Implements.Mapping.put(Interface,ImpProp);
                    }
                }

                if(Correct) {
                    GenProp = RelProp;
                    ResType = "R";
                }
            }
            
            if(PropClass==1 || PropClass==2) {
                // группировочное
                ObjectProperty GroupProp = RandObjProps.get(Randomizer.nextInt(RandObjProps.size()));
                GroupProperty Property = null;
                if(PropClass==1) {
                    Property = new SumGroupProperty(TableFactory,GroupProp);
                    ResType = "SG";
                } else {
                    Property = new MaxGroupProperty(TableFactory,GroupProp);
                    ResType = "MG";
                }
                
                boolean Correct = true;                
                List<PropertyInterface> GroupInt = new ArrayList(GroupProp.Interfaces);
                int GroupCount = Randomizer.nextInt(MaxInterface)+1;
                for(int j=0;j<GroupCount;j++) {
                    PropertyInterfaceImplement Implement;
                    // генерируем случайно map'ы на эти интерфейсы
                    if(Randomizer.nextBoolean()) {
                        Implement = GroupInt.get(Randomizer.nextInt(GroupInt.size()));
                    } else {
                        // другое property пока сгенерим на 1
                        PropertyMapImplement ImpProp = new PropertyMapImplement(RandObjProps.get(Randomizer.nextInt(RandObjProps.size())));
                        if(ImpProp.Property.Interfaces.size()>GroupInt.size()) {
                            Correct = false;
                            break;
                        }

                        List<PropertyInterface> MapRelInt = new ArrayList(GroupInt);
                        for(PropertyInterface ImpInterface : (Collection<PropertyInterface>)ImpProp.Property.Interfaces) {
                            PropertyInterface MapInterface = MapRelInt.get(Randomizer.nextInt(MapRelInt.size()));
                            ImpProp.Mapping.put(ImpInterface,MapInterface);
                            MapRelInt.remove(MapInterface);
                        }
                        Implement = ImpProp;
                    }
                    
                    Property.Interfaces.add(new GroupPropertyInterface(Implement));
                }
                
                if(Correct)
                    GenProp = Property;
            }

            if(PropClass==3 || PropClass==4 || PropClass==5) {
                ListProperty Property = null;
                if(PropClass==3) {
                    Property = new SumListProperty(TableFactory);
                    ResType = "SL";
                } else {
                if(PropClass==4) {
                    Property = new MaxListProperty(TableFactory);
                    ResType = "ML";
                } else {
                    Property = new OverrideListProperty(TableFactory);
                    ResType = "OL";
                }
                }
                
                int OpIntCount = Randomizer.nextInt(MaxInterface)+1;
                for(int j=0;j<OpIntCount;j++)
                    Property.Interfaces.add(new PropertyInterface());
        
                boolean Correct = true;
                List<PropertyInterface> OpInt = new ArrayList(Property.Interfaces);
                int OpCount = Randomizer.nextInt(4)+1;
                for(int j=0;j<OpCount;j++) {
                    PropertyMapImplement Operand = new PropertyMapImplement(RandObjProps.get(Randomizer.nextInt(RandObjProps.size())));
                    if(Operand.Property.Interfaces.size()!=OpInt.size()) {
                        Correct = false;
                        break;
                    }

                    List<PropertyInterface> MapRelInt = new ArrayList(OpInt);
                    for(PropertyInterface ImpInterface : (Collection<PropertyInterface>)Operand.Property.Interfaces) {
                        PropertyInterface MapInterface = MapRelInt.get(Randomizer.nextInt(MapRelInt.size()));
                        Operand.Mapping.put(ImpInterface,MapInterface);
                        MapRelInt.remove(MapInterface);
                    }
                    Property.Operands.add(Operand);
                }
                
                if(Correct)
                    GenProp = Property;
            }
                       

            if(GenProp!=null) {
                // проверим что есть в интерфейсе и покрыты все ключи
                Iterator<InterfaceClass> ic = GenProp.GetClassSet(null).iterator();
                if(ic.hasNext() && ic.next().keySet().size()==GenProp.Interfaces.size()) {
                    System.out.print(ResType+"-");
                    RandProps.add(GenProp);
                    RandObjProps.add(GenProp);
                }
            }
        }
        
        Properties.addAll(RandProps);
        
        System.out.println();
    }
    
    // случайным образом генерирует имплементацию
    void RandomImplement(Random Randomizer) {
        List<Class> Classes = new ArrayList();
        BaseClass.FillClassList(Classes);

        // заполнение физ модели
        int ImplementCount = Randomizer.nextInt(8);
        for(int i=0;i<ImplementCount;i++) {
            TableImplement Include = new TableImplement();
            int ObjCount = Randomizer.nextInt(3)+1;
            for(int ioc=0;ioc<ObjCount;ioc++)
                Include.add(new DataPropertyInterface(Classes.get(Randomizer.nextInt(Classes.size()))));
            TableFactory.IncludeIntoGraph(Include);               
        }        
    }
    
    // случайным образом генерирует постоянные аггрегации
    void RandomPersistent(Random Randomizer) {
        
        // сначала список получим
        List<AggregateProperty> AggrProperties = new ArrayList();
        for(Property Property : Properties) {
            if(Property instanceof AggregateProperty)
                AggrProperties.add((AggregateProperty)Property);
        }
        
        int PersistentNum = Randomizer.nextInt(AggrProperties.size())+1;
        for(int i=0;i<PersistentNum;i++)
            PersistentProperties.add(AggrProperties.get(Randomizer.nextInt(AggrProperties.size())));
    }
    
    void FullDBTest()  throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        
        // сгенерить классы
        // сгенерить св-ва
        // сгенерить физ. модель
        // сгенерить persistent аггрегации
        OpenTest(false,false,false);

        Random Randomizer = new Random();
        Randomizer.setSeed(1000);


        while(true) {
            RandomProperties(Randomizer);
            
            RandomImplement(Randomizer);

            RandomPersistent(Randomizer);

            DataAdapter Adapter = new DataAdapter();
            Adapter.Connect("");

            FillDB(Adapter);

            // сгенерить объекты (их пока тестить не надо)
            List<Class> Classes = new ArrayList();
            BaseClass.FillClassList(Classes);
            for(Class ObjectClass : Classes)
                for(int j=0;j<6;j++)
                    ObjectClass.AddObject(Adapter,TableFactory);

            // запустить ChangeDBTest
            ChangeDBTest(Adapter,20,Randomizer);

            Adapter.Disconnect();
        }
    }
    
    
    void ChangeDBTest(DataAdapter Adapter,Integer MaxIterations,Random Randomizer) throws SQLException {
        
        // сначала список получим
        List<DataProperty> DataProperties = new ArrayList();
        for(Property Property : Properties) {
            if(Property instanceof DataProperty)
                DataProperties.add((DataProperty)Property);
        }
        
//        Randomizer.setSeed(1);
        int Iterations = 2;
        while(Iterations<MaxIterations) {
            ChangesSession Session = new ChangesSession(Iterations++);
            System.out.println(Iterations);

            int PropertiesChanged = Randomizer.nextInt(10)+1;
            for(int ip=0;ip<PropertiesChanged;ip++) {
                // берем случайные n св-в
                DataProperty ChangeProp = DataProperties.get(Randomizer.nextInt(DataProperties.size()));
                int NumChanges = Randomizer.nextInt(20)+1;
                for(int in=0;in<NumChanges;in++) {
                    Object ValueObject = ChangeProp.Value.GetRandomObject(Adapter,TableFactory,Randomizer,Iterations);
/*                    // теперь определяем класс найденного объекта
                    Class ValueClass = null;
                    if(ChangeProp.Value instanceof ObjectClass)
                        ValueClass = BaseClass.FindClassID(ValueObject);
                    else
                        ValueClass = ChangeProp.Value;*/
                        
                    InterfaceClassSet InterfaceSet = ChangeProp.GetClassSet(null);
                    // определяем входные классы
                    InterfaceClass Classes = InterfaceSet.get(Randomizer.nextInt(InterfaceSet.size()));
                    // генерим рандомные объекты этих классов
                    Map<DataPropertyInterface,Integer> Keys = new HashMap();
                    for(DataPropertyInterface Interface : ChangeProp.Interfaces)
                        Keys.put(Interface,(Integer)Classes.get(Interface).GetRandomObject(Adapter,TableFactory,Randomizer,0));
                    
                    ChangeProp.ChangeProperty(Adapter,Keys,ValueObject,Session);
                }
            }
            
            Apply(Adapter,Session);
            CheckPersistent(Adapter);
        }
    }
}
