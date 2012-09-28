package platform.gwt.form2.client.navigator;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.user.client.ui.CellPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import platform.gwt.form2.shared.view.GNavigatorElement;
import platform.gwt.form2.shared.view.panel.ImageButton;
import platform.gwt.form2.shared.view.window.GToolbarNavigatorWindow;

import java.util.Set;

public class GToolbarNavigatorView extends GNavigatorView {
    private static final int PADDING_STEP = 15;
    private CellPanel panel;
    private boolean vertical;

    public GToolbarNavigatorView(GToolbarNavigatorWindow window, GINavigatorController navigatorController) {
        super(window, navigatorController);
        vertical = window.type == 1;
        panel = vertical ? new VerticalPanel() : new HorizontalPanel();
        SimplePanel tollbarContainer = new SimplePanel(panel);
        if (vertical) {
            tollbarContainer.setStyleName("verticaToolbar");
            panel.setWidth("100%");
        } else {
            tollbarContainer.setStyleName("horizontalToolbar");
            panel.setHeight("100%");
        }
        setComponent(tollbarContainer);
    }

    @Override
    public void refresh(Set<GNavigatorElement> newElements) {
        panel.clear();

        for (GNavigatorElement element : newElements) {
            if (!element.containsParent(newElements)) {
                addElement(element, newElements, 0);
            }
        }
    }

    private void addElement(final GNavigatorElement element, Set<GNavigatorElement> newElements, int step) {
        ImageButton button = new ImageButton(element.caption, element.icon, !vertical);
        button.addMouseDownHandler(new MouseDownHandler() {
            @Override
            public void onMouseDown(MouseDownEvent event) {
                selected = element;
                navigatorController.update();
                navigatorController.openElement(element);
            }
        });

        if (vertical) {
            button.setWidth("100%");
        } else {
            button.setHeight("100%");
        }
        button.addStyleName("toolbarNavigatorButton");
        button.getElement().getStyle().setPaddingLeft(7 + PADDING_STEP * step, Style.Unit.PX);

        panel.add(button);

        if (vertical) {
            panel.setCellWidth(button, "100%");
        } else {
            panel.setCellHeight(button, "100%");
        }

        if ((element.window != null) && (!element.window.equals(window))) {
            return;
        }
        for (GNavigatorElement childEl: element.children) {
            if (newElements.contains(childEl)) {
                addElement(childEl, newElements, step + 1);
            }
        }
    }

    @Override
    public GNavigatorElement getSelectedElement() {
        return selected;
    }

    @Override
    public int getHeight() {
        return panel.getOffsetHeight();
    }

    @Override
    public int getWidth() {
        return panel.getOffsetWidth();
    }
}
