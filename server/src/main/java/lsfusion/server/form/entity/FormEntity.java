package lsfusion.server.form.entity;

import lsfusion.base.*;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.LongMutable;
import lsfusion.base.col.interfaces.mutable.MCol;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.add.MAddSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.interop.FormEventType;
import lsfusion.interop.ModalityType;
import lsfusion.interop.PropertyEditType;
import lsfusion.interop.form.ServerResponse;
import lsfusion.server.caches.IdentityInstanceLazy;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.classes.LogicalClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.form.entity.filter.FilterEntity;
import lsfusion.server.form.entity.filter.RegularFilterEntity;
import lsfusion.server.form.entity.filter.RegularFilterGroupEntity;
import lsfusion.server.form.stat.StaticDataGenerator;
import lsfusion.server.form.view.ComponentView;
import lsfusion.server.form.view.DefaultFormView;
import lsfusion.server.form.view.FormView;
import lsfusion.server.form.view.PropertyDrawView;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.CanonicalNameUtils;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.debug.DebugInfo;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.logics.mutables.FindIndex;
import lsfusion.server.logics.mutables.NFFact;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.mutables.interfaces.*;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.actions.flow.ChangeFlowType;
import lsfusion.server.logics.property.group.AbstractNode;
import lsfusion.server.session.DataSession;
import org.apache.log4j.Logger;

import java.util.*;

public class FormEntity implements FormSelector<ObjectEntity> {
    private final static Logger logger = Logger.getLogger(FormEntity.class);
    
    public static Boolean DEFAULT_NOCANCEL = null;

    public static final IsFullClientFormulaProperty isFullClient = IsFullClientFormulaProperty.instance;
    public static final IsDebugFormulaProperty isDebug = IsDebugFormulaProperty.instance;
    public static final SessionDataProperty isFloat = new SessionDataProperty(LocalizedString.create("Is dialog"), LogicalClass.instance);
    public static final SessionDataProperty isSync = new SessionDataProperty(LocalizedString.create("Is modal"), LogicalClass.instance);
    public static final SessionDataProperty isAdd = new SessionDataProperty(LocalizedString.create("Is add"), LogicalClass.instance);
    public static final SessionDataProperty manageSession = new SessionDataProperty(LocalizedString.create("Manage session"), LogicalClass.instance);
    public static final SessionDataProperty showDrop = new SessionDataProperty(LocalizedString.create("Show drop"), LogicalClass.instance);

    public PropertyDrawEntity printActionPropertyDraw;
    public PropertyDrawEntity editActionPropertyDraw;
    public PropertyDrawEntity xlsActionPropertyDraw;
    public PropertyDrawEntity dropActionPropertyDraw;
    public PropertyDrawEntity refreshActionPropertyDraw;
    public PropertyDrawEntity applyActionPropertyDraw;
    public PropertyDrawEntity cancelActionPropertyDraw;
    public PropertyDrawEntity okActionPropertyDraw;
    public PropertyDrawEntity closeActionPropertyDraw;

    private int ID;
    
    private String canonicalName;
    private LocalizedString caption;
    private DebugInfo.DebugPoint debugPoint; 

    private String defaultImagePath;
    
    public NFMapList<Object, ActionPropertyObjectEntity<?>> eventActions = NFFact.mapList();
    public ImMap<Object, ImList<ActionPropertyObjectEntity<?>>> getEventActions() {
        return eventActions.getOrderMap();
    }
    public Iterable<ActionPropertyObjectEntity<?>> getEventActionsListIt(Object eventObject) {
        return eventActions.getListIt(eventObject);
    }

    private NFOrderSet<GroupObjectEntity> groups = NFFact.orderSet(true); // для script'ов, findObjectEntity в FORM / EMAIL objects
    public Iterable<GroupObjectEntity> getGroupsIt() {
        return groups.getIt();
    }
    public Iterable<GroupObjectEntity> getNFGroupsIt(Version version) { // не finalized
        return groups.getNFIt(version);
    }
    public ImOrderSet<GroupObjectEntity> getGroupsList() {
        return groups.getOrderSet(); 
    }
    public Iterable<GroupObjectEntity> getNFGroupsListIt(Version version) {
        return groups.getNFListIt(version);
    }
    
    private NFSet<TreeGroupEntity> treeGroups = NFFact.set();
    public Iterable<TreeGroupEntity> getTreeGroupsIt() {
        return treeGroups.getIt();
    }
    public Iterable<TreeGroupEntity> getNFTreeGroupsIt(Version version) { // предполагается все с одной версией, равной текущей (конструирование FormView)
        return treeGroups.getNFIt(version);
    }    
    
    private NFOrderSet<PropertyDrawEntity> propertyDraws = NFFact.orderSet();
    public Iterable<PropertyDrawEntity> getPropertyDrawsIt() {
        return propertyDraws.getIt();
    }
    public Iterable<PropertyDrawEntity> getNFPropertyDrawsIt(Version version) {
        return propertyDraws.getNFIt(version);
    }
    public ImList<PropertyDrawEntity> getPropertyDrawsList() {
        return propertyDraws.getOrderSet();        
    }
    public Iterable<PropertyDrawEntity> getPropertyDrawsListIt() {
        return propertyDraws.getListIt();        
    }
    public Iterable<PropertyDrawEntity> getNFPropertyDrawsListIt(Version version) { // предполагается все с одной версией, равной текущей (конструирование FormView)
        return propertyDraws.getNFListIt(version);
    }
    
    private NFSet<FilterEntity> fixedFilters = NFFact.set();
    public ImSet<FilterEntity> getFixedFilters() {
        return fixedFilters.getSet();
    }
    
    private NFOrderSet<RegularFilterGroupEntity> regularFilterGroups = NFFact.orderSet();
    public Iterable<RegularFilterGroupEntity> getRegularFilterGroupsIt() {
        return regularFilterGroups.getIt();
    }
    public ImList<RegularFilterGroupEntity> getRegularFilterGroupsList() {
        return regularFilterGroups.getList();
    }
    public Iterable<RegularFilterGroupEntity> getNFRegularFilterGroupsIt(Version version) {
        return regularFilterGroups.getNFIt(version);        
    }
    public Iterable<RegularFilterGroupEntity> getNFRegularFilterGroupsListIt(Version version) { // предполагается все с одной версией, равной текущей (конструирование FormView)
        return regularFilterGroups.getNFListIt(version);
    }

    private NFOrderMap<PropertyDrawEntity<?>,Boolean> defaultOrders = NFFact.orderMap();
    public ImOrderMap<PropertyDrawEntity<?>,Boolean> getDefaultOrdersList() {
        return defaultOrders.getListMap();
    }
    public Boolean getNFDefaultOrder(PropertyDrawEntity<?> entity, Version version) {
        return defaultOrders.getNFValue(entity, version);
    }
    
    private NFOrderMap<OrderEntity<?>,Boolean> fixedOrders = NFFact.orderMap();
    public ImOrderMap<OrderEntity<?>,Boolean> getFixedOrdersList() {
        return fixedOrders.getListMap();
    }

    public ModalityType modalityType = ModalityType.DOCKED;
    public int autoRefresh = 0;

    public CalcPropertyObjectEntity<?> reportPathProp;

    protected FormEntity(String canonicalName, LocalizedString caption, Version version) {
        this(canonicalName, null, caption, null, version);
    }

    public FormEntity(String canonicalName, DebugInfo.DebugPoint debugPoint, LocalizedString caption, String imagePath, Version version) {
        this.ID = BaseLogicsModule.generateStaticNewID();
        this.caption = caption;
        this.canonicalName = canonicalName;
        this.debugPoint = debugPoint;
        
        this.defaultImagePath = imagePath;
        
        logger.debug("Initializing form " + ThreadLocalContext.localize(caption) + "...");

        BaseLogicsModule baseLM = ThreadLocalContext.getBusinessLogics().LM;

        LAP<PropertyInterface> formOk = baseLM.getFormOk();
        LAP<PropertyInterface> formClose = baseLM.getFormClose();

        editActionPropertyDraw = addPropertyDraw(baseLM.getFormEditReport(), version);
        refreshActionPropertyDraw = addPropertyDraw(baseLM.getFormRefresh(), version);
        applyActionPropertyDraw = addPropertyDraw(baseLM.getFormApply(), version);
        cancelActionPropertyDraw = addPropertyDraw(baseLM.getFormCancel(), version);
        okActionPropertyDraw = addPropertyDraw(formOk, version);
        closeActionPropertyDraw = addPropertyDraw(formClose, version);
        dropActionPropertyDraw = addPropertyDraw(baseLM.getFormDrop(), version);
        
        addActionsOnEvent(FormEventType.QUERYOK, true, version, new ActionPropertyObjectEntity<>(formOk.property, MapFact.<PropertyInterface, ObjectEntity>EMPTYREV()));
        addActionsOnEvent(FormEventType.QUERYCLOSE, true, version, new ActionPropertyObjectEntity<>(formClose.property, MapFact.<PropertyInterface, ObjectEntity>EMPTYREV()));
    }

    public void finalizeInit(Version version) {
//        getNFRichDesign(version);
        setRichDesign(createDefaultRichDesign(version), version);
    }

    public void addFixedFilter(FilterEntity filter, Version version) {
        fixedFilters.add(filter, version);
    }

    public void addFixedOrder(OrderEntity order, boolean descending, Version version) {
        fixedOrders.add(order, descending, version);
    }

    public void addRegularFilterGroup(RegularFilterGroupEntity group, Version version) {
        regularFilterGroups.add(group, version);
        FormView richDesign = getNFRichDesign(version);
        if (richDesign != null)
            richDesign.addRegularFilterGroup(group, version);
    }
    
    public void addRegularFilter(RegularFilterGroupEntity filterGroup, RegularFilterEntity filter, boolean isDefault, Version version) {
        filterGroup.addFilter(filter, isDefault, version);
        
        FormView richDesign = getNFRichDesign(version);
        if (richDesign != null)
            richDesign.addRegularFilter(filterGroup, filter, version);
    }

    public int genID() {
        return BaseLogicsModule.generateStaticNewID();
    }

    public GroupObjectEntity getGroupObject(int id) {
        for (GroupObjectEntity group : getGroupsIt()) {
            if (group.getID() == id) {
                return group;
            }
        }

        return null;
    }

    public GroupObjectEntity getGroupObject(String sID) {
        for (GroupObjectEntity group : getGroupsIt()) {
            if (group.getSID().equals(sID)) {
                return group;
            }
        }
        return null;
    }

    public GroupObjectEntity getGroupObjectIntegration(String sID) {
        for (GroupObjectEntity group : getGroupsIt()) {
            if (group.getIntegrationSID().equals(sID)) {
                return group;
            }
        }
        return null;
    }

    public GroupObjectEntity getNFGroupObject(String sID, Version version) {
        for (GroupObjectEntity group : getNFGroupsIt(version)) {
            if (group.getSID().equals(sID)) {
                return group;
            }
        }
        return null;
    }

    public TreeGroupEntity getTreeGroup(int id) {
        for (TreeGroupEntity treeGroup : getTreeGroupsIt()) {
            if (treeGroup.getID() == id) {
                return treeGroup;
            }
        }

        return null;
    }

    public ObjectEntity getObject(int id) {
        for (GroupObjectEntity group : getGroupsIt()) {
            for (ObjectEntity object : group.getObjects()) {
                if (object.getID() == id) {
                    return object;
                }
            }
        }
        return null;
    }

    public ObjectEntity getObject(String sid) {
        for (GroupObjectEntity group : getGroupsIt()) {
            for (ObjectEntity object : group.getObjects()) {
                if (object.getSID().equals(sid)) {
                    return object;
                }
            }
        }
        return null;
    }

    public ObjectEntity getNFObject(String sid, Version version) {
        for (GroupObjectEntity group : getNFGroupsIt(version)) {
            for (ObjectEntity object : group.getObjects()) {
                if (object.getSID().equals(sid)) {
                    return object;
                }
            }
        }
        return null;
    }

    public ObjectEntity getNFObject(ValueClass cls, Version version) {
        for (GroupObjectEntity group : getNFGroupsListIt(version)) { // для детерменированности
            for (ObjectEntity object : group.getObjects()) {
                if (cls.equals(object.baseClass)) {
                    return object;
                }
            }
        }
        return null;
    }

    public List<String> getNFObjectsNamesAndClasses(List<ValueClass> classes, Version version) {
        List<String> names = new ArrayList<>();
        classes.clear();
        
        for (GroupObjectEntity group : getNFGroupsIt(version)) {
            for (ObjectEntity object : group.getObjects()) {
                names.add(object.getSID());
                classes.add(object.baseClass);
            }
        }
        return names;
    }

    public RegularFilterGroupEntity getRegularFilterGroup(int id) {
        for (RegularFilterGroupEntity filterGroup : getRegularFilterGroupsIt()) {
            if (filterGroup.getID() == id) {
                return filterGroup;
            }
        }

        return null;
    }

    public RegularFilterGroupEntity getNFRegularFilterGroup(String sid, Version version) {
        if (sid == null) {
            return null;
        }

        for (RegularFilterGroupEntity filterGroup : getNFRegularFilterGroupsIt(version)) {
            if (sid.equals(filterGroup.getSID())) {
                return filterGroup;
            }
        }

        return null;
    }

    @IdentityLazy
    public ImMap<GroupObjectEntity, ImSet<FilterEntity>> getGroupFixedFilters(final ImSet<GroupObjectEntity> excludeGroupObjects) {
        return getFixedFilters().group(new BaseUtils.Group<GroupObjectEntity, FilterEntity>() {
            @Override
            public GroupObjectEntity group(FilterEntity key) {
                GroupObjectEntity groupObject = key.getApplyObject(FormEntity.this, excludeGroupObjects);
                if (groupObject == null) 
                    return GroupObjectEntity.NULL;
                return groupObject;
            }
        });
    }
    
    @IdentityLazy
    public ImMap<GroupObjectEntity, ImOrderSet<PropertyDrawEntity>> getGroupProperties(final ImSet<GroupObjectEntity> excludeGroupObjects) {
        return ((ImOrderSet<PropertyDrawEntity>)getPropertyDrawsList()).filterOrder(new SFunctionSet<PropertyDrawEntity>() {
            public boolean contains(PropertyDrawEntity element) {
                return element.isCalcProperty();
            }
        }).groupOrder(new BaseUtils.Group<GroupObjectEntity, PropertyDrawEntity>() {
            public GroupObjectEntity group(PropertyDrawEntity key) {
                GroupObjectEntity applyObject = key.getApplyObject(FormEntity.this, excludeGroupObjects);
                return applyObject == null ? GroupObjectEntity.NULL : applyObject;
            }});
    }

    @IdentityLazy
    public ImMap<GroupObjectEntity, ImOrderMap<OrderEntity, Boolean>> getGroupOrdersList(final ImSet<GroupObjectEntity> excludeGroupObjects) {
        return BaseUtils.immutableCast(getDefaultOrdersList().mapOrderKeyValues(new GetValue<OrderEntity<?>, PropertyDrawEntity<?>>() {
            public OrderEntity<?> getMapValue(PropertyDrawEntity<?> value) {
                return value.getOrder();
            }
        }, new GetValue<Boolean, Boolean>() {
            public Boolean getMapValue(Boolean value) {
                return !value;
            }}).mergeOrder(getFixedOrdersList()).groupOrder(new BaseUtils.Group<GroupObjectEntity, OrderEntity<?>>() {
            @Override
            public GroupObjectEntity group(OrderEntity<?> key) {
                GroupObjectEntity groupObject = key.getApplyObject(FormEntity.this, excludeGroupObjects);
                if(groupObject == null)
                    return GroupObjectEntity.NULL;
                return groupObject;
            }
        }));
    }

    public RegularFilterEntity getRegularFilter(int id) {
        for (RegularFilterGroupEntity filterGroup : getRegularFilterGroupsIt()) {
            for (RegularFilterEntity filter : filterGroup.getFiltersList()) {
                if (filter.getID() == id) {
                    return filter;
                }
            }
        }

        return null;
    }

    @IdentityLazy
    public boolean hasNoChange() {
        for (PropertyDrawEntity property : getPropertyDrawsIt()) {
            ActionPropertyObjectEntity<?> editAction = property.getEditAction(ServerResponse.CHANGE, null); // in theory it is possible to support securityPolicy, but in this case we have to drag it through hasFlow + do some complex caching 
            if (editAction != null && editAction.property.hasFlow(ChangeFlowType.FORMCHANGE) && !editAction.property.endsWithApplyAndNoChangesAfterBreaksBefore())
                return false;
        }

        return true;
    }

    public ObjectEntity addSingleGroupObject(ValueClass baseClass, Version version, Object... groups) {
        GroupObjectEntity groupObject = new GroupObjectEntity(genID(), (TreeGroupEntity) null);
        ObjectEntity object = new ObjectEntity(genID(), baseClass, baseClass.getCaption());
        groupObject.add(object);
        addGroupObject(groupObject, version);

        addPropertyDraw(groups, false, version, object);

        return object;
    }

    public TreeGroupEntity addTreeGroupObject(TreeGroupEntity treeGroup, GroupObjectEntity neighbour, boolean isRightNeighbour, String sID, Version version, GroupObjectEntity... tGroups) {
        if (sID != null)
            treeGroup.setSID(sID);
        for (GroupObjectEntity group : tGroups) {
            if(!groups.containsNF(group, version))
                groups.add(group, version);
            treeGroup.add(group);
        }

        treeGroups.add(treeGroup, version);

        FormView richDesign = getNFRichDesign(version);
        if (richDesign != null)
            richDesign.addTreeGroup(treeGroup, neighbour, isRightNeighbour, version);

        return treeGroup;
    }

    public void addGroupObject(GroupObjectEntity group, GroupObjectEntity neighbour, Boolean isRightNeighbour, Version version) {
        for (GroupObjectEntity groupOld : getNFGroupsIt(version)) {
            assert group.getID() != groupOld.getID() && !group.getSID().equals(groupOld.getSID());
            for (ObjectEntity obj : group.getObjects()) {
                for (ObjectEntity objOld : groupOld.getObjects()) {
                    assert obj.getID() != objOld.getID() && !obj.getSID().equals(objOld.getSID());
                }
            }
        }
        if (neighbour != null) {
            groups.addIfNotExistsToThenLast(group, neighbour, isRightNeighbour != null && isRightNeighbour, version);
        } else {
            groups.add(group, version);    
        }

        FormView richDesign = getNFRichDesign(version);
        if (richDesign != null) {
            richDesign.addGroupObject(group, neighbour, isRightNeighbour, version);
        }
    }

    public void addGroupObject(GroupObjectEntity group, Version version) {
        addGroupObject(group, null, null, version);
    }

    public void addPropertyDraw(ObjectEntity object, Version version, Object... groups) {
        addPropertyDraw(groups, false, version, object);
    }

    public void addPropertyDraw(Object[] groups, boolean useObjSubsets, Version version, ObjectEntity object) {

        for (int i = 0; i < groups.length; i++) {

            Object group = groups[i];
            if (group instanceof Boolean) {
//                continue;
            } else if (group instanceof AbstractNode) {
                boolean upClasses = false;
                if ((i + 1) < groups.length && groups[i + 1] instanceof Boolean) {
                    upClasses = (Boolean) groups[i + 1];
                }
                addPropertyDraw((AbstractNode) group, upClasses, useObjSubsets, version, object);
            } else if (group instanceof LP) {
                this.addPropertyDraw((LP) group, version, object);
            } else if (group instanceof LP[]) {
                this.addPropertyDraw((LP[]) group, version, object);
            }
        }
    }

    public List<PropertyDrawEntity> addPropertyDraw(AbstractNode group, boolean upClasses, Version version, ObjectEntity object) {
        return addPropertyDraw(group, upClasses, null, false, version, object);
    }

    public void addPropertyDraw(AbstractNode group, boolean upClasses, boolean useObjSubsets, Version version, ObjectEntity object) {
        addPropertyDraw(group, false, upClasses, null, useObjSubsets, version, object);
    }

    protected List<PropertyDrawEntity> addPropertyDraw(AbstractNode group, boolean upClasses, GroupObjectEntity groupObject, boolean useObjSubsets, Version version, ObjectEntity object) {
        return addPropertyDraw(group, false, upClasses, groupObject, useObjSubsets, version, object);
    }

    protected List<PropertyDrawEntity> addPropertyDraw(AbstractNode group, boolean prev, boolean upClasses, GroupObjectEntity groupObject, boolean useObjSubsets, Version version, ObjectEntity object) {
        return addPropertyDraw(group, prev, upClasses, groupObject, useObjSubsets, version, SetFact.singletonOrder(object));
    }
    
    protected List<PropertyDrawEntity> addPropertyDraw(AbstractNode group, boolean prev, boolean upClasses, GroupObjectEntity groupObject, boolean useObjSubsets, Version version, ImOrderSet<ObjectEntity> objects) {
        ImRevMap<ObjectEntity, ValueClassWrapper> objectToClass = objects.getSet().mapRevValues(new GetValue<ValueClassWrapper, ObjectEntity>() {
            public ValueClassWrapper getMapValue(ObjectEntity value) {
                return new ValueClassWrapper(value.baseClass);
            }
        });
        ImSet<ValueClassWrapper> valueClasses = objectToClass.valuesSet();

        List<PropertyDrawEntity> propertyDraws = new ArrayList<>();

        ImOrderSet<ValueClassWrapper> orderInterfaces = objects.mapOrder(objectToClass);
        for (PropertyClassImplement implement : group.getProperties(valueClasses, useObjSubsets, upClasses, version)) {
            ImSet<ValueClassWrapper> wrapers = implement.mapping.valuesSet();
            ImOrderSet<ObjectEntity> filterObjects = objects.filterOrderIncl(objectToClass.filterValuesRev(wrapers).keys());
            propertyDraws.add(addPropertyDraw(implement.createLP(orderInterfaces.filterOrderIncl(wrapers), prev), groupObject, version, filterObjects));
        }

        return propertyDraws;
    }

    public static ImCol<ImSet<ValueClassWrapper>> getSubsets(ImSet<ValueClassWrapper> valueClasses, boolean useObjSubsets) {
        if(!useObjSubsets)
            return SetFact.singleton(valueClasses);
            
        ImCol<ImSet<ValueClassWrapper>> classSubsets;MCol<ImSet<ValueClassWrapper>> mClassSubsets = ListFact.mCol();
        for (ImSet<ValueClassWrapper> set : new Subsets<>(valueClasses)) {
            if (!set.isEmpty()) {
                mClassSubsets.add(set);
            }
        }
        classSubsets = mClassSubsets.immutableCol();
        return classSubsets;
    }

    public PropertyDrawEntity addPropertyDraw(LP property, Version version, ImOrderSet<ObjectEntity> objects) {
        return addPropertyDraw(property, null, version, objects);
    }
    public PropertyDrawEntity addPropertyDraw(LP property, Version version, ObjectEntity object) {
        return addPropertyDraw(property, version, SetFact.singletonOrder(object));
    }
    public PropertyDrawEntity addPropertyDraw(LP property, Version version) {
        return addPropertyDraw(property, version, SetFact.<ObjectEntity>EMPTYORDER());
    }

    public void addPropertyDraw(LP[] properties, Version version, ObjectEntity object) {
        for (LP property : properties) {
            addPropertyDraw(property, version, object);
        }
    }

    public <P extends PropertyInterface> PropertyDrawEntity addPropertyDraw(LP<P, ?> property, GroupObjectEntity groupObject, Version version, ImOrderSet<ObjectEntity> objects) {
        return addPropertyDraw(groupObject, property.createObjectEntity(objects), null, property.listInterfaces, version);
    }

    public GroupObjectEntity getNFApplyObject(ImSet<ObjectEntity> objects, Version version) {
        GroupObjectEntity result = null;
        for (GroupObjectEntity group : getNFGroupsListIt(version)) {
            for (ObjectEntity object : group.getObjects()) {
                if (objects.contains(object)) {
                    result = group;
                    break;
                }
            }
        }
        return result;
    }

    public GroupObjectEntity getApplyObject(ImSet<ObjectEntity> objects) {
        return getApplyObject(objects, SetFact.<GroupObjectEntity>EMPTY());
    }
    public GroupObjectEntity getApplyObject(ImSet<ObjectEntity> objects, ImSet<GroupObjectEntity> excludeGroupObjects) {
        GroupObjectEntity result = null;
        for (GroupObjectEntity group : getGroupsList()) {
            if(!excludeGroupObjects.contains(group)) {
                for (ObjectEntity object : group.getObjects()) {
                    if (objects.contains(object)) {
                        result = group;
                        break;
                    }
                }
            }
        }
        return result;
    }

    public <I extends PropertyInterface, P extends Property<I>> PropertyDrawEntity<I> addPropertyDraw(P property, ImRevMap<I, ObjectEntity> mapping, Version version) {
        PropertyObjectEntity<I, ?> entity = PropertyObjectEntity.create(property, mapping, null, null);
        return addPropertyDraw(null, entity, null, entity.property.getReflectionOrderInterfaces(), version);
    }

    public <P extends PropertyInterface> PropertyDrawEntity<P> addPropertyDraw(GroupObjectEntity groupObject, PropertyObjectEntity<P, ?> propertyImplement, String formPath, ImOrderSet<P> interfaces, Version version) {
        String propertySID = null;
        if (propertyImplement.property.isNamed()) 
            propertySID = PropertyDrawEntity.createSID(propertyImplement, interfaces);
        return addPropertyDraw(groupObject, propertyImplement, formPath, propertySID, null, version);        
    }
    public <P extends PropertyInterface> PropertyDrawEntity<P> addPropertyDraw(GroupObjectEntity groupObject, PropertyObjectEntity<P, ?> propertyImplement, String formPath, String propertySID, Property inheritedProperty, Version version) {
        final PropertyDrawEntity<P> newPropertyDraw = new PropertyDrawEntity<>(genID(), propertyImplement, groupObject);

        if(inheritedProperty == null)
            inheritedProperty = propertyImplement.property;
        inheritedProperty.drawOptions.proceedDefaultDraw(newPropertyDraw, this);
        newPropertyDraw.group = inheritedProperty.getNFParent(version);

        if (propertySID != null) {
            newPropertyDraw.setSID(propertySID);
            
            newPropertyDraw.setIntegrationSID(inheritedProperty.getName());
        }

        propertyDraws.add(newPropertyDraw, new FindIndex<PropertyDrawEntity>() {
            public int getIndex(List<PropertyDrawEntity> list) {
                int ind = list.size() - 1;
                if (!newPropertyDraw.shouldBeLast) {
                    while (ind >= 0) {
                        PropertyDrawEntity property = list.get(ind);
                        if (!property.shouldBeLast) {
                            break;
                        }
                        --ind;
                    }
                }
                return ind + 1;
            }
        }, version);
        newPropertyDraw.setFormPath(formPath);
        return newPropertyDraw;
    }

    public PropertyDrawView addPropertyDrawView(PropertyDrawEntity propertyDraw, Version version) {
        FormView richDesign = getNFRichDesign(version);
        if (richDesign != null) {
            return richDesign.addPropertyDraw(propertyDraw, version);
        }
        return null;
    }

    public void movePropertyDrawTo(PropertyDrawEntity property, PropertyDrawEntity newNeighbour, boolean isRightNeighbour, Version version) {
        propertyDraws.move(property, newNeighbour, isRightNeighbour, version);

        FormView richDesign = getNFRichDesign(version);
        if (richDesign != null) {
            richDesign.movePropertyDrawTo(property, newNeighbour, isRightNeighbour, version);
        }
    }

    public <P extends PropertyInterface> CalcPropertyObjectEntity addPropertyObject(LCP<P> property, ImOrderSet<ObjectEntity> objects) {
        return addPropertyObject(property, property.getRevMap(objects));
    }
    public <P extends PropertyInterface> CalcPropertyObjectEntity addPropertyObject(LCP<P> property) {
        return addPropertyObject(property, MapFact.<P, ObjectEntity>EMPTYREV());
    }
    public <P extends PropertyInterface> ActionPropertyObjectEntity<P> addPropertyObject(LAP<P> property, ImOrderSet<ObjectEntity> objects) {
        return addPropertyObject(property, property.getRevMap(objects));
    }

    public <P extends PropertyInterface> CalcPropertyObjectEntity addPropertyObject(LCP<P> property, ImRevMap<P, ObjectEntity> objects) {
        return new CalcPropertyObjectEntity<>(property.property, objects, property.getCreationScript(), property.getCreationPath());
    }
    public <P extends PropertyInterface> ActionPropertyObjectEntity<P> addPropertyObject(LAP<P> property, ImRevMap<P, ObjectEntity> objects) {
        return new ActionPropertyObjectEntity<>(property.property, objects, property.getCreationScript(), property.getCreationPath());
    }
    
    public <P extends PropertyInterface> CalcPropertyObjectEntity addPropertyObject(CalcPropertyRevImplement<P, ObjectEntity> impl) {
        return addPropertyObject(impl.property, impl.mapping);
    }
    public <P extends PropertyInterface> CalcPropertyObjectEntity<P> addPropertyObject(CalcProperty<P> property, ImRevMap<P, ObjectEntity> objects) {
        return new CalcPropertyObjectEntity<>(property, objects);
    }

    public PropertyDrawEntity<?> getPropertyDraw(int iID) {
        for (PropertyDrawEntity propertyDraw : getPropertyDrawsIt()) {
            if (propertyDraw.getID() == iID) {
                return propertyDraw;
            }
        }

        return null;
    }

    public PropertyDrawEntity<?> getPropertyDraw(String sid, Version version) {
        if (sid == null) {
            return null;
        }
        for (PropertyDrawEntity propertyDraw : getNFPropertyDrawsIt(version)) {
            if (sid.equals(propertyDraw.getSID())) {
                return propertyDraw;
            }
        }

        return null;
    }

    public PropertyDrawEntity<?> getPropertyDrawIntegration(String sid, Version version) {
        if (sid == null) {
            return null;
        }
        for (PropertyDrawEntity propertyDraw : getNFPropertyDrawsIt(version)) {
            if (sid.equals(propertyDraw.getIntegrationSID())) {
                return propertyDraw;
            }
        }

        return null;
    }

    public boolean noClasses() {
        return false;
    }

    public static class AlreadyDefined extends Exception {
        public final String formCanonicalName;
        public final String newSID;
        public final String formPath;

        public AlreadyDefined(String formCanonicalName, String newSID, String formPath) {
            this.formCanonicalName = formCanonicalName;
            this.newSID = newSID;
            this.formPath = formPath;
        }
    }

    public void setFinalPropertyDrawSID(PropertyDrawEntity property, String alias) throws AlreadyDefined {
        String newSID = (alias == null ? property.getSID() : alias);
        property.setSID(null);
        PropertyDrawEntity drawEntity;
        if ((drawEntity = getPropertyDraw(newSID, Version.CURRENT)) != null) {
            throw new AlreadyDefined(getCanonicalName(), newSID, drawEntity.getFormPath());
        }
        property.setSID(newSID);

        String newIntegrationSID = (alias == null ? property.getIntegrationSID() : alias);
        property.setIntegrationSID(null);
        if ((drawEntity = getPropertyDrawIntegration(newIntegrationSID, Version.CURRENT)) != null) {
            throw new AlreadyDefined(getCanonicalName(), newIntegrationSID, drawEntity.getFormPath());
        }
        property.setIntegrationSID(newIntegrationSID);
    }


    public PropertyDrawEntity<?> getPropertyDraw(String name, List<String> mapping, Version version) {
        return getPropertyDraw(PropertyDrawEntity.createSID(name, mapping), version);
    }

    private NFSet<CalcProperty> hintsIncrementTable = NFFact.set();
    @LongMutable
    public ImSet<CalcProperty> getHintsIncrementTable() {
        return hintsIncrementTable.getSet();
    }

    public void addHintsIncrementTable(Version version, LCP... props) {
        for (LP prop : props) {
            hintsIncrementTable.add((CalcProperty) prop.property, version);
        }
    }

    public void addHintsIncrementTable(Version version, CalcProperty... props) {
        for (CalcProperty prop : props) {
            hintsIncrementTable.add(prop, version);
        }
    }

    private NFSet<CalcProperty> hintsNoUpdate = NFFact.set();
    @LongMutable
    public ImSet<CalcProperty> getHintsNoUpdate() {
        return hintsNoUpdate.getSet();
    }

    public void addHintsNoUpdate(Version version, LCP... props) {
        for (LCP prop : props) {
            addHintsNoUpdate(prop, version);
        }
    }

    protected void addHintsNoUpdate(LCP prop, Version version) {
        addHintsNoUpdate((CalcProperty) prop.property, version);
    }

    public void addHintsNoUpdate(CalcProperty prop, Version version) {
        hintsNoUpdate.add(prop, version);
    }

    public FormView createDefaultRichDesign(Version version) {
        return new DefaultFormView(this, version);
    }

    private NFProperty<FormView> richDesign = NFFact.property();

    public FormView getRichDesign() {
        return richDesign.get(); // assert что не null см. последнюю строку в конструкторе
/*        return richDesign.getDefault(new NFDefault<FormView>() {
            public FormView create() {
                return createDefaultRichDesign(Version.LAST);
            }
        });*/
    }

    public FormView getNFRichDesign(Version version) {
        return richDesign.getNF(version);
    }

    public void setRichDesign(FormView view, Version version) {
        richDesign.set(view, version);
    }

    private StaticDataGenerator.Hierarchy getHierarchy(boolean supportGroupColumns, ImSet<GroupObjectEntity> valueGroups, GetKeyValue<ImOrderSet<PropertyDrawEntity>, GroupObjectEntity, ImOrderSet<PropertyDrawEntity>> filter) {
        ImMap<GroupObjectEntity, ImOrderSet<PropertyDrawEntity>> groupProperties = getGroupProperties(valueGroups);
        if(filter != null)
            groupProperties = groupProperties.mapValues(filter);
        return new StaticDataGenerator.Hierarchy(getGroupHierarchy(supportGroupColumns, valueGroups), groupProperties, valueGroups);
    }

    @IdentityInstanceLazy
    public StaticDataGenerator.Hierarchy getImportHierarchy() {
        return getHierarchy(false, SetFact.<GroupObjectEntity>EMPTY(), null);
    }
    public ImMap<GroupObjectEntity, ImSet<FilterEntity>> getImportFixedFilters() {
        return getGroupFixedFilters(SetFact.<GroupObjectEntity>EMPTY());
    }

    @IdentityInstanceLazy
    private StaticDataGenerator.Hierarchy getCachedStaticHierarchy(boolean isReport, ImSet<GroupObjectEntity> valueGroups) {
        return getHierarchy(isReport, valueGroups, null);
    }
    
    public StaticDataGenerator.Hierarchy getStaticHierarchy(boolean supportGroupColumns, ImSet<GroupObjectEntity> valueGroups, GetKeyValue<ImOrderSet<PropertyDrawEntity>, GroupObjectEntity, ImOrderSet<PropertyDrawEntity>> filter) {
        if(filter == null) // optimization
            return getCachedStaticHierarchy(supportGroupColumns, valueGroups);
        return getHierarchy(supportGroupColumns, valueGroups, filter);
    }

    @IdentityInstanceLazy
    public GroupObjectHierarchy getGroupHierarchy(boolean supportGroupColumns, ImSet<GroupObjectEntity> excludeGroupObjects) {
        return new FormGroupHierarchyCreator(this, supportGroupColumns).createHierarchy(excludeGroupObjects);
    }

    @IdentityInstanceLazy
    public GroupObjectHierarchy getSingleGroupObjectHierarchy(GroupObjectEntity groupObject) {
        return new GroupObjectHierarchy(groupObject, Collections.singletonMap(groupObject, SetFact.<GroupObjectEntity>EMPTYORDER()));
    }

    public void addActionsOnObjectChange(ObjectEntity object, Version version, ActionPropertyObjectEntity... actions) {
        addActionsOnObjectChange(object, false, version, actions);
    }

    public void addActionsOnObjectChange(ObjectEntity object, boolean drop, Version version, ActionPropertyObjectEntity... actions) {
        addActionsOnEvent(object, drop, version, actions);
    }

    public void addActionsOnEvent(Object eventObject, Version version, ActionPropertyObjectEntity<?>... actions) {
        addActionsOnEvent(eventObject, false, version, actions);
    }

    public void addActionsOnEvent(Object eventObject, boolean drop, Version version, ActionPropertyObjectEntity<?>... actions) {
        if(drop)
            eventActions.removeAll(eventObject, version);
        eventActions.addAll(eventObject, Arrays.asList(actions), version);
    }

    public ComponentView getDrawComponent(PropertyDrawEntity<?> property, boolean grid) {
        FormView formView = getRichDesign();
        ComponentView drawComponent;
        if(grid) {
            GroupObjectEntity toDraw = property.getToDraw(this);
            if (toDraw.isInTree())
                drawComponent = formView.get(toDraw.treeGroup);
            else
                drawComponent = formView.get(toDraw).grid;
        } else
            drawComponent = formView.get(property);
        return drawComponent;
    }

    public void finalizeAroundInit() {
        groups.finalizeChanges();
        treeGroups.finalizeChanges();
        propertyDraws.finalizeChanges();
        fixedFilters.finalizeChanges();
        eventActions.finalizeChanges();
        defaultOrders.finalizeChanges();
        fixedOrders.finalizeChanges();
        
        hintsIncrementTable.finalizeChanges();
        hintsNoUpdate.finalizeChanges();
        
        for(RegularFilterGroupEntity regularFilterGroup : getRegularFilterGroupsIt())
            regularFilterGroup.finalizeAroundInit();
        
        getRichDesign().finalizeAroundInit();
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public String getName() {
        return CanonicalNameUtils.getName(canonicalName);
    } 
    
    public LocalizedString getCaption() {
        return caption;
    }

    public String getCreationPath() {
        if (debugPoint == null) {
            return null;
        } else {
            return debugPoint.toString();
        }
    }

    public int getID() {
        return ID;
    }

    public String getSID() {
        if (canonicalName != null) {
            return canonicalName;
        } else {
            return "_FORM_" + getID();
        }
    }

    public boolean isNamed() {
        return canonicalName != null;
    }

    public boolean needsToBeSynchronized() {
        return isNamed();
    }

    public String getDefaultImagePath() {
        return defaultImagePath;
    }

    public void setDebugPoint(DebugInfo.DebugPoint debugPoint) {
        this.debugPoint = debugPoint;
    }

    // сохраняет нижние компоненты
    public static class ComponentDownSet extends AddSet<ComponentView, ComponentDownSet> {

        public ComponentDownSet() {
        }

        public static ComponentDownSet create(MAddSet<ComponentView> components) {
            ComponentDownSet result = new ComponentDownSet();
            for(ComponentView component : components)
                result = result.addItem(component);
            return result;
        }

        public ComponentDownSet(ComponentView where) {
            super(where);
        }

        public ComponentDownSet(ComponentView[] wheres) {
            super(wheres);
        }

        protected ComponentDownSet createThis(ComponentView[] wheres) {
            return new ComponentDownSet(wheres);
        }

        protected ComponentView[] newArray(int size) {
            return new ComponentView[size];
        }

        protected boolean containsAll(ComponentView who, ComponentView what) {
            return what.isAncestorOf(who);
        }

        public ComponentDownSet addItem(ComponentView container) {
            return add(new ComponentDownSet(container));
        }

        public ComponentDownSet addAll(ComponentDownSet set) {
            return add(set);
        }
    }

    // сохраняет верхние компоненты
    public static class ComponentUpSet extends AddSet<ComponentView, ComponentUpSet> {

        public ComponentUpSet() {
        }

        public ComponentUpSet(ComponentView where) {
            super(where);
        }

        public ComponentUpSet(ComponentView[] wheres) {
            super(wheres);
        }

        protected ComponentUpSet createThis(ComponentView[] wheres) {
            return new ComponentUpSet(wheres);
        }

        protected ComponentView[] newArray(int size) {
            return new ComponentView[size];
        }

        protected boolean containsAll(ComponentView who, ComponentView what) {
            return who.isAncestorOf(what);
        }

        public ComponentUpSet addItem(ComponentView container) {
            return add(new ComponentUpSet(container));
        }
        
        public ComponentUpSet addAll(ComponentUpSet set) {
            return add(set);            
        }
    }

    public boolean isDesignHidden(ComponentView component) { // global
        return component.isDesignHidden();
    }

    @IdentityLazy
    public ComponentUpSet getDrawLocalHideableContainers(GroupObjectEntity group) {
        ComponentUpSet result = new ComponentUpSet();
        for(PropertyDrawEntity<?> property : getPropertyDrawsIt())
            if(!group.getObjects().disjoint(property.getObjectInstances())) {  // для свойств "зависящих" от группы
                for(int t=0;t<2;t++) {
                    ComponentView drawComponent = getDrawComponent(property, t == 0); // не hidden и первый showifOrTab
                    if(!isDesignHidden(drawComponent)) {
                        ComponentView localHideableContainer = drawComponent.getLocalHideableContainer();
                        if (localHideableContainer == null) // cheat \ оптимизация
                            return null;
                        result = result.addItem(localHideableContainer);
                    }
                }
            }
        ImSet<FilterEntity> fixedFilters = getFixedFilters();
        MSet<GroupObjectEntity> mFixedGroupObjects = SetFact.mSetMax(fixedFilters.size());
        for(FilterEntity<?> filterEntity : fixedFilters) {
            if(!group.getObjects().disjoint(filterEntity.getObjects())) { // для фильтров "зависящих" от группы
                GroupObjectEntity drawGroup = filterEntity.getApplyObject(this);
                if(!drawGroup.equals(group))
                    mFixedGroupObjects.add(drawGroup); 
            }
        }
        for(GroupObjectEntity fixedGroupObject : mFixedGroupObjects.immutable()) {
            ComponentUpSet drawContainers = getDrawLocalHideableContainers(fixedGroupObject);
            if(drawContainers==null)
                return null;
                
            result = result.addAll(drawContainers);
        }
        return result;
    }

    public void setReadOnlyIf(PropertyDrawEntity property, CalcPropertyObjectEntity condition) {
        property.propertyReadOnly = condition;
    }

    public void setEditType(PropertyEditType editType) {
        for (PropertyDrawEntity propertyView : getPropertyDrawsIt()) {
            setEditType(propertyView, editType);
        }
    }

    public void setNFEditType(PropertyEditType editType, Version version) {
        for (PropertyDrawEntity propertyView : getNFPropertyDrawsIt(version)) {
            setEditType(propertyView, editType);
        }
    }

    public void setEditType(PropertyDrawEntity property, PropertyEditType editType) {
        property.setEditType(editType);
    }

    public void addDefaultOrder(PropertyDrawEntity property, boolean ascending, Version version) {
        defaultOrders.add(property, ascending, version);
    }

    public void addDefaultOrderView(PropertyDrawEntity property, boolean ascending, Version version) {
        FormView richDesign = getNFRichDesign(version);
        if(richDesign !=null)
            richDesign.addDefaultOrder(property, ascending, version);
    }

    public void setPageSize(int pageSize) {
        for (GroupObjectEntity group : getGroupsIt()) {
            group.pageSize = pageSize;
        }
    }

    public void setNeedVerticalScroll(boolean scroll) {
        for (GroupObjectEntity entity : getGroupsIt()) {
            getRichDesign().get(entity).needVerticalScroll = scroll;
        }
    }

    public ValueClass getBaseClass(ObjectEntity object) {
        return object.baseClass;
    }

    @IdentityLazy
    public ImSet<ObjectEntity> getObjects() {
        MExclSet<ObjectEntity> mObjects = SetFact.mExclSet();
        for (GroupObjectEntity group : getGroupsIt())
            mObjects.exclAddAll(group.getObjects());
        return mObjects.immutable();
    }

    @Override
    public String toString() {
        String result = getSID();
        if (caption != null) {
            result += " '" + ThreadLocalContext.localize(caption) + "'";
        }
        if (debugPoint != null) {
            result += " [" + debugPoint + "]";
        }
        return result;
    }

    @Override
    public FormEntity getStaticForm() {
        return this;
    }

    public Pair<FormEntity, ImRevMap<ObjectEntity, ObjectEntity>> getForm(BaseLogicsModule<?> LM, DataSession session, ImMap<ObjectEntity, ? extends ObjectValue> mapObjectValues) {
        return new Pair<>((FormEntity)this, getObjects().toRevMap());
    }
}