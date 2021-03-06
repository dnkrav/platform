package lsfusion.server.physics.dev.debug;

public class PropertyFollowsDebug {
    public final boolean isTrue;
    public final DebugInfo.DebugPoint debugPoint;
    public final boolean isFull;

    public PropertyFollowsDebug(boolean isTrue, DebugInfo.DebugPoint debugPoint) {
        this(isTrue, false, debugPoint);
    }

    public PropertyFollowsDebug(boolean isTrue, boolean isFull, DebugInfo.DebugPoint debugPoint) {
        this.isTrue = isTrue;
        this.isFull = isFull;
        this.debugPoint = debugPoint;
    }
}
