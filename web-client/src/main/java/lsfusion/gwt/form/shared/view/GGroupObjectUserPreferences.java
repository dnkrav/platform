package lsfusion.gwt.form.shared.view;

import java.io.Serializable;
import java.util.Map;

public class GGroupObjectUserPreferences implements Serializable {
    private Map<String, GColumnUserPreferences> columnUserPreferences;
    private String groupObjectSID;
    private GFont font;
    private  boolean hasUserPreferences;

    @SuppressWarnings("UnusedDeclaration")
    public GGroupObjectUserPreferences() {
    }

    public GGroupObjectUserPreferences(Map<String, GColumnUserPreferences> columnUserPreferences,
                                      String groupObjectSID, GFont font, boolean hasUserPreferences) {
        this.columnUserPreferences = columnUserPreferences;
        this.groupObjectSID = groupObjectSID;
        this.font = font;
        this.hasUserPreferences = hasUserPreferences;
    }

    public Map<String, GColumnUserPreferences> getColumnUserPreferences() {
        return columnUserPreferences;
    }

    public String getGroupObjectSID() {
        return groupObjectSID;
    }

    public GFont getFont() {
        return font;
    }
    
    public boolean hasUserPreferences() {
        return hasUserPreferences;
    }
}
