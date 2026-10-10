package me.collective.setting;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class Setting<T> {
    private final String name;
    private T value;
    private final Predicate<Boolean> visible;
    private Consumer<T> changeListener;

    public Setting(String name, T defaultValue) {
        this(name, defaultValue, null);
    }

    public Setting(String name, T defaultValue, Predicate<Boolean> visible) {
        this.name = name;
        this.value = defaultValue;
        this.visible = visible;
    }

    public String getName() {
        return name;
    }

    public T getValue() {
        return value;
    }

    public T get() {
        return getValue();
    }

    public void set(T val) {
        setValue(val);
    }

    public void setValue(T value) {
        if (java.util.Objects.equals(this.value, value)) {
            return;
        }
        this.value = value;
        if (changeListener != null) {
            changeListener.accept(value);
        }
    }

    public void setValueQuietly(T value) {
        this.value = value;
    }

    public boolean isVisible() {
        return visible == null || visible.test(true);
    }

    public Setting<T> setChangeListener(Consumer<T> listener) {
        this.changeListener = listener;
        return this;
    }
}
