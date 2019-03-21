package lsfusion.gwt.shared.navigator;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.view.MainFrame;
import lsfusion.gwt.shared.GwtSharedUtils;
import lsfusion.gwt.shared.navigator.window.GNavigatorWindow;
import lsfusion.gwt.client.base.ImageDescription;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public abstract class GNavigatorElement implements Serializable {
    public String canonicalName;
    public String caption;
    public String creationPath;
    public ImageDescription icon;

    public ArrayList<GNavigatorElement> children;
    public HashSet<GNavigatorElement> parents = new HashSet<>();

    public GNavigatorWindow window;

    public boolean containsParent(Set<GNavigatorElement> set) {
        for (GNavigatorElement parent : parents) {
            if (set.contains(parent)) return true;
        }
        return false;
    }

    public String getTooltipText() {
        return MainFrame.configurationAccessAllowed ?
                GwtSharedUtils.stringFormat("<html><b>%s</b><hr><b>sID:</b> %s<br><b>" + ClientMessages.Instance.get().tooltipPath() 
                        + ":</b> %s</html>", caption, canonicalName, creationPath) : caption;
    }
}
