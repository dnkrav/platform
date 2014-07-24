package lsfusion.server.logics.property;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;

public abstract class ChangeProperty<T extends PropertyInterface> extends AggregateProperty<T> {

    public ChangeProperty(String caption, ImOrderSet<T> interfaces) {
        super(caption, interfaces);
    }

    protected boolean useSimpleIncrement() {
        throw new RuntimeException("not supported"); // can not be stored / modified;
    }
}
