package com.github.sszuev;

import java.io.PrintStream;
import java.util.function.Consumer;

/**
 * A simple leveled logger.
 * <p>
 * Created by @szuev on 13.01.2018.
 */
@SuppressWarnings("WeakerAccess")
public class Logs {

    private final Level level;
    private final Consumer<String> delegate;

    public Logs(Level level, Consumer<String> printer) {
        this.level = level;
        this.delegate = printer;
    }

    public boolean isTraceEnabled() {
        return isGreaterOrEqual(Level.TRACE);
    }

    public boolean isDebugEnabled() {
        return isGreaterOrEqual(Level.DEBUG);
    }

    public boolean isInfoEnabled() {
        return isGreaterOrEqual(Level.INFO);
    }

    public boolean isErrorEnabled() {
        return isGreaterOrEqual(Level.ERROR);
    }

    private boolean isGreaterOrEqual(Level other) {
        return this.level.compareTo(other) >= 0;
    }

    protected void println(String msg) {
        if (msg == null) return;
        delegate.accept(msg);
    }

    public boolean isEnabled() {
        return Level.NONE != level;
    }

    public void trace(String msg) {
        if (!isTraceEnabled()) return;
        println(msg);
    }

    public void debug(String msg) {
        if (!isDebugEnabled()) return;
        println(msg);
    }

    public void info(String msg) {
        if (!isInfoEnabled()) return;
        println(msg);
    }

    public void error(String msg) {
        if (!isErrorEnabled()) return;
        println(msg);
    }

    public Logs increase() {
        return toLevel(Level.at(level.ordinal() + 1));
    }

    public Logs decrease() {
        return toLevel(Level.at(level.ordinal() - 1));
    }

    public Logs toLevel(Level level) {
        return new Logs(level, delegate);
    }

    public enum Level {
        NONE, ERROR, INFO, DEBUG, TRACE, ALL,;

        public static Level at(int index) {
            try {
                return values()[index];
            } catch (IndexOutOfBoundsException e) {
                return index > 0 ? ALL : NONE;
            }
        }

        public Logs create(PrintStream out) {
            return new Logs(this, out::println);
        }
    }

}