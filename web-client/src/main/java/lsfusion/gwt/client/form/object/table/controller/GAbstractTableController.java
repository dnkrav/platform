package lsfusion.gwt.client.form.object.table.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.panel.controller.GPanelController;
import lsfusion.gwt.client.form.object.table.view.GToolbarView;
import lsfusion.gwt.client.form.filter.user.controller.GFilterController;
import lsfusion.gwt.client.form.design.view.GFormLayout;
import lsfusion.gwt.shared.form.design.GComponent;
import lsfusion.gwt.shared.form.design.GContainer;
import lsfusion.gwt.shared.form.object.GObject;
import lsfusion.gwt.shared.form.object.table.GToolbar;
import lsfusion.gwt.shared.form.property.GPropertyDraw;
import lsfusion.gwt.shared.form.object.GGroupObjectValue;
import lsfusion.gwt.shared.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.property.cell.controller.EditEvent;
import lsfusion.gwt.shared.form.property.GFooterReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class GAbstractTableController implements GTableController {
    protected final GFormController formController;
    protected final GPanelController panel;
    protected final GToolbarView toolbarView;
    public GFilterController filter;

    public GAbstractTableController(GFormController formController, GToolbar toolbar) {
        this.formController = formController;

        panel = new GPanelController(formController);

        if (toolbar == null || !toolbar.visible) {
            toolbarView = null;
        } else {
            toolbarView = new GToolbarView();
            getFormLayout().add(toolbar, toolbarView);
        }
    }

    public GFormLayout getFormLayout() {
        return formController.formLayout;
    }

    public boolean hasPanelProperty(GPropertyDraw property) {
        return panel.containsProperty(property);
    }

    public void addToToolbar(Widget tool) {
        if (toolbarView != null) {
            toolbarView.addTool(tool);
        }
    }

    public void addFilterButton() {
        if (showFilter()) {
            filter = new GFilterController(this) {
                @Override
                public void remoteApplyQuery() {
                    changeFilter(new ArrayList<>(getConditions()));
                }

                @Override
                public void filterHidden() {
                    focusFirstWidget();
                }
            };

            addToToolbar(filter.getToolbarButton());
        }
    }

    public void setFilterVisible(boolean visible) {
        if (filter != null) {
            filter.setVisible(visible);
        }
    }

    @Override
    public List<GObject> getObjects() {
        return formController.getObjects();
    }

    @Override
    public List<GPropertyDraw> getPropertyDraws() {
        return formController.getPropertyDraws();
    }

    protected boolean showFilter() {
        return true;
    }

    public void quickEditFilter(EditEvent editEvent, GPropertyDraw propertyDraw, GGroupObjectValue columnKey) {
        filter.quickEditFilter(editEvent, propertyDraw, columnKey);
    }

    public void replaceFilter() {
        filter.replaceConditionPressed();
    }

    public void addFilter() {
        filter.addPressed();
    }

    public void removeFilters() {
        filter.allRemovedPressed();
    }

    protected abstract void changeFilter(List<GPropertyFilter> conditions);
    public abstract boolean focusFirstWidget();
    public abstract GComponent getGridComponent();

    @Override
    public void updateFooterValues(GFooterReader reader, Map<GGroupObjectValue, Object> values) {
    }

    // вызов focus() у getFocusHolderElement() грида по какой-то причине приводит к подскролливанию нашего скролла
    // (если грид заключён в скролл и не влезает по высоте) до первого ряда таблицы, скрывая заголовок (видимо вызывается scrollIntoView(), 
    // который, кстати, продолжает вызываться и при последующих изменениях фокуса в IE).
    // поэтому крутим все скроллы-предки вверх при открытии формы.
    // неоднозначное решение, т.к. вовсе необязательно фокусный компонент находится вверху скролла, но пока должно хватать. 
    public void scrollToTop() {
        GComponent gridComponent = getGridComponent();
        if (gridComponent != null) {
            scrollToTop(gridComponent.container);
        }
    }

    private void scrollToTop(GContainer container) {
        if (container != null) {
            if (container.isScroll()) {
                Element childElement = getFormLayout().getFormContainer(container).getView().getElement().getFirstChildElement();
                if (childElement != null && childElement.getScrollTop() != 0) {
                    childElement.setScrollTop(0);
                }
            }
            scrollToTop(container.container);
        }
    }
}
