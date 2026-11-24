package co.surumene.simplecoins.domain;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public final class SettlementReadDispatchChecks {
    private static void check(boolean value) { if(!value) throw new AssertionError("Read dispatch contract"); }
    private interface Action {void run() throws Exception;}
    private static void rejected(Action action) throws Exception {
        try {action.run();} catch(IOException | IllegalStateException expected){return;}
        throw new AssertionError("Unsafe primary read accepted");
    }
    public static void run() throws Exception {
        var primary=new AtomicReference<Thread>();
        var executor=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r);primary.set(t);return t;});
        try {
            var dispatch=SettlementReadDispatch.bounded(executor,()->Thread.currentThread()==primary.get(),5000);
            check(dispatch.call(()->Thread.currentThread())==primary.get());
            check(dispatch.call(()->42)==42);
            var foreign=Executors.newSingleThreadExecutor();
            try {foreign.submit(()->{rejected(()->dispatch.call(()->0));return null;}).get(5,TimeUnit.SECONDS);} finally {foreign.shutdownNow();}
            dispatch.close(); rejected(()->dispatch.call(()->0));
            var failure=SettlementReadDispatch.bounded(executor,()->Thread.currentThread()==primary.get(),5000);
            rejected(()->failure.call(()->{throw new IOException("source changed");}));
            rejected(()->failure.call(()->1));
            // Queue but never run a primary task until after timeout. A late task
            // must not call the source or leave a reusable observation.
            var late=new AtomicReference<Runnable>(); var ran=new AtomicBoolean();
            var timeout=SettlementReadDispatch.bounded(late::set,()->false,30);
            rejected(()->timeout.call(()->{ran.set(true);return 1;}));
            if(late.get()!=null) late.get().run(); check(!ran.get()); rejected(()->timeout.call(()->2));
            var wrong=SettlementReadDispatch.bounded(Runnable::run,()->false,1000);
            rejected(()->wrong.call(()->{throw new AssertionError("Wrong thread callback");}));
        } finally {executor.shutdownNow();}
    }
    public static void main(String[] args) throws Exception {run();System.out.println("SETTLEMENT_READ_DISPATCH_CHECKS_PASSED");}
}
