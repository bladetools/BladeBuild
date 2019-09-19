import org.blade.lib.*;

@BladeInject("org.test.Test")
public abstract class Test {
    @BladeSwap("getString")
    public abstract String getStringOrigin(int resId);

    private String getString(int resId) {
        return null;
    }

    @BladeSwap("getMyString_")
    public abstract String getMyStringOrigin(int resId);

    private String getMyString(int resId) {
        return null;
    }

    private String getMyString_(float f) {
        return null;
    }
}
