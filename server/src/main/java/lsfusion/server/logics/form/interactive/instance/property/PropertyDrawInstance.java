package lsfusion.server.logics.form.interactive.instance.property;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.form.property.ClassViewType;
import lsfusion.interop.form.property.PropertyReadType;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.lambda.SQLCallable;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.form.interactive.instance.CellInstance;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.instance.order.OrderInstance;
import lsfusion.server.logics.form.struct.action.ActionObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawExtraType;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.logics.property.value.NullValueProperty;
import lsfusion.server.physics.admin.authentication.security.policy.SecurityPolicy;

import java.sql.SQLException;
import java.util.Map;

// представление св-ва
public class PropertyDrawInstance<P extends PropertyInterface> extends CellInstance<PropertyDrawEntity> implements AggrReaderInstance {

    public ActionObjectInstance getEventAction(String actionId, FormInstance formInstance, SQLCallable<Boolean> checkReadOnly, SecurityPolicy securityPolicy) throws SQLException, SQLHandledException {
        ActionObjectEntity<?> eventAction = entity.getEventAction(actionId, formInstance.entity, checkReadOnly, securityPolicy);
        if(eventAction!=null)
            return formInstance.instanceFactory.getInstance(eventAction);
        return null;
    }

    private ActionOrPropertyObjectInstance<?, ?> propertyObject;
    
    public ActionOrPropertyObjectInstance<?, ?> getValueProperty() {
        return propertyObject;
    }

    public boolean isInInterface(final ImSet<GroupObjectInstance> classGroups, boolean any) {
        return getValueProperty().isInInterface(classGroups, any);
    }

    public OrderInstance getOrder() {
        return (PropertyObjectInstance) getValueProperty();
    }
    
    public boolean isProperty() {
        return getValueProperty() instanceof PropertyObjectInstance;
    }

    // в какой "класс" рисоваться, ессно один из Object.GroupTo должен быть ToDraw
    public GroupObjectInstance toDraw; // не null, кроме когда без параметров в FormInstance проставляется

    private final ImOrderSet<GroupObjectInstance> columnGroupObjects;
    public ImSet<GroupObjectInstance> getColumnGroupObjects() {
        return columnGroupObjects.getSet();
    }
    public ImOrderSet<GroupObjectInstance> getOrderColumnGroupObjects() {
        return columnGroupObjects;
    }
    @IdentityLazy
    public ImSet<GroupObjectInstance> getColumnGroupObjectsInGrid() {
        return getColumnGroupObjects().filterFn(element -> element.classView.isGrid());
    }
    @IdentityLazy
    public ImSet<GroupObjectInstance> getGroupObjectsInGrid() {
        ImSet<GroupObjectInstance> result = getColumnGroupObjectsInGrid();
        if(isGrid())
            result = result.addExcl(toDraw);
        return result;
    }

    public Type getType() {
        return entity.getType();
    }

    public HiddenReaderInstance hiddenReader = new HiddenReaderInstance();

    // предполагается что propertyCaption ссылается на все из propertyObject но без toDraw (хотя опять таки не обязательно)
    public final PropertyObjectInstance<?> propertyCaption;
    public final PropertyObjectInstance<?> propertyShowIf;
    public final PropertyObjectInstance<?> propertyReadOnly;
    public final PropertyObjectInstance<?> propertyFooter;
    public final PropertyObjectInstance<?> propertyBackground;
    public final PropertyObjectInstance<?> propertyForeground;
    public final ImList<PropertyObjectInstance<?>> propertiesAggrLast;

    public ExtraReaderInstance captionReader;
    public ShowIfReaderInstance showIfReader;
    public ExtraReaderInstance footerReader;
    public ExtraReaderInstance readOnlyReader;
    public ExtraReaderInstance backgroundReader;
    public ExtraReaderInstance foregroundReader;
    public final ImOrderSet<LastReaderInstance> aggrLastReaders;

    public PropertyDrawInstance(PropertyDrawEntity<P> entity,
                                ActionOrPropertyObjectInstance<?, ?> propertyObject,
                                GroupObjectInstance toDraw,
                                ImOrderSet<GroupObjectInstance> columnGroupObjects,
                                ImMap<PropertyDrawExtraType, PropertyObjectInstance<?>> propertyExtras,
                                ImList<PropertyObjectInstance<?>> propertiesAggrLast) {
        super(entity);
        this.propertyObject = propertyObject;
        this.toDraw = toDraw;
        this.columnGroupObjects = columnGroupObjects;

        propertyCaption = propertyExtras.get(PropertyDrawExtraType.CAPTION);
        propertyShowIf = propertyExtras.get(PropertyDrawExtraType.SHOWIF);
        propertyReadOnly = propertyExtras.get(PropertyDrawExtraType.READONLYIF);
        propertyFooter = propertyExtras.get(PropertyDrawExtraType.FOOTER);
        propertyBackground = propertyExtras.get(PropertyDrawExtraType.BACKGROUND);
        propertyForeground = propertyExtras.get(PropertyDrawExtraType.FOREGROUND);
        this.propertiesAggrLast = propertiesAggrLast;

        captionReader = new ExtraReaderInstance(PropertyDrawExtraType.CAPTION, propertyCaption);
        showIfReader = new ShowIfReaderInstance(PropertyDrawExtraType.SHOWIF, propertyShowIf);
        footerReader = new ExtraReaderInstance(PropertyDrawExtraType.FOOTER, propertyFooter);
        readOnlyReader = new ExtraReaderInstance(PropertyDrawExtraType.READONLYIF, propertyReadOnly);
        backgroundReader = new ExtraReaderInstance(PropertyDrawExtraType.BACKGROUND, propertyBackground);
        foregroundReader = new ExtraReaderInstance(PropertyDrawExtraType.FOREGROUND, propertyForeground);
        aggrLastReaders = SetFact.toOrderExclSet(propertiesAggrLast.size(), LastReaderInstance::new);
    }

    public PropertyObjectInstance getPropertyObjectInstance() {
        return getDrawInstance();
    }

    public PropertyObjectInstance<?> getDrawInstance() {
        return getValueProperty().getDrawProperty();
    }

    public byte getTypeID() {
        return PropertyReadType.DRAW;
    }

    public boolean isGrid() {
        return (toDraw != null ? toDraw.classView : ClassViewType.PANEL).isGrid() && entity.forceViewType.isGrid();
    }

    public String toString() {
        return propertyObject.toString();
    }

    public PropertyDrawEntity getEntity() {
        return entity;
    }
    
    public String getIntegrationSID() {
        return entity.getIntegrationSID();
    }

    @Override
    public Object getProfiledObject() {
        return entity;
    }

    // заглушка чтобы на сервере ничего не читать
    public class HiddenReaderInstance implements PropertyReaderInstance {

        public PropertyObjectInstance getPropertyObjectInstance() {
            return new PropertyObjectInstance<>(NullValueProperty.instance, MapFact.<PropertyInterface, ObjectInstance>EMPTY());
        }

        public byte getTypeID() {
            return PropertyDrawInstance.this.getTypeID();
        }

        public int getID() {
            return PropertyDrawInstance.this.getID();
        }

        @Override
        public Object getProfiledObject() {
            return NullValueProperty.instance;
        }
    }

    public class ExtraReaderInstance implements PropertyReaderInstance {
        private final PropertyDrawExtraType type;
        private final PropertyObjectInstance property;
        
        public ExtraReaderInstance(PropertyDrawExtraType type, PropertyObjectInstance property) {
            this.type = type;
            this.property = property;
        }
        
        @Override
        public PropertyObjectInstance getPropertyObjectInstance() {
            return property;
        }

        @Override
        public byte getTypeID() {
            return type.getPropertyReadType();
        }

        @Override
        public int getID() {
            return PropertyDrawInstance.this.getID();
        }

        @Override
        public Object getProfiledObject() {
            return entity.getPropertyExtra(type);
        }
        
        public String toString() {
            return ThreadLocalContext.localize(type.getText()) + "(" + PropertyDrawInstance.this.toString() + ")";
        }

        public PropertyDrawInstance<P> getPropertyDraw() {
            return PropertyDrawInstance.this;
        }
    }

    public class ShowIfReaderInstance extends ExtraReaderInstance {
        public ShowIfReaderInstance(PropertyDrawExtraType type, PropertyObjectInstance property) {
            super(type, property);
        }
    }

    @Override
    public PropertyDrawInstance getProperty() {
        return this;
    }

    public class LastReaderInstance implements AggrReaderInstance {
        public final int index;

        public LastReaderInstance(int index) {
            this.index = index;
        }

        @Override
        public PropertyObjectInstance getPropertyObjectInstance() {
            return propertiesAggrLast.get(index);
        }

        @Override
        public PropertyDrawInstance getProperty() {
            return PropertyDrawInstance.this;
        }

        @Override
        public byte getTypeID() {
            return PropertyReadType.LAST;
        }

        @Override
        public int getID() {
            return PropertyDrawInstance.this.getID();
        }

        @Override
        public Object getProfiledObject() {
            return entity.lastAggrColumns.get(index);
        }
    }
}
