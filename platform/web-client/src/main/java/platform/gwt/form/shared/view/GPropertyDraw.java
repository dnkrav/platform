package platform.gwt.form.shared.view;

import platform.gwt.form.client.form.ui.GFormController;
import platform.gwt.form.shared.view.changes.GGroupObjectValue;
import platform.gwt.form.shared.view.classes.GType;
import platform.gwt.form.shared.view.grid.EditManager;
import platform.gwt.form.shared.view.grid.editor.GridCellEditor;
import platform.gwt.form.shared.view.grid.renderer.GridCellRenderer;
import platform.gwt.form.shared.view.logics.GGroupObjectLogicsSupplier;
import platform.gwt.form.shared.view.panel.PanelRenderer;
import platform.gwt.form.shared.view.reader.*;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class GPropertyDraw extends GComponent implements GPropertyReader {
    public static final String CAPTION_ORIGINAL = "CAPTION_ORIGINAL";

    public int ID;
    public String sID;
    public String caption;
    public GType baseType;

    public GGroupObject groupObject;
    public ArrayList<GGroupObject> columnGroupObjects;

    public GType changeType;
    public AddRemove addRemove;
    public boolean askConfirm;
    public String askConfirmMessage;

    public GEditBindingMap editBindingMap;

    public ImageDescription icon;
    public boolean focusable;
    public boolean checkEquals;
    public GPropertyEditType editType = GPropertyEditType.EDITABLE;

    public boolean echoSymbols;

    public GKeyStroke editKey;
    public boolean showEditKey;

    public boolean drawAsync;

    public GCaptionReader captionReader;
    public GFooterReader footerReader;
    public GReadOnlyReader readOnlyReader;
    public GBackgroundReader backgroundReader;
    public GForegroundReader foregroundReader;

    public int minimumCharWidth;
    public int maximumCharWidth;
    public int preferredCharWidth;

    public boolean panelLabelAbove;

    private transient GridCellRenderer cellRenderer;

    public static class AddRemove implements Serializable {
        public GObject object;
        public boolean add;

        public AddRemove() {}

        public AddRemove(GObject object, boolean add) {
            this.object = object;
            this.add = add;
        }
    }

    public GPropertyDraw(){}

    public void update(GGroupObjectLogicsSupplier controller, Map<GGroupObjectValue, Object> values, boolean updateKeys) {
        controller.updatePropertyDrawValues(this, values, updateKeys);
    }

    public PanelRenderer createPanelRenderer(GFormController form, GGroupObjectValue columnKey) {
        return baseType.createPanelRenderer(form, this, columnKey);
    }

    public GridCellRenderer getGridCellRenderer() {
        if (cellRenderer == null) {
            cellRenderer = baseType.createGridCellRenderer(this);
        }
        return cellRenderer;
    }

    public GridCellEditor createGridCellEditor(EditManager editManager) {
        return baseType.createGridCellEditor(editManager, this);
    }

    public GridCellEditor createValueCellEdtor(EditManager editManager) {
        return baseType.createValueCellEditor(editManager, this);
    }

    public Object parseString(String s) throws ParseException {
        return baseType.parseString(s);
    }

    @Override
    public int getGroupObjectID() {
        return groupObject != null ? groupObject.ID : -1;
    }

    public String getCaptionOrEmpty() {
        return caption == null ? "" : caption;
    }

    public String getDynamicCaption(Object caption) {
        return CAPTION_ORIGINAL.equals(caption)
               ? getCaptionOrEmpty()
               : (caption == null ? "" : caption.toString().trim());
    }

    public String getEditCaption(String caption) {
        if (caption == null) {
            caption = this.caption;
        }

        return showEditKey && editKey != null ? caption + " (" + editKey + ")" : caption;
    }

    public String getEditCaption() {
        return getEditCaption(caption);
    }

    public String getNotEmptyCaption() {
        if (caption == null || caption.trim().length() == 0) {
            return "Неопределённое свойство";
        } else {
            return caption;
        }
    }

    public String getIconPath(boolean enabled) {
        if (icon == null) {
            return null;
        }
        if (!enabled && icon.url != null) {
            int dotInd = icon.url.lastIndexOf(".");
            if (dotInd != -1) {
                return icon.url.substring(0, dotInd) + "_Disabled" + icon.url.substring(dotInd);
            }
        }

        return icon.url;
    }

    public boolean isReadOnly() {
        return editType == GPropertyEditType.READONLY;
    }

    public String getMinimumWidth() {
        return getMinimumPixelWidth() + "px";
    }

    public int getMinimumPixelWidth() {
        if (minimumWidth != -1) {
            return minimumWidth;
        } else {
            return baseType.getMinimumPixelWidth(minimumCharWidth, font);
        }
    }

    public String getMinimumHeight() {
        return getMinimumPixelWidth() + "px";
    }

    public int getMinimumPixelHeight() {
        if (minimumHeight != -1) {
            return minimumHeight;
        } else {
            return baseType.getMinimumPixelHeight(font);
        }
    }

    public String getMaximumWidth() {
        return getMaximumPixelWidth() + "px";
    }

    public int getMaximumPixelWidth() {
        if (maximumWidth != -1) {
            return maximumWidth;
        } else {
            return baseType.getMaximumPixelWidth(maximumCharWidth, font);
        }
    }

    public String getMaximumHeight() {
        return maximumHeight + "px";
    }

    public String getPreferredWidth() {
        return getPreferredPixelWidth() + "px";
    }

    public int getPreferredPixelWidth() {
        if (preferredWidth != -1) {
            return preferredWidth;
        } else {
            return baseType.getPreferredPixelWidth(preferredCharWidth, font);
        }
    }

    public String getPreferredHeight() {
        return getPreferredPixelHeight() + "px";
    }

    public int getPreferredPixelHeight() {
        if (preferredHeight != -1) {
            return preferredHeight;
        } else {
            return baseType.getPreferredPixelHeight(font);
        }
    }

    public LinkedHashMap<String, String> getContextMenuItems() {
        return editBindingMap == null ? null : editBindingMap.getContextMenuItems();
    }

    @Override
    public int hashCode() {
        return ID;
    }

    @Override
    public String toString() {
        return "GPropertyDraw{" +
                ", sID='" + sID + '\'' +
                ", caption='" + caption + '\'' +
                ", baseType=" + baseType +
                ", changeType=" + changeType +
                ", iconPath='" + icon + '\'' +
                ", focusable=" + focusable +
                ", checkEquals=" + checkEquals +
                ", editType=" + editType +
                '}';
    }
}
