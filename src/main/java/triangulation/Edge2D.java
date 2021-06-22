package triangulation;

import org.matsim.counts.algorithms.graphs.helper.Comp;

import static de.mpi.ds.utils.GeneralUtils.doubleCloseToZero;

/**
 * 2D edge class implementation.
 *
 * @author Johannes Diemke
 */
public class Edge2D implements Comparable {

    public Vector2D a;
    public Vector2D b;

    /**
     * Constructor of the 2D edge class used to create a new edge instance from
     * two 2D vectors describing the edge's vertices.
     *
     * @param a The first vertex of the edge
     * @param b The second vertex of the edge
     */
    public Edge2D(Vector2D a, Vector2D b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public int compareTo(Object o) {
        Edge2D toComp = (Edge2D) o;
        if ((a.compareTo(toComp.a) == 0 || b.compareTo(toComp.a) == 0) && (a.compareTo(toComp.b)==0 || b.compareTo(toComp.b) == 0)) {
            return 0;
        } else if (a.sub(b).mag() > toComp.a.sub(toComp.b).mag()){
            return 1;
        } else {
            return -1;
        }
    }

//    @Override
//    public int compareTo(Object o) {
//        Edge2D toComp = (Edge2D) o;
//        if (toComp.a.compareTo(a) == 0 && toComp.b.compareTo(b) || )
//        return 0;
//    }
}