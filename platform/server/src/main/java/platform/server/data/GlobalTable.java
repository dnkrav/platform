package platform.server.data;

public abstract class GlobalTable extends Table {

    public GlobalTable(String name) {
        super(name);
    }

    @Override
    public int getCount() {
        return Integer.MAX_VALUE;
    }
}
