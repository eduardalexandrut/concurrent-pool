package app.bench;

import app.model.Physics;
import app.model.entities.game.Ball;
import app.model.entities.game.Board;
import app.model.entities.physics.P2d;
import app.model.entities.physics.V2d;
import app.model.physics.executor.ExecutorPhysics;
import app.model.physics.multi_thread.MultiThreadPhysics;
import app.model.physics.sequential.SequentialPhysics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Headless benchmark of computeState(16), without GUI and without the sleep of the game loop.
 *
 * Usage: java app.bench.Benchmark engine balls threads grid [warmup] [frames]
 *   engine = seq | mt | exec, grid = number of rows and columns (e.g. 20).
 * Prints one CSV line: engine,balls,threads,grid,mean,sd,p95,p99,max,fps,under16
 */
public final class Benchmark {

    public static void main(String[] args) {
        String engine = args[0];
        int numBalls = Integer.parseInt(args[1]);
        int nThreads = Integer.parseInt(args[2]);
        int grid = Integer.parseInt(args[3]);
        int warmup = args.length > 4 ? Integer.parseInt(args[4]) : 300;
        int frames = args.length > 5 ? Integer.parseInt(args[5]) : 1000;

        // same table for every engine: fixed seed
        Random rand = new Random(42);
        List<Ball> balls = new ArrayList<>();
        for (int i = 0; i < numBalls; i++) {
            double x = 20 + rand.nextDouble() * 1560;
            double y = 20 + rand.nextDouble() * 1160;
            balls.add(new Ball(new P2d(x, y), 5, 1.0,
                    new V2d(rand.nextDouble() * 4 - 2, rand.nextDouble() * 4 - 2)));
        }
        Board board = new Board(1600, 1200, balls);

        Physics physics;
        switch (engine) {
            case "seq":  physics = new SequentialPhysics(board, grid, grid); break;
            case "mt":   physics = new MultiThreadPhysics(board, grid, grid, nThreads); break;
            case "exec": physics = new ExecutorPhysics(board, grid, grid); break;
            default: throw new IllegalArgumentException(engine);
        }

        for (int i = 0; i < warmup; i++) {          // JIT warm-up, not measured
            physics.computeState(16);
        }
        long[] times = new long[frames];
        for (int i = 0; i < frames; i++) {
            long start = System.nanoTime();
            physics.computeState(16);
            times[i] = System.nanoTime() - start;
        }
        int remaining = physics.getStateSnapshot().size();   // use the result (dead code)
        physics.shutdown();

        double[] ms = Arrays.stream(times).mapToDouble(t -> t / 1e6).sorted().toArray();
        double mean = Arrays.stream(ms).average().orElse(0);
        double sd = Math.sqrt(Arrays.stream(ms).map(v -> (v - mean) * (v - mean)).sum() / ms.length);
        double under = 100.0 * Arrays.stream(ms).filter(v -> v <= 16.6).count() / ms.length;
        System.out.printf(java.util.Locale.ROOT, "%s,%d,%d,%d,%.3f,%.3f,%.3f,%.3f,%.3f,%.1f,%.1f,%d%n",
                engine, numBalls, nThreads, grid, mean, sd,
                ms[(int) (ms.length * 0.95)], ms[(int) (ms.length * 0.99)], ms[ms.length - 1],
                1000 / mean, under, remaining);
    }
}
