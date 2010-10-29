package platform.client.form.editor;

import platform.interop.ComponentDesign;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

public class StringPropertyEditor extends TextFieldPropertyEditor {

    public StringPropertyEditor(final int length, Object value, ComponentDesign design) {
        super(design);

        setDocument(new PlainDocument() {

            public void insertString(int offset, String  str, AttributeSet attr) throws BadLocationException {

                if (str == null) return;

                if ((getLength() + str.length()) <= length)
                    super.insertString(offset, str, attr);
            }
        });

        if (value != null) {
            setText(value.toString());
        }
    }

    public Object getCellEditorValue() {
        if (getText().isEmpty()) return null;
        return getText();
    }


}