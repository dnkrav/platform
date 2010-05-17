package platform.interop.form;

import platform.interop.action.ClientAction;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface RemoteFormInterface extends Remote {

    byte[] getReportDesignByteArray(boolean toExcel) throws RemoteException;
    boolean hasCustomReportDesign() throws RemoteException;
    byte[] getReportDataByteArray() throws RemoteException;

    byte[] getRichDesignByteArray() throws RemoteException;

    void changePageSize(int groupID, int pageSize) throws RemoteException;
    void gainedFocus() throws RemoteException;

    byte[] getFormChangesByteArray() throws RemoteException;

    void changeGroupObject(int groupID, byte[] value) throws RemoteException;

    int getObjectClassID(int objectID) throws RemoteException;

    void changeGroupObject(int groupID, byte changeType) throws RemoteException;

    List<ClientAction> changePropertyView(int propertyID, byte[] object) throws RemoteException;

    void changeObject(int objectID, Integer value) throws RemoteException;

    void addObject(int objectID, int classID) throws RemoteException;

    void changeClass(int objectID, int classID) throws RemoteException;

    void changeGridClass(int objectID,int idClass) throws RemoteException;

    boolean switchClassView(int groupID) throws RemoteException;

    boolean changeClassView(int groupID, byte classView) throws RemoteException;

    void changePropertyOrder(int propertyID, byte modiType) throws RemoteException;

    void changeObjectOrder(int propertyID, byte modiType) throws RemoteException;

    void clearUserFilters() throws RemoteException;

    void addFilter(byte[] state) throws RemoteException;

    void setRegularFilter(int groupID, int filterID) throws RemoteException;

    int getID() throws RemoteException;

    void refreshData() throws RemoteException;

    boolean hasSessionChanges() throws RemoteException;

    String saveChanges() throws RemoteException;

    void cancelChanges() throws RemoteException;

    byte[] getBaseClassByteArray(int objectID) throws RemoteException;

    byte[] getChildClassesByteArray(int objectID, int classID) throws RemoteException;

    byte[] getPropertyChangeType(int propertyID) throws RemoteException;

    final static int GID_SHIFT = 1000;

    final static int CHANGEGROUPOBJECT_FIRSTROW = 0;
    final static int CHANGEGROUPOBJECT_LASTROW = 1;

    RemoteDialogInterface createClassPropertyDialog(int viewID, int value) throws RemoteException;

    RemoteDialogInterface createEditorPropertyDialog(int viewID) throws RemoteException;

    RemoteDialogInterface createObjectDialog(int objectID) throws RemoteException;

    RemoteDialogInterface createObjectDialog(int objectID, int value) throws RemoteException;
}
