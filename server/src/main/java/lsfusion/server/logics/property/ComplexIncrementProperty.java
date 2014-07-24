package lsfusion.server.logics.property;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;

// кроме OrderGroupProperty и FormulaUnionProperty
public abstract class ComplexIncrementProperty<T extends PropertyInterface> extends FunctionProperty<T> {

    public ComplexIncrementProperty(String caption, ImOrderSet<T> interfaces) {
        super(caption, interfaces);
    }

    protected boolean useSimpleIncrement() {
        return false;
    }
}
