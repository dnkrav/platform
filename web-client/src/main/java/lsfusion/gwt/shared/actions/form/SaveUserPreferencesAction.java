package lsfusion.gwt.shared.actions.form;

import lsfusion.gwt.shared.form.object.table.grid.user.design.GGroupObjectUserPreferences;

public class SaveUserPreferencesAction extends FormRequestIndexCountingAction<ServerResponseResult> {
    public GGroupObjectUserPreferences groupObjectUserPreferences;
    public boolean forAllUsers;
    public boolean completeOverride;
    public String[] hiddenProps;

    @SuppressWarnings("Unused")
    public SaveUserPreferencesAction() {
    }

    public SaveUserPreferencesAction(GGroupObjectUserPreferences userPreferences, boolean forAllUsers, boolean completeOverride, String[] hiddenProps) {
        this.groupObjectUserPreferences = userPreferences;
        this.forAllUsers = forAllUsers;
        this.completeOverride = completeOverride;
        this.hiddenProps = hiddenProps;
    }
}
