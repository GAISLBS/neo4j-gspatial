package org.neo4j.gspatial.index.rtree;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

public class Envelope extends org.neo4j.gspatial.index.Envelope {

    /**
     * Copy constructor
     */
    public Envelope(org.neo4j.gspatial.index.Envelope e) {
        super(e.getMin(), e.getMax());
    }

    /**
     * General constructor for the n-dimensional case
     */
    public Envelope(double[] min, double[] max) {
        super(min, max);
    }

    /**
     * General constructor for the n-dimensional case starting with a single point
     */
    public Envelope(double[] p) {
        super(p.clone(), p.clone());
    }

    /**
     * Special constructor for the 2D case
     */
    public Envelope(double xmin, double xmax, double ymin, double ymax) {
        super(xmin, xmax, ymin, ymax);
    }

    public Envelope(org.locationtech.jts.geom.Envelope envelopeInternal) {
        super(envelopeInternal.getMinX(), envelopeInternal.getMaxX(), envelopeInternal.getMinY(), envelopeInternal.getMaxY());
    }

    /**
     * Note that this doesn't exclude the envelope boundary.
     * See JTS Envelope.
     */
    public boolean contains(Envelope other) {
        //TODO: We can remove this method and covers method if we determine why super.covers does not do boolean shortcut
        return covers(other);
    }

    public boolean covers(Envelope other) {
        boolean ans = getDimension() == other.getDimension();
        for (int i = 0; i < min.length; i++) {
            //TODO: Why does the parent class not use this shortcut?
            if (!ans)
                return ans;
            ans = ans && other.min[i] >= min[i] && other.max[i] <= max[i];
        }
        return ans;
    }

    public void scaleBy(double factor) {
        for (int i = 0; i < min.length; i++) {
            scaleBy(factor, i);
        }
    }

    private void scaleBy(double factor, int dimension) {
        max[dimension] = min[dimension] + (max[dimension] - min[dimension]) * factor;
    }

    public void shiftBy(double offset) {
        for (int i = 0; i < min.length; i++) {
            shiftBy(offset, i);
        }
    }

    public void shiftBy(double offset, int dimension) {
        min[dimension] += offset;
        max[dimension] += offset;
    }

    public double[] centre() {
        double[] center = new double[min.length];
        for (int i = 0; i < min.length; i++) {
            center[i] = centre(i);
        }
        return center;
    }

    public double centre(int dimension) {
        return (min[dimension] + max[dimension]) / 2.0;
    }

    public void expandToInclude(double[] p) {
        for (int i = 0; i < Math.min(p.length, min.length); i++) {
            if (p[i] < min[i])
                min[i] = p[i];
            if (p[i] > max[i])
                max[i] = p[i];
        }
    }

    public double separation(Envelope other) {
        Envelope combined = new Envelope(this);
        combined.expandToInclude(other);
        return combined.getArea() - this.getArea() - other.getArea();
    }

    public double separation(Envelope other, int dimension) {
        Envelope combined = new Envelope(this);
        combined.expandToInclude(other);
        return combined.getWidth(dimension) - this.getWidth(dimension) - other.getWidth(dimension);
    }

    public Envelope intersection(Envelope other) {
        return new Envelope(super.intersection(other));
    }

    public Envelope bbox(Envelope other) {
        if (getDimension() == other.getDimension()) {
            Envelope result = new Envelope(this);
            result.expandToInclude(other);
            return result;
        } else {
            throw new IllegalArgumentException("Cannot calculate bounding box of Envelopes with different dimensions: " + this.getDimension() + " != " + other.getDimension());
        }
    }

    public Geometry toGeometry() {
        GeometryFactory factory = new GeometryFactory();
        Coordinate[] coordinates = new Coordinate[]{
                new Coordinate(min[0], min[1]),
                new Coordinate(min[0], max[1]),
                new Coordinate(max[0], max[1]),
                new Coordinate(max[0], min[1]),
                new Coordinate(min[0], min[1])
        };
        return factory.createPolygon(coordinates);
    }

    /**
     * Calculate the distance from this envelope to a point
     *
     * @param p the point represented as an array of doubles
     * @return the distance from the envelope to the point
     */
    public double distance(double[] p) {
        double distanceSquared = 0.0;
        for (int i = 0; i < p.length; i++) {
            double minDistance = 0.0;
            if (p[i] < min[i]) {
                minDistance = min[i] - p[i];
            } else if (p[i] > max[i]) {
                minDistance = p[i] - max[i];
            }
            distanceSquared += minDistance * minDistance;
        }
        return Math.sqrt(distanceSquared);
    }
}
