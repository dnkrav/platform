package lsfusion.server.data.expr;

import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.logics.classes.user.set.AndClassSet;

public class CurrentEnvironmentExpr extends StaticNullableExpr {

    private final String paramString;

    public CurrentEnvironmentExpr(String paramString, AndClassSet paramClass) {
        super(paramClass);
        this.paramString = paramString;
    }
    public boolean calcTwins(TwinImmutableObject obj) {
        return paramString.equals(((CurrentEnvironmentExpr) obj).paramString);
    }

    public int hash(HashContext hashContext) {
        return paramString.hashCode();
    }

    public String getSource(CompileSource compile, boolean needValue) {
        return paramString;
    }
}
