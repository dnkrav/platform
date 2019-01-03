package lsfusion.base;

import org.jfree.ui.ExtensionFileFilter;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static lsfusion.base.ApiResourceBundle.getString;

public class FileDialogUtils {

    public static void showSaveFileDialog(String path, RawFileData file) {
        showSaveFileDialog(getFileMap(path, file));
    }

    public static void showSaveFileDialog(Map<String, RawFileData> files) {
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(SystemUtils.loadCurrentDirectory());
            boolean singleFile;
            fileChooser.setAcceptAllFileFilterUsed(false);
            if (files.size() > 1) {
                singleFile = false;
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            } else {
                singleFile = true;
                File file = new File(files.keySet().iterator().next());
                fileChooser.setSelectedFile(file);
                String extension = BaseUtils.getFileExtension(file);
                if (!BaseUtils.isRedundantString(extension)) {
                    ExtensionFileFilter filter = new ExtensionFileFilter("." + extension, extension);
                    fileChooser.addChoosableFileFilter(filter);
                }
            }
            if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                String path = fileChooser.getSelectedFile().getAbsolutePath();
                for (String file : files.keySet()) {
                    if (singleFile) {
                        File file1 = new File(path);
                        if (file1.exists()) {
                            int answer = showConfirmDialog(fileChooser, getString("layout.menu.file.already.exists.replace"),
                                    getString("layout.menu.file.already.exists"), JOptionPane.QUESTION_MESSAGE, false);
                            if (answer == JOptionPane.YES_OPTION) {
                                files.get(file).write(file1);
                            }
                        } else {
                            files.get(file).write(file1);
                        }
                    } else {
                        files.get(file).write(new File(path + "\\" + file));
                    }
                }
                SystemUtils.saveCurrentDirectory(!singleFile ? new File(path) : new File(path.substring(0, path.lastIndexOf("\\"))));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, RawFileData> getFileMap(String path, RawFileData file) {
        Map<String, RawFileData> result = new HashMap<>();
        result.put(path, file);
        return result;
    }

    public static int showConfirmDialog(Component parentComponent, Object message, String title, int messageType, boolean cancel) {
        return showConfirmDialog(parentComponent, message, title, messageType, 0, cancel, 0);
    }

    public static int showConfirmDialog(Component parentComponent, Object message, String title, int messageType, int initialValue,
                                        boolean cancel, int timeout) {

        Object[] options = {UIManager.getString("OptionPane.yesButtonText"),
                UIManager.getString("OptionPane.noButtonText")};
        if (cancel) {
            options = BaseUtils.add(options, UIManager.getString("OptionPane.cancelButtonText"));
        }

        JOptionPane dialogPane = new JOptionPane(message,
                messageType,
                cancel ? JOptionPane.YES_NO_CANCEL_OPTION : JOptionPane.YES_NO_OPTION,
                null, options, options[initialValue]);

        addFocusTraversalKey(dialogPane, KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, KeyStroke.getKeyStroke("RIGHT"));
        addFocusTraversalKey(dialogPane, KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, KeyStroke.getKeyStroke("UP"));
        addFocusTraversalKey(dialogPane, KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, KeyStroke.getKeyStroke("LEFT"));
        addFocusTraversalKey(dialogPane, KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, KeyStroke.getKeyStroke("DOWN"));

        final JDialog dialog = dialogPane.createDialog(parentComponent, title);
        if (timeout != 0) {
            final java.util.Timer timer = new java.util.Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    timer.cancel();
                    dialog.setVisible(false);
                }
            }, timeout);
        }
        dialog.setVisible(true);

        if (dialogPane.getValue() == JOptionPane.UNINITIALIZED_VALUE)
            return initialValue;
        if (dialogPane.getValue() == options[0]) {
            return JOptionPane.YES_OPTION;
        } else {
            if (!cancel || dialogPane.getValue() == options[1])
                return JOptionPane.NO_OPTION;
            else
                return JOptionPane.CANCEL_OPTION;
        }
    }

    public static void addFocusTraversalKey(Component comp, int id, KeyStroke key) {
        Set keys = comp.getFocusTraversalKeys(id);
        Set newKeys = new HashSet(keys);
        newKeys.add(key);
        comp.setFocusTraversalKeys(id, newKeys);
    }
}