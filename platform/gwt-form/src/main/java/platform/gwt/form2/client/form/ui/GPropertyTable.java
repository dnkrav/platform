package platform.gwt.form2.client.form.ui;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.client.ui.CustomScrollPanel;
import com.google.gwt.user.client.ui.HeaderPanel;
import platform.gwt.form2.client.form.dispatch.GEditPropertyDispatcher;
import platform.gwt.form2.client.form.dispatch.GEditPropertyHandler;
import platform.gwt.form2.shared.actions.form.ServerResponseResult;
import platform.gwt.form2.shared.view.GPropertyDraw;
import platform.gwt.form2.shared.view.changes.GGroupObjectValue;
import platform.gwt.form2.shared.view.classes.GType;
import platform.gwt.form2.shared.view.grid.EditManager;
import platform.gwt.form2.shared.view.grid.GridEditableCell;
import platform.gwt.form2.shared.view.grid.editor.GridCellEditor;

public abstract class GPropertyTable extends DataGrid implements EditManager, GEditPropertyHandler {

    protected final GFormController form;
    protected final GEditPropertyDispatcher editDispatcher;

    protected GridEditableCell editCell;
    protected NativeEvent editEvent;
    protected Cell.Context editContext;
    protected Element editCellParent;
    protected GType editType;

    public GPropertyTable(GFormController iform, Resources resources) {
        super(50, resources);

        addStyleName(getResources().style().widget());

        this.form = iform;

        this.editDispatcher = new GEditPropertyDispatcher(form);
    }

    public CustomScrollPanel getScrollPanel() {
        HeaderPanel header = (HeaderPanel) getWidget();
        return (CustomScrollPanel) header.getContentWidget();
    }

    @Override
    public void requestValue(GType valueType, Object oldValue) {
        editType = valueType;

        GridCellEditor cellEditor = valueType.createGridCellEditor(this, getProperty(editContext));
        if (cellEditor != null) {
            NativeEvent event = editEvent;
            editEvent = null;
            form.setCurrentEditingTable(this);
            editCell.startEditing(event, editContext, editCellParent, cellEditor, oldValue);
        } else {
            cancelEditing();
        }
    }

    @Override
    public void updateEditValue(Object value) {
        setValueAt(editContext, value);
    }

    public abstract boolean isEditable(Cell.Context context);
    public abstract void setValueAt(Cell.Context context, Object value);
    public abstract Object getValueAt(Cell.Context context);

    @Override
    public void postDispatchResponse(ServerResponseResult response) {
        setFocus(true);
    }

    @Override
    public boolean canStartNewEdit() {
        return !form.isEditing();
    }

    @Override
    public void executePropertyEditAction(GridEditableCell editCell, NativeEvent editEvent, Cell.Context editContext, Element parent) {
        this.editCell = editCell;
        this.editEvent = editEvent;
        this.editContext = editContext;
        this.editCellParent = parent;

        if (isEditable(editContext)) {
            GPropertyDraw property = getProperty(editContext);
            GGroupObjectValue columnKey = getColumnKey(editContext);
            Object oldValue = getValueAt(editContext);

            editDispatcher.executePropertyEditAction(this, property, oldValue, columnKey);
        }
    }

    @Override
    public void commitEditing(Object value) {
        editDispatcher.commitValue(value);

        clearEditState();
        setFocus(true);
        form.setCurrentEditingTable(null);
    }

    @Override
    public void cancelEditing() {
        editDispatcher.cancelEdit();

        clearEditState();
        setFocus(true);
        form.setCurrentEditingTable(null);
    }

    private void clearEditState() {
        editCell.finishEditing(editContext, editCellParent, getValueAt(editContext));

        editCell = null;
        editContext = null;
        editCellParent = null;
        editType = null;
    }
}
