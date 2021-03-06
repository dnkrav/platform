package lsfusion.gwt.client.base.view;

import com.allen_sauer.gwt.dnd.client.DragContext;
import com.allen_sauer.gwt.dnd.client.util.Location;
import com.allen_sauer.gwt.dnd.client.util.WidgetLocation;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.HasCloseHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.base.TooltipManager;

import static lsfusion.gwt.client.base.view.ResizableWindow.Direction.*;

/**
 * based on https://github.com/fredsa/gwt-dnd/blob/master/DragDrop/demo/com/allen_sauer/gwt/dnd/demo/client/example/window/WindowPanel.java
 */
public class ResizableWindow extends Composite implements HasCloseHandlers<ResizableWindow> {

    private static final int MIN_WIDGET_SIZE = 10;

    private static final int BORDER_THICKNESS = 5;

    private static final String RESIZE_EDGE_STYLE = "resize-edge";

    private static final String RESIZABLE_DIALOG_STYLE = "ResizableWindow";

    private static final String RESIZABLE_DIALOG_HEADER_STYLE = "ResizableWindow-header";

    private final WindowDragDropController windowController;

    private FocusPanel mainPanel;

    private Widget contentWidget;

    private HeaderWidget headerWidget;

    private FocusPanel headerPanel;

    private String tooltip;

    private Grid contentGrid;

    private Widget northEdge;

    private Widget southEdge;

    private Widget eastEdge;

    private Widget westEdge;

    private int contentHeight;

    private int contentWidth;

    protected boolean initialOnLoad = true;

    public ResizableWindow() {
        windowController = WindowDragDropController.rootController;
        headerWidget = new HeaderWidget();

        initWidget(mainPanel = new FocusPanel());

        addStyleName(RESIZABLE_DIALOG_STYLE);
    }

    public void setCaption(String caption) {
        headerWidget.setCaption(caption);
    }
    public void setTooltip(String tooltip) {
        this.tooltip = tooltip;
    }

    public void setContentWidget(Widget icontentWidget) {
        if (icontentWidget == null) {
            throw new NullPointerException("Content widget should not be null.");
        }

        if (contentWidget != null) {
            throw new IllegalStateException("Content widget should only be set once");
        }

        contentWidget = icontentWidget;
        initLayout();
        initUIHandlers();
    }

    private void initLayout() {
        headerPanel = new FocusPanel();
        headerPanel.addStyleName(RESIZABLE_DIALOG_HEADER_STYLE);
        headerPanel.add(headerWidget);
        headerPanel.setTabIndex(-1);

        TooltipManager.registerWidget(this, new TooltipManager.TooltipHelper() {
            @Override
            public String getTooltip() {
                return tooltip;
            }

            @Override
            public boolean stillShowTooltip() {
                return headerPanel.isAttached() && headerPanel.isVisible() && !windowController.isDragging();
            }
        });

        VerticalPanel centerPanel = new VerticalPanel();
        centerPanel.add(headerPanel);
        centerPanel.add(contentWidget);

        contentGrid = new Grid(3, 3);
        contentGrid.setCellSpacing(0);
        contentGrid.setCellPadding(0);

        createEdgeWidget(0, 0, NORTH_WEST);
        northEdge = createEdgeWidget(0, 1, NORTH);
        northEdge.getElement().setTabIndex(-1);
        createEdgeWidget(0, 2, NORTH_EAST);

        westEdge = createEdgeWidget(1, 0, WEST);
        westEdge.getElement().setTabIndex(-1);
        contentGrid.setWidget(1, 1, centerPanel);
        eastEdge = createEdgeWidget(1, 2, EAST);
        eastEdge.getElement().setTabIndex(-1);

        createEdgeWidget(2, 0, SOUTH_WEST);
        southEdge = createEdgeWidget(2, 1, SOUTH);
        southEdge.getElement().setTabIndex(-1);
        createEdgeWidget(2, 2, SOUTH_EAST);

        mainPanel.add(contentGrid);
    }

    private Widget createEdgeWidget(int row, int col, Direction direction) {
        final EdgeWidget edgeWidget = new EdgeWidget(direction);
        edgeWidget.setPixelSize(BORDER_THICKNESS, BORDER_THICKNESS);
        edgeWidget.setTabIndex(-1);

        contentGrid.setWidget(row, col, edgeWidget);
        windowController.getResizeDragController().addDraggableEdge(edgeWidget);

        contentGrid.getCellFormatter().addStyleName(row, col, RESIZE_EDGE_STYLE);
        contentGrid.getCellFormatter().addStyleName(row, col, RESIZE_EDGE_STYLE + "-" + direction.getLetters());
        return edgeWidget;
    }

    private void initUIHandlers() {
        windowController.getPickupDragController().makeDraggable(this, headerPanel);
    }

    public int getContentHeight() {
        return contentHeight;
    }

    public int getContentWidth() {
        return contentWidth;
    }

    private void move(int dx, int dy) {
        AbsolutePanel parent = (AbsolutePanel) getParent();
        Location location = new WidgetLocation(this, parent);
        int left = location.getLeft() + dx;
        int top = location.getTop() + dy;
        parent.setWidgetPosition(this, left, top);
    }

    public void setContentSize(int width, int height) {
        if (width != contentWidth) {
            contentWidth = width;
            headerPanel.setPixelSize(contentWidth, headerWidget.getOffsetHeight());
            northEdge.setPixelSize(contentWidth, BORDER_THICKNESS);
            southEdge.setPixelSize(contentWidth, BORDER_THICKNESS);
        }
        if (height != contentHeight) {
            contentHeight = height;
            int headerHeight = headerPanel.getOffsetHeight();
            westEdge.setPixelSize(BORDER_THICKNESS, contentHeight + headerHeight);
            eastEdge.setPixelSize(BORDER_THICKNESS, contentHeight + headerHeight);
        }
        contentWidget.setPixelSize(contentWidth, contentHeight);
        
        if (contentWidget instanceof RequiresResize) {
            ((RequiresResize) contentWidget).onResize();
        }
    }

    @Override
    protected void onLoad() {
        if (initialOnLoad && contentWidget.getOffsetHeight() != 0) {
            initialOnLoad = false;
            headerWidget.setPixelSize(headerWidget.getOffsetWidth(), headerWidget.getOffsetHeight());
            setContentSize(contentWidget.getOffsetWidth(), contentWidget.getOffsetHeight());
        }
    }

    public void hide() {
        windowController.getBoundaryPanel().remove(this);
        CloseEvent.fire(this, this);
    }

    public void center() {
        attach();
        justCenter();
    }

    protected void attach() {
        windowController.getBoundaryPanel().add(this);
    }

    protected void justCenter() {
        int left = (Window.getClientWidth() - getOffsetWidth()) / 2;
        int top = (Window.getClientHeight() - getOffsetHeight()) / 2;

//        left = Math.max(Window.getScrollLeft() + left, 0);
//        top = Math.max(Window.getScrollTop() + top, 0);

        windowController.getBoundaryPanel().setWidgetPosition(this, left, top);
    }

    public HandlerRegistration addCloseHandler(CloseHandler<ResizableWindow> handler) {
        return addHandler(handler, CloseEvent.getType());
    }

    public enum Direction {
        NORTH("n", true, null),
        SOUTH("s", false, null),
        WEST("w", null, true),
        EAST("e", null, false),
        NORTH_WEST("nw", true, true),
        NORTH_EAST("ne", true, false),
        SOUTH_WEST("sw", false, true),
        SOUTH_EAST("se", false, false);

        private final String letters;
        private final Boolean north;
        private final Boolean west;

        Direction(String letters, Boolean north, Boolean west) {
            this.letters = letters;
            this.north = north;
            this.west = west;
        }

        public String getLetters() {
            return letters;
        }

        public boolean isNorthDirection() {
            return north != null && north;
        }

        public boolean isSouthDirection() {
            return north != null && !north;
        }

        public boolean isWestDirection() {
            return west != null && west;
        }

        public boolean isEastDirection() {
            return west != null && !west;
        }
    }

    public static final class HeaderWidget extends HTML {
        public HeaderWidget() {
            super();
        }

        public void setCaption(String caption) {
            setHTML(caption);
        }
    }

    public final class EdgeWidget extends FocusPanel {
        private final Direction direction;

        public EdgeWidget(Direction direction) {
            this.direction = direction;
        }

        public void dragMove(DragContext context) {
            ResizableWindow currentWindow = ResizableWindow.this;

            if (direction.isNorthDirection()) {
                int delta = context.draggable.getAbsoluteTop() - context.desiredDraggableY;
                if (delta != 0) {
                    int contentHeight = currentWindow.getContentHeight();
                    int newHeight = Math.max(contentHeight + delta, MIN_WIDGET_SIZE);
                    if (newHeight != contentHeight) {
                        currentWindow.move(0, contentHeight - newHeight);
                    }
                    currentWindow.setContentSize(currentWindow.getContentWidth(), newHeight);
                }
            } else if (direction.isSouthDirection()) {
                int delta = context.desiredDraggableY - context.draggable.getAbsoluteTop();
                if (delta != 0) {
                    currentWindow.setContentSize(currentWindow.getContentWidth(), currentWindow.getContentHeight() + delta);
                }
            }
            if (direction.isWestDirection()) {
                int delta = context.draggable.getAbsoluteLeft() - context.desiredDraggableX;
                if (delta != 0) {
                    int contentWidth = currentWindow.getContentWidth();
                    int newWidth = Math.max(contentWidth + delta, MIN_WIDGET_SIZE);
                    if (newWidth != contentWidth) {
                        currentWindow.move(contentWidth - newWidth, 0);
                    }
                    currentWindow.setContentSize(newWidth, currentWindow.getContentHeight());
                }
            } else if (direction.isEastDirection()) {
                int delta = context.desiredDraggableX - context.draggable.getAbsoluteLeft();
                if (delta != 0) {
                    currentWindow.setContentSize(currentWindow.getContentWidth() + delta, currentWindow.getContentHeight());
                }
            }
        }
    }
}