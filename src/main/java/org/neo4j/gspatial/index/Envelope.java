package org.neo4j.gspatial.index;

import java.util.Arrays;

public class Envelope {
    static final double MAXIMAL_ENVELOPE_SIDE_RATIO = 100000.0;
    protected final double[] min;
    protected final double[] max;

    public Envelope(Envelope e) {
        this(e.min, e.max);
    }

    public Envelope(double[] min, double[] max) {
        this.min = Arrays.copyOf(min, min.length);
        this.max = Arrays.copyOf(max, max.length);
        if (!isValid(min, max)) {
            throw new IllegalArgumentException("Invalid envelope created " + this);
        }
    }

    public Envelope(double xmin, double xmax, double ymin, double ymax) {
        this(new double[]{xmin, ymin}, new double[]{xmax, ymax});
    }

    public Envelope withSideRatioNotTooSmall() {
        double[] from = Arrays.copyOf(this.min, this.min.length);
        double[] to = Arrays.copyOf(this.max, this.max.length);
        double highestDiff = -1.7976931348623157E308;
        double[] diffs = new double[from.length];

        for (int i = 0; i < from.length; ++i) {
            diffs[i] = to[i] - from[i];
            highestDiff = Math.max(highestDiff, diffs[i]);
        }

        double mindiff = highestDiff / 100000.0;

        for (int i = 0; i < from.length; ++i) {
            if (diffs[i] < mindiff) {
                to[i] = from[i] + mindiff;
            }
        }

        return new Envelope(from, to);
    }

    public double[] getMin() {
        return this.min;
    }

    public double[] getMax() {
        return this.max;
    }

    public double getMin(int dimension) {
        return this.min[dimension];
    }

    public double getMax(int dimension) {
        return this.max[dimension];
    }

    public double getMinX() {
        return this.getMin(0);
    }

    public double getMaxX() {
        return this.getMax(0);
    }

    public double getMinY() {
        return this.getMin(1);
    }

    public double getMaxY() {
        return this.getMax(1);
    }

    public int getDimension() {
        return this.min.length;
    }

    public boolean contains(Envelope other) {
        return this.covers(other);
    }

    public boolean covers(Envelope other) {
        boolean covers = this.getDimension() == other.getDimension();

        for (int i = 0; i < this.min.length && covers; ++i) {
            covers = other.min[i] >= this.min[i] && other.max[i] <= this.max[i];
        }

        return covers;
    }

    public boolean intersects(Envelope other) {
        boolean intersects = this.getDimension() == other.getDimension();

        for (int i = 0; i < this.min.length && intersects; ++i) {
            intersects = other.min[i] <= this.max[i] && other.max[i] >= this.min[i];
        }

        return intersects;
    }

    public void expandToInclude(Envelope other) {
        if (this.getDimension() != other.getDimension()) {
            int var10002 = this.getDimension();
            throw new IllegalArgumentException("Cannot join Envelopes with different dimensions: " + var10002 + " != " + other.getDimension());
        } else {
            for (int i = 0; i < this.min.length; ++i) {
                if (other.min[i] < this.min[i]) {
                    this.min[i] = other.min[i];
                }

                if (other.max[i] > this.max[i]) {
                    this.max[i] = other.max[i];
                }
            }

        }
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Envelope other)) {
            return false;
        } else if (this.getDimension() != other.getDimension()) {
            return false;
        } else {
            for (int i = 0; i < this.getDimension(); ++i) {
                if (this.min[i] != other.getMin(i) || this.max[i] != other.getMax(i)) {
                    return false;
                }
            }

            return true;
        }
    }

    public int hashCode() {
        int result = 1;
        double[] var2 = this.min;
        int var3 = var2.length;

        int var4;
        double element;
        long bits;
        for (var4 = 0; var4 < var3; ++var4) {
            element = var2[var4];
            bits = Double.doubleToLongBits(element);
            result = 31 * result + (int) (bits ^ bits >>> 32);
        }

        var2 = this.max;
        var3 = var2.length;

        for (var4 = 0; var4 < var3; ++var4) {
            element = var2[var4];
            bits = Double.doubleToLongBits(element);
            result = 31 * result + (int) (bits ^ bits >>> 32);
        }

        return result;
    }

    public double distance(Envelope other, int dimension) {
        return this.min[dimension] < other.min[dimension] ? other.min[dimension] - this.max[dimension] : this.min[dimension] - other.max[dimension];
    }

    public double distance(Envelope other) {
        if (this.intersects(other)) {
            return 0.0;
        } else {
            double distance = 0.0;

            for (int i = 0; i < this.min.length; ++i) {
                double dist = this.distance(other, i);
                if (dist > 0.0) {
                    distance += dist * dist;
                }
            }

            return Math.sqrt(distance);
        }
    }

    public double getWidth() {
        return this.getWidth(0);
    }

    public double getWidth(int dimension) {
        return this.max[dimension] - this.min[dimension];
    }

    public double[] getWidths(int divisor) {
        double[] widths = Arrays.copyOf(this.max, this.max.length);

        for (int d = 0; d < this.max.length; ++d) {
            widths[d] -= this.min[d];
            widths[d] /= (double) divisor;
        }

        return widths;
    }

    public double getArea() {
        double area = 1.0;

        for (int i = 0; i < this.min.length; ++i) {
            area *= this.max[i] - this.min[i];
        }

        return area;
    }

    public double overlap(Envelope other) {
        Envelope smallest = this.getArea() < other.getArea() ? this : other;
        Envelope intersection = this.intersection(other);
        return intersection == null ? 0.0 : (smallest.isPoint() ? 1.0 : intersection.getArea() / smallest.getArea());
    }

    public boolean isPoint() {
        boolean ans = true;

        for (int i = 0; i < this.min.length && ans; ++i) {
            ans = this.min[i] == this.max[i];
        }

        return ans;
    }

    private static boolean isValid(double[] min, double[] max) {
        boolean valid = min != null && max != null && min.length == max.length;

        for (int i = 0; valid && i < min.length; ++i) {
            valid = min[i] <= max[i];
        }

        return valid;
    }

    public String toString() {
        String var10000 = makeString(this.min);
        return "Envelope: min=" + var10000 + ", max=" + makeString(this.max);
    }

    private static String makeString(double[] vals) {
        StringBuilder sb = new StringBuilder();
        if (vals == null) {
            sb.append("null");
        } else {
            double[] var2 = vals;
            int var3 = vals.length;

            for (int var4 = 0; var4 < var3; ++var4) {
                double val = var2[var4];
                if (sb.length() > 0) {
                    sb.append(',');
                } else {
                    sb.append('(');
                }

                sb.append(val);
            }

            if (sb.length() > 0) {
                sb.append(')');
            }
        }

        return sb.toString();
    }

    public Envelope intersection(Envelope other) {
        if (this.getDimension() != other.getDimension()) {
            int var10002 = this.getDimension();
            throw new IllegalArgumentException("Cannot calculate intersection of Envelopes with different dimensions: " + var10002 + " != " + other.getDimension());
        } else {
            double[] iMin = new double[this.min.length];
            double[] iMax = new double[this.min.length];
            Arrays.fill(iMin, Double.NaN);
            Arrays.fill(iMax, Double.NaN);
            boolean result = true;

            for (int i = 0; i < this.min.length; ++i) {
                if (other.min[i] <= this.max[i] && other.max[i] >= this.min[i]) {
                    iMin[i] = Math.max(this.min[i], other.min[i]);
                    iMax[i] = Math.min(this.max[i], other.max[i]);
                } else {
                    result = false;
                }
            }

            return result ? new Envelope(iMin, iMax) : null;
        }
    }
}
