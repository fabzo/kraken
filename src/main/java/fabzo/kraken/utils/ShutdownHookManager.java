package fabzo.kraken.utils;

import io.netty.util.internal.ObjectUtil;

import java.util.PriorityQueue;

public class ShutdownHookManager {
    private static final PriorityQueue<ShutdownHookEntry> shutdownHooks = new PriorityQueue<>((o1, o2) -> Integer.compare(o2.priority, o1.priority));

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(ShutdownHookManager::executeShutdownHooks));
    }

    private static void executeShutdownHooks() {
        synchronized (shutdownHooks) {
            ShutdownHookEntry entry;
            while ((entry = shutdownHooks.poll()) != null) {
                entry.hook.run();
            }
        }
    }

    public static void addHook(final int priority, final Runnable shutdownHook) {
        ObjectUtil.checkNotNull(shutdownHook, "Shutdown hook should not be null");
        synchronized (shutdownHooks) {
            shutdownHooks.add(new ShutdownHookEntry(priority, shutdownHook));
        }
    }

    public static class ShutdownHookEntry {
        public int priority;
        public Runnable hook;

        public ShutdownHookEntry(final int priority, final Runnable hook) {
            this.priority = priority;
            this.hook = hook;
        }
    }
}
