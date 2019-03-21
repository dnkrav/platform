package lsfusion.gwt.shared.actions.navigator;

import lsfusion.gwt.client.base.result.VoidResult;

public class InterruptNavigator extends NavigatorAction<VoidResult> {
    public boolean cancelable;

    public InterruptNavigator() {
    }

    public InterruptNavigator(boolean cancelable) {
        this.cancelable = cancelable;
    }

}