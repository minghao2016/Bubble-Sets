package setvis.shape;

import java.awt.geom.Point2D;
import java.util.LinkedList;
import java.util.List;

import setvis.VecUtil;

/**
 * Simplifies shapes by removing points that lie on a line. Given an arbitrary
 * line between two control points of the shape, one can remove all control
 * points in-between if all those points lie beneath the line between the two
 * points. This is a fast implementation of this property by not checking every
 * pair of two points but only those that could possibly lead to a
 * simplification. The class can be used as decorator for shape generators.
 * 
 * @author Joschi <josua.krause@googlemail.com>
 */
public class ShapeSimplifier extends ShapeGeneratorDecorator {

  // the maximum distance where points are regarded as beneath
  private double tolerance;

  /**
   * Creates a shape simplifier that removes points which lie exactly between
   * other points.
   * 
   * @param parent The underlying generator.
   */
  public ShapeSimplifier(final AbstractShapeGenerator parent) {
    this(parent, 0.0);
  }

  /**
   * Creates a shape simplifier that removes points which lie within a certain
   * radius between other points.
   * 
   * @param parent The underlying generator.
   * @param tolerance The radius where points are regarded as near.
   */
  public ShapeSimplifier(final AbstractShapeGenerator parent,
      final double tolerance) {
    super(parent);
    // proper initialization of the tolerance
    setTolerance(tolerance);
  }

  @Override
  public void setRadius(final double radius) {
    // parent is null during initialization
    final AbstractShapeGenerator parent = getParent();
    if(parent != null) {
      parent.setRadius(radius);
    }
  }

  @Override
  public double getRadius() {
    return getParent().getRadius();
  }

  // the square of the tolerance for fast checks
  private double tsqr;

  /**
   * Setter.
   * 
   * @param tolerance Sets the radius where points are regarded as near.
   */
  public void setTolerance(final double tolerance) {
    this.tolerance = tolerance;
    tsqr = tolerance * tolerance;
  }

  /**
   * Getter.
   * 
   * @return The radius where points are regarded as near.
   */
  public double getTolerance() {
    return tolerance;
  }

  /**
   * Getter.
   * 
   * @return The squared radius where points are regarded as near.
   */
  public double getSqrTolerance() {
    return tsqr;
  }

  /**
   * Getter.
   * 
   * @return Whether this simplifier is disabled.
   */
  public boolean isDisabled() {
    return tolerance < 0.0;
  }

  /**
   * A tuple of two points of the shapes original point list.
   * 
   * @author Joschi <josua.krause@googlemail.com>
   */
  private final class State {

    // whether the shape will be drawn closed
    private final boolean closed;

    // the original list of points
    private final Point2D[] list;

    // the start points index
    private final int start;

    // the end points index
    private int end;

    /**
     * Creates a minimal state, i.e. of two directly following points.
     * 
     * @param list The original list of points.
     * @param closed Whether the shape will be drawn closed.
     * @param start The start point. The end point is {@code (start+1)}.
     */
    public State(final Point2D[] list, final boolean closed, final int start) {
      this.list = list;
      this.closed = closed;
      this.start = start;
      end = start + 1;
    }

    /**
     * Advances the end point by one.
     */
    public void advanceEnd() {
      ++end;
    }

    /**
     * Decreases the end point by one.
     */
    public void decreaseEnd() {
      --end;
    }

    /**
     * Getter.
     * 
     * @return Returns the index of the end point.
     */
    public int getEndIndex() {
      return end;
    }

    /**
     * Getter.
     * 
     * @return Whether the end point could be used as start point.
     */
    public boolean validEnd() {
      return closed ? end < list.length : end < list.length - 1;
    }

    /**
     * Getter.
     * 
     * @return The end point.
     */
    public Point2D getEnd() {
      return list[end % list.length];
    }

    /**
     * Getter.
     * 
     * @return The start point.
     */
    public Point2D getStart() {
      return list[start];
    }

    /**
     * Getter.
     * 
     * @param i The index of a point between the points.
     * @return The squared distance of the point to the line.
     */
    private double lineDstSqr(final int i) {
      return VecUtil.distPointLineSqr(getStart(), getEnd(), list[i]);
    }

    /**
     * Getter.
     * 
     * @return Whether the end point can be increased.
     */
    public boolean canTakeNext() {
      if(!validEnd()) return false;
      boolean ok = true;
      advanceEnd();
      for(int i = start + 1; i < end; ++i) {
        if(lineDstSqr(i) > getSqrTolerance()) {
          ok = false;
          break;
        }
      }
      decreaseEnd();
      return ok;
    }

  }

  /**
   * Creator.
   * 
   * @param states The states.
   * @return Creates a list of points from the state.
   */
  private static Point2D[] createArrayFrom(final List<State> states) {
    final Point2D[] res = new Point2D[states.size()];
    int p = 0;
    for(final State s : states) {
      res[p++] = s.getStart();
    }
    return res;
  }

  @Override
  protected Point2D[] convert(final Point2D[] points, final boolean closed) {
    if(isDisabled() || points.length < 3) return points;
    final List<State> states = new LinkedList<State>();
    int start = 0;
    while(start < points.length) {
      final State s = new State(points, closed, start);
      while(s.canTakeNext()) {
        s.advanceEnd();
      }
      start = s.getEndIndex();
      states.add(s);
    }
    return createArrayFrom(states);
  }

}
