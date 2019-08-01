package wbif.sjx.MIA;

import ij.IJ;
import ij.ImagePlus;
import org.apache.spark.util.SizeEstimator;
import wbif.sjx.MIA.Module.ModuleCollection;
import wbif.sjx.MIA.Module.ObjectProcessing.Identification.IdentifyObjects;
import wbif.sjx.MIA.Object.Image;
import wbif.sjx.MIA.Object.Obj;
import wbif.sjx.MIA.Object.ObjCollection;
import wbif.sjx.MIA.Object.Workspace;
import wbif.sjx.MIA.Process.Logging.LogRenderer;
import wbif.sjx.common.MathFunc.CumStat;
import wbif.sjx.common.Object.Point;
import wbif.sjx.common.Object.Volume.VolumeType;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;

public class PerformanceTest {
    private static final String INPUT_IMAGE = "Input image";
    private static final String OUTPUT_OBJECTS = "Output objects";

    public static void main(String[] args) throws UnsupportedEncodingException {
//        getDistributionResults(URLDecoder.decode(PerformanceTest.class.getResource("/performance/BigFilled.tif").getPath(),"UTF-8"));
//        getDistributionResults(URLDecoder.decode(PerformanceTest.class.getResource("/performance/Blobs1.tif").getPath(),"UTF-8"));
        getDistributionResults(URLDecoder.decode(PerformanceTest.class.getResource("/performance/Blobs2.tif").getPath(),"UTF-8"));
        getDistributionResults(URLDecoder.decode(PerformanceTest.class.getResource("/performance/Blobs3.tif").getPath(),"UTF-8"));
        getDistributionResults(URLDecoder.decode(PerformanceTest.class.getResource("/performance/Spot1.tif").getPath(),"UTF-8"));
        getDistributionResults(URLDecoder.decode(PerformanceTest.class.getResource("/performance/Squares.tif").getPath(),"UTF-8"));

    }

    public static void getDistributionResults(String path) {
        // Getting each volume
        Object[] pointVol = runTest(path, Image.VolumeTypes.POINTLIST,false);
        Object[] quadtreeVol = runTest(path,Image.VolumeTypes.QUADTREE,false);
        Object[] octreeVol = runTest(path,Image.VolumeTypes.OCTREE,false);

        // Report distribution for smallest object
        if ((long) pointVol[1] < (long) quadtreeVol[1] && (long) pointVol[1] < (long) octreeVol[1]) {
            reportDistribution((Obj) pointVol[0]);
            System.out.println(",point");
        } else  if ((long) quadtreeVol[1] < (long) pointVol[1] && (long) quadtreeVol[1] < (long) octreeVol[1]) {
            reportDistribution((Obj) pointVol[0]);
            System.out.println(",quadtree");
        } if ((long) octreeVol[1] < (long) pointVol[1] && (long) octreeVol[1] < (long) quadtreeVol[1]) {
            reportDistribution((Obj) pointVol[0]);
            System.out.println(",octree");
        }
    }

    public static void comparePerformances(String path) {
        // Testing PointVolume
        System.out.println("Point");
        Object[] pointVol = runTest(path, Image.VolumeTypes.POINTLIST,true);
        System.out.println(" ");

        // Testing Quadtree
        System.out.println("Quadtree");
        Object[] quadtreeVol = runTest(path,Image.VolumeTypes.QUADTREE,true);
        System.out.println(" ");

        // Testing Octree
        System.out.println("Octree");
        Object[] octreeVol = runTest(path,Image.VolumeTypes.OCTREE,true);
        System.out.println(" ");

//        // Testing Optimised
//        System.out.println("Optimised");
//        Obj obj = runTest(path,Image.VolumeTypes.OPTIMISED,false);
//        System.out.println("    Selected volume "+obj.getVolumeType());
//        System.out.println(" ");

    }

    public static Object[] runTest(String path, String type, boolean verbosePerformance) {
        // Creating a new workspace
        Workspace workspace = new Workspace(0,null,1);
        ModuleCollection modules = new ModuleCollection();

        // Loading the test image and adding to workspace
        ImagePlus ipl = IJ.openImage(path);
        Image image = new Image(INPUT_IMAGE,ipl);
        workspace.addImage(image);

        // Creating the object
        Obj obj = getObject(workspace,modules,type,verbosePerformance);

        // Testing object memory properties
        long memory = evaluateObjectMemory(obj,verbosePerformance);

        // When running Optimised volume we don't need to re-run these tests
        if (!verbosePerformance) return new Object[]{obj,memory};

        // Testing full coordinate access time (via iterator)
        evaluateFullAccessTime(obj);

        // Testing random coordinate access time
        evaluateRandomAccessTime(obj);

        return new Object[]{obj,memory};

    }

    public static Obj getObject(Workspace workspace, ModuleCollection modules, String type, boolean verbosePerformance) {
        // Initialising IdentifyObjects
        IdentifyObjects identifyObjects = new IdentifyObjects(modules);
        identifyObjects.updateParameterValue(IdentifyObjects.INPUT_IMAGE,INPUT_IMAGE);
        identifyObjects.updateParameterValue(IdentifyObjects.OUTPUT_OBJECTS,OUTPUT_OBJECTS);
        identifyObjects.updateParameterValue(IdentifyObjects.WHITE_BACKGROUND,true);
        identifyObjects.updateParameterValue(IdentifyObjects.VOLUME_TYPE,type);

        // Running IdentifyObjects
        long startTime = System.nanoTime();
        identifyObjects.execute(workspace);
        long endTime = System.nanoTime();

        Obj obj = workspace.getObjectSet(OUTPUT_OBJECTS).getFirst();

        // Reporting creation time
        if (verbosePerformance) System.out.println("    Load objects time = "+((endTime-startTime)/1E6)+" ms");

        return obj;

    }

    public static long evaluateObjectMemory(Obj obj, boolean verbosePerformance) {
        // Reporting Obj memory usage
        long bytes = SizeEstimator.estimate(obj);
        if (verbosePerformance) System.out.println("    Memory usage = "+(((double) bytes/1048576d))+" MB");

        // Reporting number of Obj elements (points, nodes, etc.)
        if (verbosePerformance) System.out.println("    Number of elements = "+obj.getNumberOfElements());

        return bytes;

    }

    public static void evaluateFullAccessTime(Obj obj) {
        long startTime = System.nanoTime();
        for (Point<Integer> point:obj.getCoordinateStore());
        long endTime = System.nanoTime();

        // Reporting full volume access time
        System.out.println("    Full access time = "+((endTime-startTime)/1E6)+" ms");

    }

    public static void evaluateRandomAccessTime(Obj obj) {
        int width = obj.getWidth();
        int height = obj.getHeight();
        int nSlices = obj.getnSlices();

        // Testing 5% of points
        long nPoints = (long) Math.floor(((double) obj.size())*0.05);

        // Getting list of coordinates to test
        System.out.println("    Testing "+nPoints+" points");
        HashSet<Point<Integer>> testPoints = new HashSet<>();

        while (testPoints.size()<nPoints) {
            int x = (int) Math.floor(Math.random()*width);
            int y = (int) Math.floor(Math.random()*height);
            int z = (int) Math.floor(Math.random()*nSlices);

            testPoints.add(new Point<Integer>(x,y,z));

        }

        long startTime = System.nanoTime();
        for (Point<Integer> point:testPoints) obj.contains(point);
        long endTime = System.nanoTime();

        System.out.println("    Full access time = "+((endTime-startTime)/1E6)+" ms");

    }

    public static void reportDistribution(Obj obj) {
        CumStat csX = new CumStat();
        CumStat csY = new CumStat();
        CumStat csZ = new CumStat();

        for (Point<Integer> point:obj.getCoordinateStore()) {
            csX.addMeasure(point.getX());
            csY.addMeasure(point.getY());
            csZ.addMeasure(point.getZ());
        }

        double dppXY = obj.getDppXY();
        double dppZ = obj.getDppZ();
        int nPoints = (int) csX.getN();
        double xStdev = csX.getStd();
        double xMin = csX.getMin()-csX.getMean();
        double xMax = csX.getMax()-csX.getMean();
        double yStdev = csY.getStd();
        double yMin = csY.getMin()-csY.getMean();
        double yMax = csY.getMax()-csY.getMean();
        double zStdev = csZ.getStd();
        double zMin = csZ.getMin()-csZ.getMean();
        double zMax = csZ.getMax()-csZ.getMean();


        System.out.print(dppXY+","+dppZ+","+nPoints+","+
                xStdev+","+xMin+","+xMax+","+
                yStdev+","+yMin+","+yMax+","+
                zStdev+","+zMin+","+zMax);

    }
}
