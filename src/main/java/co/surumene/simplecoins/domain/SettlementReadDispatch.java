package co.surumene.simplecoins.domain;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/** A single worker's bounded, read-only rendezvous with the owning primary thread.
 * No queue, retry, main-thread wait, or authority over writer coverage. Late callbacks
 * are canceled before execution; an already-running read cannot publish its result. */
public final class SettlementReadDispatch implements AutoCloseable {
    @FunctionalInterface public interface Read<T> { T run() throws IOException; }
    private final Thread caller=Thread.currentThread();
    private final Executor primary;
    private final BooleanSupplier onPrimary;
    private final long deadline;
    private final AtomicBoolean closed=new AtomicBoolean();
    private final AtomicReference<FutureTask<?>> pending=new AtomicReference<>();
    private SettlementReadDispatch(Executor primary,BooleanSupplier onPrimary,long deadline) {
        this.primary=primary; this.onPrimary=onPrimary; this.deadline=deadline;
    }
    /** Legacy synchronous path; individual Paper APIs still enforce their affinity. */
    public static SettlementReadDispatch direct() {return new SettlementReadDispatch(null,null,0);}
    public static SettlementReadDispatch bounded(Executor primary,BooleanSupplier onPrimary,long timeoutMillis) {
        Objects.requireNonNull(primary); Objects.requireNonNull(onPrimary);
        if(onPrimary.getAsBoolean() || timeoutMillis<1 || timeoutMillis>300_000)
            throw new IllegalArgumentException("Bounded read rendezvous requires worker thread and valid timeout");
        return new SettlementReadDispatch(primary,onPrimary,System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(timeoutMillis));
    }
    public boolean isDispatched() {return primary!=null;}
    public void requireCurrent() throws IOException {
        if(Thread.currentThread()!=caller) throw new IllegalStateException("Foreign settlement worker");
        if(closed.get() || Thread.currentThread().isInterrupted() || (primary!=null && System.nanoTime()-deadline>=0)) {
            close(); throw new IOException("Settlement read deadline or cancellation");
        }
    }
    public <T> T call(Read<T> read) throws IOException {
        Objects.requireNonNull(read); requireCurrent();
        if(primary==null) return read.run();
        if(onPrimary.getAsBoolean()) throw new IllegalStateException("Primary cannot await its own read");
        var task=new FutureTask<T>(()->{
            if(closed.get() || System.nanoTime()-deadline>=0) throw new IOException("Late settlement read");
            if(!onPrimary.getAsBoolean() || Thread.currentThread()==caller) throw new IOException("Wrong primary callback thread");
            return read.run();
        });
        if(!pending.compareAndSet(null,task)) throw new IllegalStateException("Another primary read is outstanding");
        try {
            requireCurrent(); primary.execute(task);
            long remaining=deadline-System.nanoTime();
            if(remaining<=0) throw new TimeoutException("Settlement read deadline");
            T result=task.get(remaining,TimeUnit.NANOSECONDS);
            requireCurrent(); return result;
        } catch(InterruptedException failure) {
            Thread.currentThread().interrupt(); close(); throw new IOException("Settlement read interrupted",failure);
        } catch(TimeoutException failure) {
            close(); throw new IOException("Settlement primary read timed out",failure);
        } catch(ExecutionException failure) {
            close(); Throwable cause=failure.getCause();
            if(cause instanceof IOException io) throw io;
            if(cause instanceof RuntimeException runtime) throw runtime;
            if(cause instanceof Error error) throw error;
            throw new IOException("Settlement primary read failed",cause);
        } catch(RuntimeException | Error failure) {close(); throw failure;}
        finally {pending.compareAndSet(task,null);}
    }
    @Override public void close() {
        closed.set(true); FutureTask<?> task=pending.get(); if(task!=null) task.cancel(false);
    }
}
