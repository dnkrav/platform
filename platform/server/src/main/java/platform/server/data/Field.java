package platform.server.data;

import platform.server.data.sql.SQLSyntax;
import platform.server.data.types.Type;
import platform.server.data.types.TypeSerializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class Field {
    public String name;
    public Type type;

    Field(String iName,Type iType) {
        name = iName;
        type = iType;

        if(type==null)
            iType = iType;
        assert type!=null;
    }

    public String getDeclare(SQLSyntax syntax) {
        return name + " " + type.getDB(syntax);
    }

    public String toString() {
        return name;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(getType());
        outStream.writeUTF(name);
        TypeSerializer.serialize(outStream,type);
    }

    protected Field(DataInputStream inStream) throws IOException {
        name = inStream.readUTF();
        type = TypeSerializer.deserialize(inStream);
    }

    public static Field deserialize(DataInputStream inStream) throws IOException {
        int type = inStream.readByte();
        if(type==0) return new KeyField(inStream);
        if(type==1) return new PropertyField(inStream);

        throw new IOException();
    }

    abstract byte getType();

    @Override
    public boolean equals(Object obj) {
        return this==obj || obj instanceof Field && name.equals(((Field)obj).name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
