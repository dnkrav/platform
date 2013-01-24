package platform.gwt.form.client.form.ui.container;

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.Widget;
import platform.gwt.form.client.form.ui.GFormController;
import platform.gwt.form.shared.view.GComponent;
import platform.gwt.form.shared.view.GContainer;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class GFormTabbedPane extends GAbstractFormContainer {
    private TabPanel tabsPanel;

    public GFormTabbedPane(final GFormController formController, final GContainer key) {
        this.key = key;

        tabsPanel = new TabPanel();
        tabsPanel.getDeckPanel().setHeight("100%");

        tabsPanel.addSelectionHandler(new SelectionHandler<Integer>() {
            @Override
            public void onSelection(SelectionEvent<Integer> e) {
                int index = e.getSelectedItem();
                formController.setTabVisible(key, (GComponent) childrenViews.keySet().toArray()[index]);
            }
        });
    }

    @Override
    public Widget getUndecoratedView() {
        return tabsPanel;
    }

    @Override
    protected void addToContainer(GComponent childKey, Widget childView, int position) {
        tabsPanel.insert(childView, getTabTitle(childKey), position == -1 ? tabsPanel.getWidgetCount() : position);
        ensureTabSelection();
    }

    @Override
    protected void removeFromContainer(GComponent childKey, Widget childView) {
        tabsPanel.remove(childView);
        ensureTabSelection();
    }

    private void ensureTabSelection() {
        if (tabsPanel.getTabBar().getSelectedTab() == -1 && tabsPanel.getWidgetCount() != 0) {
            tabsPanel.selectTab(0);
        }
    }

    private String getTabTitle(GComponent child) {
        String tabTitle = null;
        if (child instanceof GContainer) {
            tabTitle = ((GContainer) child).title;
        }
        if (tabTitle == null) {
            tabTitle = "";
        }
        return tabTitle;
    }

    public void setChildVisible(GComponent child, boolean visible) {
        Widget childComponent = childrenViews.get(child);
        if (childComponent == null) {
            return;
        }
        ArrayList<GComponent> children = new ArrayList<GComponent> (((LinkedHashMap)childrenViews).keySet());
        if (visible) {
            if (!containerHasChild(childComponent)) {
                int index = -1;
                if (children.indexOf(child) != children.size() - 1 && tabsPanel.getWidgetCount() != 0) {
                    for (int i = children.indexOf(child) + 1; i < children.size(); i++) {
                        int beforeIndex = tabsPanel.getWidgetIndex(childrenViews.get(children.get(i)));
                        if (beforeIndex != -1) {
                            index = beforeIndex;
                            break;
                        }
                    }
                }
                addToContainer(child, childComponent, index);
            }
        } else {
            removeFromContainer(child, childComponent);
        }
    }

    @Override
    protected boolean containerHasChild(Widget childView) {
        return tabsPanel.getWidgetIndex(childView) != -1;
    }

    @Override
    public void setChildSize(GComponent child, String width, String height) {
        Widget childView = childrenViews.get(child);
        if (childView != null) {
            if (width != null) {
                childView.setWidth(width);
            }
            if (height != null) {
                childView.setHeight(height);
            }
        }
    }
}
