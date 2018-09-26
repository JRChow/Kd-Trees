import edu.princeton.cs.algs4.RectHV;
import edu.princeton.cs.algs4.Point2D;
import edu.princeton.cs.algs4.StdOut;
import edu.princeton.cs.algs4.Queue;

public class KdTreeST<Value> {
    private Node root; // The root of the kd-tree.
    private static final boolean HORIZONTAL = true; // Horizontal means we should check the x coordinate.
    private static final double X_MIN = Double.NEGATIVE_INFINITY;
    private static final double Y_MIN = Double.NEGATIVE_INFINITY;
    private static final double X_MAX = Double.POSITIVE_INFINITY;
    private static final double Y_MAX = Double.POSITIVE_INFINITY;

    private class Node {
        private Point2D p;      // the point
        private Value value;    // the symbol table maps the point to this value
        private RectHV rect;    // the axis-aligned rectangle corresponding to this node
        private Node lb;        // the left/bottom subtree
        private Node rt;        // the right/top subtree
        private int N;          // the size of the subtree rooted at the node
        private boolean orientation; // the orientation of the node

        public Node(Point2D p, Value val, int N, boolean orientation) {
            this.p = p;
            this.value = val;
            this.N = N;
            this.orientation = orientation;
        }
    }

    // construct an empty symbol table of points
    public KdTreeST() {
        root = null;
    }

    // is the symbol table empty?
    public boolean isEmpty() {
        return root == null;
    }

    // number of points
    public int size() {
        return size(root);
    }

    private int size(Node x) {
        if (x == null) return 0;
        return x.N;
    }

    // associate the value val with point p
    public void put(Point2D p, Value val) {
        if (p == null || val == null) {
            throw new NullPointerException("Argument cannot be null!");
        }
        root = put(p, val, root, null);
    }

    private Node put(Point2D p, Value val, Node x, Node parent) {
        if (x == null) {
            Node newNode;
            if (parent == null) {
                newNode = new Node(p, val, 1, HORIZONTAL);
                newNode.rect = new RectHV(X_MIN, Y_MIN, X_MAX, Y_MAX);
                return newNode;
            }
            newNode = new Node(p, val, 1, !parent.orientation);
            newNode.rect = getBoundingBox(p, parent);
            StdOut.println("Bounding box for " + p.toString() + " ==> " + newNode.rect.toString());
            return newNode;
        }

        int cmp = myCompare(x.p, p, x.orientation);
        if (x.p.equals(p)) {
            x.value = val;
            return x;
        }
        if (cmp < 0) x.lb = put(p, val, x.lb, x);
        else if (cmp >= 0) x.rt = put(p, val, x.rt, x);

        x.N = size(x.lb) + size(x.rt) + 1;

        return x;
    }

    private RectHV getBoundingBox(Point2D currentP, Node parent) {
        if (myCompare(currentP, parent.p, parent.orientation) < 0) {
            // left
            if (parent.orientation == HORIZONTAL)
                return new RectHV(parent.rect.xmin(), parent.rect.ymin(), parent.p.x(), parent.rect.ymax());
            else
                return new RectHV(parent.rect.xmin(), parent.rect.ymin(), parent.rect.xmax(), parent.p.y());
        } else {
            // right
            if (parent.orientation == HORIZONTAL)
                return new RectHV(parent.p.x(), parent.rect.ymin(), parent.rect.xmax(), parent.rect.ymax());
            else
                return new RectHV(parent.rect.xmin(), parent.p.y(), parent.rect.xmax(), parent.rect.ymax());
        }
    }

    private int myCompare(Point2D p1, Point2D p2, boolean orientationP1) {
        if (orientationP1 == HORIZONTAL) {
            if (p1.x() - p2.x() < 0) return -1;
            if (p1.x() - p2.x() > 0) return 1;
            return 0;
        } else { // orientationP1 is VERTICAL
            if (p1.y() - p2.y() < 0) return -1;
            if (p1.y() - p2.y() > 0) return 1;
            return 0;
        }
    }

    // value associated with point p
    public Value get(Point2D p) {
        if (p == null) throw new NullPointerException("Argument cannot be null!");
        return get(root, p);
    }

    private Value get(Node x, Point2D p) {
        if (x == null) return null;

        if (x.p.equals(p)) {
            return x.value;
        }

        int cmp = myCompare(x.p, p, x.orientation);
        if (cmp < 0) return get(x.lb, p);
        else if (cmp >= 0) return get(x.rt, p);

        else return x.value;
    }

    // does the symbol table contain point p?
    public boolean contains(Point2D p) {
        if (p == null)
            throw new NullPointerException("Argument cannot be null!");

        return get(p) != null;
    }

    // all points in the symbol table
    public Iterable<Point2D> points() {
        Queue<Point2D> queue = new Queue<Point2D>();
        points(root, queue);
        return queue;
    }

    private void points(Node x, Queue<Point2D> queue) {
        if (x == null) return;

        points(x.lb, queue);
        queue.enqueue(x.p);
        points(x.rt, queue);
    }

    // all points that are inside the rectangle
    public Iterable<Point2D> range(RectHV rect) {
        Queue<Point2D> q = new Queue<Point2D>();
        range(root, rect, q);
        return q;
    }

    private void range(Node x, RectHV rect, Queue<Point2D> queue) {
        if (x == null) return;
        if (!x.rect.intersects(rect)) return;
        if (rect.contains(x.p)) queue.enqueue(x.p);
        range(x.lb, rect, queue);
        range(x.rt, rect, queue);
    }
    /* Instead of checking whether the query rectangle intersects the rectangle corresponding to a node, it suffices to
     check only whether the query rectangle intersects the splitting line segment: if it does, then recursively search
     both subtrees; otherwise, recursively search the one subtree where points intersecting the query rectangle could be.
     */

    // a nearest neighbor to point p; null if the symbol table is empty
    public Point2D nearest(Point2D p) {
        double distance = root.rect.distanceSquaredTo(p);
        double pointDist = root.p.distanceSquaredTo(p);
        return nearest(root, root.p, p);
        // throw new UnsupportedOperationException();
    }

    private Point2D nearest(Node current, Point2D champ, Point2D p) {
        Point2D champion = champ;

        if (size() == 0)
            return null; // throw exception

        if (current == null) return champ;

        double bestDist = champ.distanceSquaredTo(p);

        if (current.p.distanceSquaredTo(p) < bestDist) champion = current.p;

        if (current.rect.distanceSquaredTo(p) < bestDist) {
            // do not prune
            // decide left or right
            if (myCompare(p, current.p, current.orientation) >= 0) {
                // go right first
                champion = nearest(current.rt, champion, p);
                // we still need to go left
                champion = nearest(current.lb, champion, p);
            } else {
                // go left first
                champion = nearest(current.lb, champion, p);
                // we still need to go right
                champion = nearest(current.rt, champion, p);
            }
        }
        // otherwise we prune
        return champion;
    }

    // unit testing (required)
    public static void main(String[] args) {
        KdTreeST<Integer> kdTreeST = new KdTreeST<Integer>();
        StdOut.println("======= Testing isEmpty() =======");
        StdOut.println("pass => " + (kdTreeST.isEmpty() == true));
        StdOut.println("======= Testing size() =======");
        StdOut.println("pass => " + (kdTreeST.size() == 0));
        StdOut.println("======= Putting in points via put() =======");
        kdTreeST.put(new Point2D(2, 3), 1);
        kdTreeST.put(new Point2D(4, 2), 2);
        kdTreeST.put(new Point2D(4, 5), 3);
        kdTreeST.put(new Point2D(3, 3), 4);
        kdTreeST.put(new Point2D(1, 5), 5);
        kdTreeST.put(new Point2D(4, 4), 6);
        kdTreeST.put(new Point2D(2, 3), -1);
        StdOut.println("======= Testing size() AGAIN =======");
        StdOut.println("Current size = " + kdTreeST.size());
        StdOut.println("pass => " + (kdTreeST.size() == 6));
        StdOut.println("======= Getting points via get() =======");
        StdOut.println("pass => " + (kdTreeST.get(new Point2D(2, 3)) == -1));
        StdOut.println("pass => " + (kdTreeST.get(new Point2D(4, 2)) == 2));
        StdOut.println("pass => " + (kdTreeST.get(new Point2D(4, 5)) == 3));
        StdOut.println("pass => " + (kdTreeST.get(new Point2D(3, 3)) == 4));
        StdOut.println("pass => " + (kdTreeST.get(new Point2D(1, 5)) == 5));
        StdOut.println("pass => " + (kdTreeST.get(new Point2D(4, 4)) == 6));
        StdOut.println("======= Testing contains() =======");
        StdOut.println("pass => " + (kdTreeST.contains(new Point2D(4, 4)) == true));
        StdOut.println("pass => " + (kdTreeST.contains(new Point2D(4, 100)) == false));
        StdOut.println("======= Testing points() =======");
        for (Point2D p : kdTreeST.points()) {
            StdOut.println(p.toString());
        }
        StdOut.println("======= Testing range() =======");
        for (Point2D p : kdTreeST.range(new RectHV(1.2, 2.5, 3.5, 3.5))) {
            StdOut.println(p.toString());
        }

        KdTreeST<Integer> test = new KdTreeST<Integer>();
        test.put(new Point2D(2, 3), 1);
        StdOut.println("======= Testing nearest() =======");
        StdOut.println("Nearest point = " + test.nearest(new Point2D(4, 4)));

        StdOut.println("Nearest point = " +
                kdTreeST.nearest(new Point2D(4.2, 1.5)));
        // failed- should be 3, 3, or 4, 4
        StdOut.println("Nearest point = " +
                kdTreeST.nearest(new Point2D(3, 4)));
        // passed
        StdOut.println("Nearest point = " +
                kdTreeST.nearest(new Point2D(0, 5)));

        StdOut.println("======= Testing nearest() for (4, 2) =======");
        // not considering the root
        StdOut.println("Nearest point = " +
                kdTreeST.nearest(new Point2D(4, 2)));
        StdOut.println("======= Testing nearest() for (4.2, 2.2) =======");
        // not considering the root
        StdOut.println("Nearest point = " +
                kdTreeST.nearest(new Point2D(4.2, 2.2)));
        StdOut.println("======= Testing nearest() for (2, 5.5) =======");
        // not considering the root
        StdOut.println("Nearest point = " +
                kdTreeST.nearest(new Point2D(2, 5.5)));
        StdOut.println("======= Testing nearest() for (2, 3) =======");
        // not considering the root
        StdOut.println("Nearest point = " +
                kdTreeST.nearest(new Point2D(2, 3)));

    }
}