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
        String path = URLDecoder.decode(PerformanceTest.class.getResource("/performance/Blobs2.tif").getPath(),"UTF-8");
        runImage(path);

    }

    public static void runImage(String path) {
        // Testing PointVolume
        System.out.println("PointVolume");
        runTest(path, Image.VolumeTypes.POINTLIST);
        System.out.println(" ");

        // Testing Quadtree
        System.out.println("QuadTreeVolume");
        runTest(path,Image.VolumeTypes.QUADTREE);
        System.out.println(" ");

        // Testing Octree
        System.out.println("OctreeVolulme");
        runTest(path,Image.VolumeTypes.OCTREE);
        System.out.println(" ");

    }

    public static void runTest(String path, String type) {
        // Creating a new workspace
        Workspace workspace = new Workspace(0,null,1);
        ModuleCollection modules = new ModuleCollection();

        // Loading the test image and adding to workspace
        ImagePlus ipl = IJ.openImage(path);
        Image image = new Image(INPUT_IMAGE,ipl);
        workspace.addImage(image);

        // Creating the object
        Obj obj = getObject(workspace,modules,type);

        // Testing object memory properties
        evaluateObjectMemory(obj);

        // Testing full coordinate access time (via iterator)
        evaluateFullAccessTime(obj);

        // Testing random coordinate access time
        evaluateRandomAccessTime(obj);

    }

    public static Obj getObject(Workspace workspace, ModuleCollection modules, String type) {
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
        System.out.println("    Load objects time = "+((endTime-startTime)/1E6)+" ms");

        return obj;

    }

    public static void evaluateObjectMemory(Obj obj) {
        // Reporting Obj memory usage
        System.out.println("    Memory usage = "+(((double)SizeEstimator.estimate(obj)/1048576d))+" MB");

        // Reporting number of Obj elements (points, nodes, etc.)
        System.out.println("    Number of elements = "+obj.getNumberOfElements());

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
}
