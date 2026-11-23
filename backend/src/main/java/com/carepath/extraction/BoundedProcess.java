package com.carepath.extraction;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Runs an external program under ONE wall-clock deadline and always leaves nothing running behind it.
 *
 * - The deadline covers start to exit. It is not restarted after stdout closes.
 * - Output is read on a dedicated daemon thread and capped; exceeding the cap kills the process at once.
 * - On timeout, overflow, interruption or error the process and any descendants are killed and reaped.
 * - A non-zero exit status means the output is not trusted.
 */
final class BoundedProcess {

    enum End { OK, TIMED_OUT, OVERFLOW, FAILED }

    record Outcome(End end, byte[] bytes) {
        boolean ok() { return end == End.OK; }
    }

    private static final long KILL_GRACE_MS = 1_000;

    private BoundedProcess() { }

    static Outcome run(List<String> command, Map<String, String> env, Duration timeout, int maxBytes) {
        long deadline = System.nanoTime() + Math.max(0L, timeout.toNanos());
        if (timeout.isZero() || timeout.isNegative()) return new Outcome(End.TIMED_OUT, new byte[0]);

        Process p;
        try {
            ProcessBuilder pb = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD);
            pb.environment().putAll(env);
            p = pb.start();
        } catch (IOException | RuntimeException e) {
            return new Outcome(End.FAILED, new byte[0]);
        }

        byte[][] holder = new byte[1][];
        Thread reader = new Thread(() -> {
            try (var in = p.getInputStream()) {
                holder[0] = in.readNBytes(maxBytes + 1);
            } catch (IOException e) {
                holder[0] = new byte[0];
            }
        }, "bounded-process-reader");
        reader.setDaemon(true);
        reader.start();

        try {
            reader.join(Math.max(1L, TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime())));
            if (reader.isAlive()) { kill(p); return new Outcome(End.TIMED_OUT, new byte[0]); }
            byte[] out = holder[0] == null ? new byte[0] : holder[0];
            if (out.length > maxBytes) { kill(p); return new Outcome(End.OVERFLOW, new byte[0]); }

            long left = deadline - System.nanoTime();
            if (left <= 0 || !p.waitFor(left, TimeUnit.NANOSECONDS)) { kill(p); return new Outcome(End.TIMED_OUT, new byte[0]); }
            if (p.exitValue() != 0) return new Outcome(End.FAILED, new byte[0]);
            return new Outcome(End.OK, out);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            kill(p);
            return new Outcome(End.FAILED, new byte[0]);
        } finally {
            if (p.isAlive()) kill(p);
        }
    }

    /**
     * Kills the process tree and waits (bounded) until it has exited, so the caller knows nothing is still consuming
     * memory or CPU. Descendants are collected BEFORE the parent dies, because afterwards they are re-parented and
     * can no longer be found from it.
     */
    private static void kill(Process p) {
        try {
            List<ProcessHandle> tree = p.descendants().toList();
            tree.forEach(ProcessHandle::destroyForcibly);
            p.destroyForcibly();
            long until = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(KILL_GRACE_MS);
            p.waitFor(KILL_GRACE_MS, TimeUnit.MILLISECONDS);
            for (ProcessHandle h : tree) {
                long left = until - System.nanoTime();
                if (left <= 0) break;
                try { h.onExit().get(left, TimeUnit.NANOSECONDS); } catch (java.util.concurrent.ExecutionException
                        | java.util.concurrent.TimeoutException ignored) { }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException ignored) { }
    }
}
