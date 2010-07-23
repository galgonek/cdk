package org.openscience.cdk.tools;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.openscience.cdk.interfaces.IAtom;

/**
 * Methods 'borrowed' (stolen) from Jmol to determine or check the stereo
 * class of a set of atoms.
 * 
 * @author maclean
 * @cdk.module stereo
 */
public class StereoTool {

    /**
     * Currently unused, but intended for the StereoTool to indicate what it
     * 'means' by an assignment of some atoms to a class.
     *
     */
    public enum StereoClass { TETRAHEDRAL, SQUARE_PLANAR, 
        TRIGONAL_BIPYRAMIDAL, OCTAHEDRAL }

    /**
     * The handedness of a tetrahedron, in terms of the point-plane distance
     * of three of the corners, compared to the fourth.
     * 
     * PLUS indices a positive point-plane distance,
     * MINUS is a negative point-plane distance.
     */
    public enum TetrahedralSign { PLUS, MINUS }

    /**
     * The shape that four atoms take in a plane.
     */
    public enum SquarePlanarShape { U_SHAPE, FOUR_SHAPE, Z_SHAPE }

    /**
     * The maximum angle in radians for two lines to be 'diaxial'.
     * Where 0.95 is about 172 degrees.
     */
    public static final double MAX_AXIS_ANGLE = 0.95;

    /**
     * The maximum tolerance for the normal calculated during colinearity.
     */
    public static final double MIN_COLINEAR_NORMAL = 0.05;
    
    /**
     * Checks these four atoms for square planarity.
     * 
     * @param atomA
     * @param atomB
     * @param atomC
     * @param atomD
     * @return
     */
    public static boolean isSquarePlanar(
            IAtom atomA, IAtom atomB, IAtom atomC, IAtom atomD) {
        Vector3d vNorm1 = new Vector3d();
        Vector3d vNorm2 = new Vector3d();
        Vector3d vNorm3 = new Vector3d();
        StereoTool.getPlaneNormals(
                atomA, atomB, atomC, atomD, vNorm1, vNorm2, vNorm3);

        // vNorm1 vNorm2 vNorm3 are right-hand normals for the given
        // triangles
        // 1-2-3, 2-3-4, 3-4-1
        // sp1 up up up U-shaped
        // sp2 up up DOWN 4-shaped
        // sp3 up DOWN DOWN Z-shaped
        double norm12 = vNorm1.dot(vNorm2);
        double norm23 = vNorm2.dot(vNorm3);

        // XXX this is certainly wrong... (as in, a bad translation from Jmol)
        return norm12 < 0 || norm23 < 0;    
    }

    /**
     * Checks these 7 atoms to see if they are at the points of an octahedron.
     * 
     * @param atomA
     * @param atomB
     * @param atomC
     * @param atomD
     * @param atomE
     * @param atomF
     * @param atomG
     * @return
     */
    public static boolean isOctahedral(IAtom atomA, IAtom atomB, IAtom atomC,
            IAtom atomD, IAtom atomE, IAtom atomF, IAtom atomG) {
        boolean isDiaxialAAGB = 
            isDiaxial(atomA, atomA, atomG, atomB, StereoTool.MAX_AXIS_ANGLE);
        if (!isDiaxialAAGB) return false;   // XXX ?

        Vector3d vNorm1 = new Vector3d();
        Vector3d vNorm2 = new Vector3d();
        Vector3d vNorm3 = new Vector3d();
        StereoTool.getPlaneNormals(
                atomC, atomD, atomE, atomF, vNorm1, vNorm2, vNorm3);
        
        // XXX ?
        if ((vNorm1.dot(vNorm2) < 0) || vNorm2.dot(vNorm3) < 0) return false;

        // now check rotation in relation to the first atom
        vNorm2.set(atomA.getPoint3d());
        vNorm2.sub(atomB.getPoint3d());
        return vNorm1.dot(vNorm2) < 0;  // XXX ?
    }

    /**
     * Checks these 6 atoms to see if they form a trigonal-bipyramidal shape. 
     * 
     * @param atomA one of the axial atoms
     * @param atomB the central atom
     * @param atomC one of the equatorial atoms
     * @param atomD one of the equatorial atoms
     * @param atomE one of the equatorial atoms
     * @param atomF the other axial atom
     * @return
     */
    public static boolean isTrigonalBipyramidal(IAtom atomA, IAtom atomB, 
            IAtom atomC, IAtom atomD, IAtom atomE, IAtom atomF) {
        Point3d pointA = atomA.getPoint3d();
        Point3d pointB = atomB.getPoint3d();
        Point3d pointC = atomC.getPoint3d();
        Point3d pointD = atomD.getPoint3d();
        Point3d pointE = atomE.getPoint3d();
        Point3d pointF = atomF.getPoint3d();
        
        boolean isColinearABF = StereoTool.colinear(pointA, pointB, pointF);
        if (isColinearABF) {
            // the normal to the equatorial plane
            Vector3d normal = StereoTool.getNormal(pointC, pointD, pointE);
            
            // get the side of the plane that axis point A is 
            TetrahedralSign handednessCDEA = 
                StereoTool.getHandedness(normal, pointC, pointF);
            
            // get the side of the plane that axis point F is
            TetrahedralSign handednessCDEF = 
                StereoTool.getHandedness(normal, pointC, pointA);
            
            // in other words, the two axial points (A,F) are on opposite sides
            // of the equatorial plane CDE
            return handednessCDEA != handednessCDEF;
        } else {
            return false;
        }
    }

    /**
     * Gets the tetrahedral handedness of four atoms - three of which form the
     * 'base' of the tetrahedron, and the other the apex. Note that it assumes
     * a right-handed coordinate system, and that the points {A,B,C} are in
     * a counter-clockwise order in the plane they share. 
     * 
     * @param baseAtomA the first atom in the base of the tetrahedron
     * @param baseAtomB the second atom in the base of the tetrahedron
     * @param baseAtomC the third atom in the base of the tetrahedron
     * @param apexAtom the atom in the point of the tetrahedron
     * @return
     */
    public static TetrahedralSign getHandedness(
            IAtom baseAtomA, IAtom baseAtomB, IAtom baseAtomC, IAtom apexAtom) {
        Point3d pointA = baseAtomA.getPoint3d();
        Point3d pointB = baseAtomB.getPoint3d();
        Point3d pointC = baseAtomC.getPoint3d();
        Point3d pointD = apexAtom.getPoint3d();
        return StereoTool.getHandedness(pointA, pointB, pointC, pointD);
    }

    private static TetrahedralSign getHandedness(
            Point3d pointA, Point3d pointB, Point3d pointC, Point3d pointD) {
        // assumes anti-clockwise for a right-handed system
        Vector3d normal = StereoTool.getNormal(pointA, pointB, pointC);
        
        // it doesn't matter which of points {A,B,C} is used
        return StereoTool.getHandedness(normal, pointA, pointD);
    }
        
    private static TetrahedralSign getHandedness(
            Vector3d planeNormal, Point3d pointInPlane, Point3d testPoint) {
        double distance = signedDistanceToPlane(
                planeNormal, pointInPlane, testPoint);

        // the point-plane distance is the absolute value,
        // the sign of the distance gives the side of the plane the point is on
        if (distance > 0) {
            return TetrahedralSign.PLUS;
        } else {
            return TetrahedralSign.MINUS;
        }
    }

    private static boolean isDiaxial(
          IAtom atomA, IAtom atomB, IAtom atomC, IAtom atomD, double maxAngle) {
        Point3d pointA = atomA.getPoint3d();
        Point3d pointB = atomB.getPoint3d();
        Point3d pointC = atomC.getPoint3d();
        Point3d pointD = atomD.getPoint3d();
        return isDiaxial(pointA, pointB, pointC, pointD, maxAngle);
    }

    private static boolean isDiaxial(Point3d pointA, Point3d pointB, 
            Point3d pointC, Point3d pointD,  double maxAngle) {
        // we don't care about these vectors...
        Vector3d vAC = new Vector3d();
        Vector3d vBD = new Vector3d();
        return isDiaxial(pointA, pointB, pointC, pointD, vAC, vBD, maxAngle);
    }

    // NOTE : taken from Jmol class o.j.smiles.SmilesSearch
    // I think that it is finding the angle between the lines A-C and B-D, and 
    // checking that angle is less than f...
    // XXX the side-effect (boo!) of the method is to create 
    // the vectors corresponding to the lines
    private static boolean isDiaxial(Point3d pointA, Point3d pointB, 
            Point3d pointC, Point3d pointD, Vector3d vAC, Vector3d vBD, 
            double maxAngle) {
        vAC.set(pointA);
        vBD.set(pointB);
        vAC.sub(pointC);
        vBD.sub(pointD);
        
        // -0.95f about 172 degrees
        return vAC.dot(vBD) < maxAngle;
    }
    
    /**
     * Checks the three supplied points to see if they fall on the same line.
     * It does this by finding the normal to an arbitrary pair of lines between
     * the points (in fact, A-B and A-C) and checking that its length is 0.
     * 
     * @param ptA
     * @param ptB
     * @param ptC
     * @return
     */
    public static boolean colinear(Point3d ptA, Point3d ptB, Point3d ptC) {
        Vector3d vectorAB = new Vector3d();
        Vector3d vectorAC = new Vector3d();
        Vector3d normal = new Vector3d();
        
        StereoTool.getRawNormal(ptA, ptB, ptC, normal, vectorAB, vectorAC);
        double baCrossACLen = normal.length();
        return baCrossACLen < StereoTool.MIN_COLINEAR_NORMAL;
    }

    /**
     * Given a normalized normal for a plane, any point in that plane, and
     * a point, will return the distance between the plane and that point.
     *  
     * @param planeNormal the normalized plane normal
     * @param pointInPlane an arbitrary point in that plane
     * @param point the point to measure
     * @return the signed distance to the plane
     */
    public static double signedDistanceToPlane(
            Vector3d planeNormal, Point3d pointInPlane, Point3d point) {
        if (planeNormal == null) return Double.NaN;

        Vector3d pointPointDiff = new Vector3d();
        pointPointDiff.sub(point, pointInPlane);
        return planeNormal.dot(pointPointDiff);
    }

    private static void getPlaneNormals(IAtom atomA, IAtom atomB, IAtom atomC, 
            IAtom atomD, Vector3d normalA, Vector3d normalB, Vector3d normalC) {
        Point3d pointA = atomA.getPoint3d();
        Point3d pointB = atomB.getPoint3d();
        Point3d pointC = atomC.getPoint3d();
        Point3d pointD = atomD.getPoint3d();
        
        StereoTool.getPlaneNormals(pointA, pointB, pointC, pointD, 
                                   normalA, normalB, normalC);
    }
    
    private static void getPlaneNormals(
            Point3d pointA, Point3d pointB, Point3d pointC, Point3d pointD,
            Vector3d normalA, Vector3d normalB, Vector3d normalC) {

        // these are temporary vectors that are re-used in the calculations
        Vector3d vectorD = new Vector3d();
        Vector3d vectorE = new Vector3d();

        // the normals (normalA, normalB, normalC) are calculated
        StereoTool.getRawNormal(pointA, pointB, pointC, normalA, vectorD, vectorE);
        StereoTool.getRawNormal(pointB, pointC, pointD, normalB, vectorD, vectorE);
        StereoTool.getRawNormal(pointC, pointD, pointA, normalC, vectorD, vectorE);
        normalA.normalize();
        normalB.normalize();
        normalC.normalize();
    }
    
    /**
     * <p>Given three points (A, B, C), makes the vectors A-B and A-C, and makes
     * the cross product of these two vectors; this has the effect of making a
     * third vector at right angles to AB and AC.</p>
     * 
     * <p>NOTE : the returned normal is normalized; that is, it has been
     * divided by its length.</p> 
     * 
     * @param ptA
     * @param ptB
     * @param ptC
     * @return
     */
    public static Vector3d getNormal(Point3d ptA, Point3d ptB, Point3d ptC) {
        Vector3d vectorAB = new Vector3d();
        Vector3d vectorAC = new Vector3d();
        Vector3d normal   = new Vector3d();
        StereoTool.getRawNormal(ptA, ptB, ptC, normal, vectorAB, vectorAC);
        normal.normalize();
        return normal;
    }
    
    private static void getRawNormal(Point3d ptA, Point3d ptB, Point3d ptC, 
                               Vector3d normal, Vector3d vcAB, Vector3d vcAC) {
        // make A->B and A->C
        vcAB.sub(ptB, ptA);
        vcAC.sub(ptC, ptA);
        
        // make the normal to this
        normal.cross(vcAB, vcAC);
    }

}