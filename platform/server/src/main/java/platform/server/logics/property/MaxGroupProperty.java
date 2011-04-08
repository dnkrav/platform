package platform.server.logics.property;

import platform.interop.Compare;
import platform.server.Settings;
import platform.server.data.expr.Expr;
import platform.server.session.Changes;
import platform.server.session.Modifier;

import java.util.Collection;
import java.util.Map;

public class MaxGroupProperty<T extends PropertyInterface> extends GroupProperty<T> {

    public MaxGroupProperty(String sID, String caption, Collection<? extends PropertyInterfaceImplement<T>> interfaces, Property<T> property) {
        super(sID, caption, interfaces, property, 0);
    }

    public Expr getChangedExpr(Expr changedExpr, Expr changedPrevExpr, Map<Interface<T>, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier) {
        if(Settings.instance.isNoIncrementMaxGroupProperty())
            return calculateNewExpr(joinImplement, modifier); 
        Expr prevExpr = getExpr(joinImplement);
        return changedExpr.ifElse(changedExpr.compare(prevExpr,Compare.GREATER).or(prevExpr.getWhere().not()),
                calculateNewExpr(joinImplement, modifier).ifElse(changedPrevExpr.compare(prevExpr,Compare.EQUALS), prevExpr));
    }
}
