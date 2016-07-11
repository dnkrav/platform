package lsfusion.server.logics;

import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.form.entity.PropertyObjectEntity;
import lsfusion.server.form.entity.PropertyObjectInterfaceEntity;
import lsfusion.server.logics.property.PropertyInterface;

import java.util.ArrayList;
import java.util.List;

/**
 * User: DAle
 * Date: 20.11.13
 * Time: 10:58
 */

public class OldDBNamePolicy implements PropertyDBNamePolicy {
    @Override
    public String createName(String namespaceName, String name, List<ResolveClassSet> signature) {
        if (namespaceName == null) {
            return name;
        } else {
            return namespaceName + "_" + name;
        }
    }

    @Override
    public String transformToDBName(String canonicalName) {
        String sid = canonicalName.replace(".", "_");
        int bracketPos = sid.indexOf(PropertyCanonicalNameUtils.signatureLBracket);
        if (bracketPos >= 0) {
            sid = sid.substring(0, bracketPos);
        }
        return sid;
    }

    public static String createPropertyDrawSID(String name, PropertyObjectEntity<?, ?> property) {
        List<String> mapping = new ArrayList<>();  
        for (PropertyInterface<?> pi : property.property.getOrderInterfaces()) {
            PropertyObjectInterfaceEntity obj = property.mapping.getObject(pi);
            assert obj instanceof ObjectEntity;
            mapping.add(((ObjectEntity) obj).getSID());
        }
        return PropertyDrawEntity.createSID(name, mapping);
    }
}
