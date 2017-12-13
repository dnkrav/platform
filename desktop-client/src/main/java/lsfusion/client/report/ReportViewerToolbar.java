package lsfusion.client.report;

import com.google.common.base.Throwables;
import lsfusion.base.SystemUtils;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.EditReportInvoker;
import lsfusion.client.Main;
import lsfusion.client.SwingUtils;
import lsfusion.client.exceptions.ClientExceptionManager;
import lsfusion.client.form.RmiQueue;
import net.sf.jasperreports.swing.JRViewerController;
import net.sf.jasperreports.swing.JRViewerToolbar;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.rmi.RemoteException;

public class ReportViewerToolbar extends JRViewerToolbar {
    private JButton editReportButton = null;
    private JButton addReportButton = null;
    private JButton deleteReportButton = null;
    private Boolean hasCustomReports;
    
    public ReportViewerToolbar(JRViewerController viewerContext) {
        super(viewerContext);

        lastFolder = SystemUtils.loadCurrentDirectory();

        final ActionListener[] al = btnSave.getActionListeners();

        btnSave.removeActionListener(al[0]);

        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SystemUtils.saveCurrentDirectory(lastFolder);
            }
        });

        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                try {
                    al[0].actionPerformed(evt);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(getParent(),
                            String.format("Произошла ошибка при сохранении файла: \n%s", e.getMessage()), "Ошибка", JOptionPane.ERROR_MESSAGE);
                    ClientExceptionManager.reportClientThrowable(e);
                }
            }
        });
    }
    
    protected void modify(final String printerName, EditReportInvoker editInvoker) {
        if(printerName != null) {
            btnPrint.removeActionListener(btnPrint.getActionListeners()[0]);
            btnPrint.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    PrintService[] printers = PrintServiceLookup.lookupPrintServices(null, null);
                    PrintService printer = null;
                    for (PrintService p : printers) {
                        if (p.getName().equals(printerName)) {
                            printer = p;
                            break;
                        }
                    }
                    try {
                        PrinterJob pj = PrinterJob.getPrinterJob();
                        pj.setPrintService(printer != null ? printer : PrintServiceLookup.lookupDefaultPrintService());
                        pj.printDialog();
                    } catch (PrinterException e1) {
                        e1.printStackTrace();
                    }

                }
            });
        }

        if (editInvoker != null) {
            try {
                hasCustomReports = editInvoker.hasCustomReports();
            } catch (RemoteException e) {
                throw Throwables.propagate(e);
            }

            add(Box.createHorizontalStrut(10));

            editReportButton = getEditReportButton(editInvoker, hasCustomReports);
            deleteReportButton = getDeleteReportButton(editInvoker, hasCustomReports);
            addReportButton = getAddReportButton(editInvoker);

            add(editReportButton);
            add(addReportButton);
            add(deleteReportButton);
        }
    }

    private JButton getEditReportButton(final EditReportInvoker editInvoker, boolean visible) {
        JButton editReportButton = new JButton(new ImageIcon(Main.class.getResource("/images/editReport.png")));
        editReportButton.setToolTipText(ClientResourceBundle.getString("layout.menu.file.edit.report"));
        editReportButton.setMargin(new Insets(2, 2, 2, 2));
        editReportButton.setMaximumSize(new Dimension(23, 23));
        editReportButton.setMinimumSize(new Dimension(23, 23));
        editReportButton.setPreferredSize(new Dimension(23, 23));
        editReportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RmiQueue.runAction(new Runnable() {
                    @Override
                    public void run() {
                        editButtonPressed(editInvoker);
                    }
                });
            }
        });
        editReportButton.setVisible(visible);
        return editReportButton;
    }

    private void editButtonPressed(EditReportInvoker editInvoker) {
        try {
            editInvoker.invokeEditReport(false);
        } catch (RemoteException e) {
            throw Throwables.propagate(e);
        }
    }

    private JButton getAddReportButton(final EditReportInvoker editInvoker) {
        final JButton addReportButton = new JButton(new ImageIcon(Main.class.getResource("/images/editAutoReport.png")));
        addReportButton.setToolTipText(ClientResourceBundle.getString("layout.menu.file.edit.auto.report"));
        addReportButton.setMargin(new Insets(2, 2, 2, 2));
        addReportButton.setMaximumSize(new Dimension(23, 23));
        addReportButton.setMinimumSize(new Dimension(23, 23));
        addReportButton.setPreferredSize(new Dimension(23, 23));
        addReportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RmiQueue.runAction(new Runnable() {
                    @Override
                    public void run() {
                        addButtonPressed(editInvoker);
                    }
                });
            }
        });
        return addReportButton;
    }

    private void addButtonPressed(EditReportInvoker editInvoker) {
        if(!hasCustomReports || SwingUtils.showConfirmDialog(this, ClientResourceBundle.getString("layout.menu.file.edit.auto.report.confirm"),
                ClientResourceBundle.getString("layout.menu.file.edit.auto.report"), JOptionPane.WARNING_MESSAGE, false) == 0) {
            try {
                editInvoker.invokeEditReport(true);
            } catch (RemoteException e) {
                throw Throwables.propagate(e);
            }
            editReportButton.setVisible(true);
            deleteReportButton.setVisible(true);
            hasCustomReports = true;
            invalidate();
        }
    }

    private JButton getDeleteReportButton(final EditReportInvoker editInvoker, final boolean visible) {
        final JButton deleteReportButton = new JButton(new ImageIcon(Main.class.getResource("/images/deleteReport.png")));
        deleteReportButton.setToolTipText(ClientResourceBundle.getString("layout.menu.file.delete.report"));
        deleteReportButton.setMargin(new Insets(2, 2, 2, 2));
        deleteReportButton.setMaximumSize(new Dimension(23, 23));
        deleteReportButton.setMinimumSize(new Dimension(23, 23));
        deleteReportButton.setPreferredSize(new Dimension(23, 23));
        deleteReportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RmiQueue.runAction(new Runnable() {
                    @Override
                    public void run() {
                        deleteButtonPressed(editInvoker);
                    }
                });
            }
        });
        deleteReportButton.setVisible(visible);
        return deleteReportButton;
    }

    private void deleteButtonPressed(EditReportInvoker editInvoker) {
        if(SwingUtils.showConfirmDialog(this, ClientResourceBundle.getString("layout.menu.file.delete.report.confirm"),
                ClientResourceBundle.getString("layout.menu.file.delete.report"), JOptionPane.WARNING_MESSAGE, false) == 0) {
            try {
                editInvoker.invokeDeleteReport();
            } catch (RemoteException e) {
                throw Throwables.propagate(e);
            }
            editReportButton.setVisible(false);
            deleteReportButton.setVisible(false);
            hasCustomReports = false;
            invalidate();
        }
    }

    public void clickBtnPrint() {
        btnPrint.doClick();
    }
}