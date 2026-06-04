package snake.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class Board {
  private final int width;
  private final int height;

  private final Set<Position> mice = new HashSet<>();
  private final Set<Position> obstacles = new HashSet<>();
  private final Set<Position> turbo = new HashSet<>();
  private final Map<Position, Position> teleports = new HashMap<>();
  private boolean paused = false;
  private int activeRunners = 0;
  private int waitingRunners = 0;

  public enum MoveResult { MOVED, ATE_MOUSE, HIT_OBSTACLE, ATE_TURBO, TELEPORTED }

  public Board(int width, int height) {
    if (width <= 0 || height <= 0) throw new IllegalArgumentException("Board dimensions must be positive");
    this.width = width;
    this.height = height;
    for (int i=0;i<6;i++) mice.add(randomEmpty());
    for (int i=0;i<4;i++) obstacles.add(randomEmpty());
    for (int i=0;i<3;i++) turbo.add(randomEmpty());
    createTeleportPairs(2);
  }

  public int width() { return width; }
  public int height() { return height; }

  public synchronized Set<Position> mice() { return new HashSet<>(mice); }
  public synchronized Set<Position> obstacles() { return new HashSet<>(obstacles); }
  public synchronized Set<Position> turbo() { return new HashSet<>(turbo); }
  public synchronized Map<Position, Position> teleports() { return new HashMap<>(teleports); }

  public synchronized void registerRunner() {
    activeRunners++;
  }

  public synchronized void unregisterRunner() {
    activeRunners--;
    notifyAll();
  }

  public synchronized void pauseGame() {
    paused = true;
  }

  public synchronized void resumeGame() {
    paused = false;
    notifyAll();
  }

  public synchronized void checkPause() {
    while (paused) {
      waitingRunners++;
      try {
        wait();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } finally {
        waitingRunners--;
      }
    }
  }

  public synchronized void waitUntilAllPaused() {
    while (waitingRunners < activeRunners) {
      try {
        wait(10);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  public MoveResult step(Snake snake) {
    Objects.requireNonNull(snake, "snake");

    Position head = snake.head();
    Direction dir = snake.direction();
    Position next = new Position(head.x() + dir.dx, head.y() + dir.dy).wrap(width, height);

    boolean ateMouse;
    boolean ateTurbo;
    boolean teleported;
    MoveResult result;

    synchronized (this) {
      if (obstacles.contains(next)) return MoveResult.HIT_OBSTACLE;

      teleported = false;
      if (teleports.containsKey(next)) {
        next = teleports.get(next);
        teleported = true;
      }

      ateMouse = mice.remove(next);
      ateTurbo = turbo.remove(next);

      if (ateMouse) {
        mice.add(randomEmpty());
        obstacles.add(randomEmpty());
        if (ThreadLocalRandom.current().nextDouble() < 0.2) turbo.add(randomEmpty());
      }

      if (ateTurbo) result = MoveResult.ATE_TURBO;
      else if (ateMouse) result = MoveResult.ATE_MOUSE;
      else if (teleported) result = MoveResult.TELEPORTED;
      else result = MoveResult.MOVED;
    }

    snake.advance(next, ateMouse);
    return result;
  }

  public static Snake longestAlive(List<Snake> snakes) {
    return snakes.stream()
        .filter(Snake::isAlive)
        .max((a, b) -> Integer.compare(a.length(), b.length()))
        .orElse(null);
  }

  public static Snake firstDead(List<Snake> snakes) {
    return snakes.stream()
        .filter(s -> !s.isAlive())
        .min((a, b) -> Long.compare(a.getDeathTime(), b.getDeathTime()))
        .orElse(null);
  }

  private void createTeleportPairs(int pairs) {
    for (int i=0;i<pairs;i++) {
      Position a = randomEmpty();
      Position b = randomEmpty();
      teleports.put(a, b);
      teleports.put(b, a);
    }
  }

  private Position randomEmpty() {
    var rnd = ThreadLocalRandom.current();
    Position p;
    int guard = 0;
    do {
      p = new Position(rnd.nextInt(width), rnd.nextInt(height));
      guard++;
      if (guard > width*height*2) break;
    } while (mice.contains(p) || obstacles.contains(p) || turbo.contains(p) || teleports.containsKey(p));
    return p;
  }
}
