package platform.fullclient.layout;

import bibliothek.gui.dock.common.*;
import bibliothek.gui.dock.common.event.CDockableAdapter;
import bibliothek.gui.dock.common.intern.CDockable;
import net.sf.jasperreports.engine.JRException;
import platform.client.navigator.ClientNavigator;
import platform.interop.form.RemoteFormInterface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class ViewManager {
    private CControl control;
    private FormRepository forms;

    List<FormDockable> pages = new ArrayList<FormDockable>();

    private FormFactory pageFactory;

    private CGridArea gridArea;

    public ViewManager(CControl control, ClientNavigator mainNavigator) {
        this.control = control;
        forms = new FormRepository();

        pageFactory = new FormFactory(mainNavigator);
        control.addMultipleDockableFactory("page", pageFactory);
        gridArea = control.createGridArea("Form area");
        gridArea.setVisible(true);
    }

    public FormRepository getForms() {
        return forms;
    }

    public CGridArea getGridArea() {
        return gridArea;
    }

    private void openForm(FormDockable page) {
        page.addCDockableStateListener(new CDockableStateAdapter(page));
        page.setLocation(gridArea.getStationLocation());
        control.add(page);
        page.setVisible(true);
    }

    public void openClient(int formID, ClientNavigator navigator, boolean currentSession) throws IOException, ClassNotFoundException, JRException {
        openForm(new ClientFormDockable(formID, navigator, currentSession, pageFactory));
    }

    public void openClient(ClientNavigator navigator, RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException, JRException {
        openForm(new ClientFormDockable(navigator, remoteForm, pageFactory));
    }

    public void openReport(int formID, ClientNavigator navigator, boolean currentSession) throws IOException, ClassNotFoundException {
        openForm(new ReportDockable(formID, navigator, currentSession, pageFactory));
    }

    public void openReport(ClientNavigator navigator, RemoteFormInterface remoteForm) throws IOException, ClassNotFoundException {
        openForm(new ReportDockable(navigator, remoteForm, pageFactory));
    }

    public void openReport(String fileName, String directory) throws JRException {
        openForm(new ReportDockable(fileName, directory, pageFactory));
    }

    private class FormFactory implements MultipleCDockableFactory<FormDockable, FormLayout> {
        ClientNavigator mainNavigator;

        public FormFactory(ClientNavigator mainNavigator) {
            this.mainNavigator = mainNavigator;
        }

        public FormLayout create() {
            return new FormLayout();
        }

        public FormDockable read(FormLayout layout) {
            try {
                FormDockable page = new ClientFormDockable(layout.getFormID(), this, mainNavigator);
                page.addCDockableStateListener(new CDockableStateAdapter(page));
                return page;
            }
            catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        public FormLayout write(FormDockable dockable) {
            FormLayout layout = new FormLayout();
            layout.setFormID(dockable.getFormID());
            return layout;
        }

        public boolean match(FormDockable dockable, FormLayout layout) {
            return false;
        }
    }

    private class CDockableStateAdapter extends CDockableAdapter {
        private FormDockable page;

        public CDockableStateAdapter(FormDockable page) {
            this.page = page;
        }

        @Override
        public void visibilityChanged(CDockable dockable) {
            if (dockable.isVisible()) {
                forms.add(page.getFormID());
                pages.add(page);
            } else {
                pages.remove(page);
                forms.remove(page.getFormID());
                control.remove(page);
                page.closed();
            }
        }
    }
}
