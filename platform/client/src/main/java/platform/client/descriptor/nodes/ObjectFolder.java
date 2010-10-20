package platform.client.descriptor.nodes;

import platform.client.ClientTree;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.ObjectDescriptor;

import javax.swing.*;

public class ObjectFolder extends PlainTextNode<ObjectFolder> {

    private GroupObjectDescriptor group;

    public ObjectFolder(GroupObjectDescriptor group) {
        super("Oбъекты");

        this.group = group;

        for (ObjectDescriptor object : group)
            add(new ObjectNode(object, group));
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {
        return ClientTree.getNode(info) instanceof ObjectNode;
    }

    @Override
    public boolean importData(ClientTree tree, TransferHandler.TransferSupport info) {
        return group.moveObject((ObjectDescriptor)ClientTree.getNode(info).getTypedObject(), ClientTree.getChildIndex(info));
    }

}
