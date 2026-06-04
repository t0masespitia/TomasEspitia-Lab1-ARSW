package snake.concurrency;

import snake.core.Board;
import snake.core.Direction;
import snake.core.Snake;

import java.util.concurrent.ThreadLocalRandom;

public final class SnakeRunner implements Runnable {
  private final Snake snake;
  private final Board board;
  private final int baseSleepMs = 80;
  private final int turboSleepMs = 40;
  private int turboTicks = 0;
  private int consecutiveHits = 0;

  public SnakeRunner(Snake snake, Board board) {
    this.snake = snake;
    this.board = board;
  }

  @Override
  public void run() {
    board.registerRunner();
    try {
      while (!Thread.currentThread().isInterrupted()) {
        board.checkPause();
        maybeTurn();
        var res = board.step(snake);
        if (res == Board.MoveResult.HIT_OBSTACLE) {
          consecutiveHits++;
          randomTurn();
          if (consecutiveHits >= 5) {
            snake.kill();
            return;
          }
        } else {
          consecutiveHits = 0;
          if (res == Board.MoveResult.ATE_TURBO) {
            turboTicks = 100;
          }
        }
        int sleep = (turboTicks > 0) ? turboSleepMs : baseSleepMs;
        if (turboTicks > 0) turboTicks--;
        Thread.sleep(sleep);
      }
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    } finally {
      board.unregisterRunner();
    }
  }

  private void maybeTurn() {
    double p = (turboTicks > 0) ? 0.05 : 0.10;
    if (ThreadLocalRandom.current().nextDouble() < p) randomTurn();
  }

  private void randomTurn() {
    var dirs = Direction.values();
    snake.turn(dirs[ThreadLocalRandom.current().nextInt(dirs.length)]);
  }
}
