import org.blade.lib.*;

@BladeInject("org.test.Test")
public abstract class Test {
    @BladeSwap("getString")
    public abstract String getStringOrigin(int resId);

    private String getString(int resId) {
        return null;
    }
}
