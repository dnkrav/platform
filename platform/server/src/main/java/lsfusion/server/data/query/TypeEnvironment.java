package lsfusion.server.data.query;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.data.type.ConcatenateType;
import lsfusion.server.data.type.Type;

public interface TypeEnvironment {
    void addNeedRecursion(ImList<Type> types); // assert что все типы уже есть

    void addNeedType(ConcatenateType concType);
}
