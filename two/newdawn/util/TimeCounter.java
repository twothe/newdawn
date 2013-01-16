/*
 * Copyright (c) by Stefan Feldbinder aka Two
 */
package two.newdawn.util;

/**
 *
 * @author Two
 */
public class TimeCounter {

  final String what;
  long timeStart;
  double calls, timeTotal, timeMax, timeMin;

  public TimeCounter(final String what) {
    this.what = what;
    this.timeMax = -1;
    this.timeMin = -1;
  }

  public void start() {
    timeStart = System.nanoTime();
  }

  public void stop() {
    ++calls;
    final double timeTaken = ((double) (System.nanoTime() - timeStart)) / 1000000.0;
    timeTotal += timeTaken;
    this.timeMax = this.timeMax == -1 ? timeTaken : Math.max(timeTaken, this.timeMax);
    this.timeMin = this.timeMin == -1 ? timeTaken : Math.min(timeTaken, this.timeMin);
  }

  @Override
  public String toString() {
    return String.format("[%s]: Time [avg]: %3.2f ms, [min]: %3.2f ms, [max]: %3.2f ms", what, timeTotal / calls, this.timeMin, this.timeMax);
  }
}
