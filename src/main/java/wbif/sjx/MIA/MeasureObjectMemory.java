package wbif.sjx.MIA;

import wbif.sjx.MIA.Module.Module;
import wbif.sjx.MIA.Module.ModuleCollection;
import wbif.sjx.MIA.Module.PackageNames;
import wbif.sjx.MIA.Object.Measurement;
import wbif.sjx.MIA.Object.Obj;
import wbif.sjx.MIA.Object.ObjCollection;
import wbif.sjx.MIA.Object.Status;
import wbif.sjx.MIA.Object.Workspace;
import wbif.sjx.MIA.Object.Parameters.InputObjectsP;
import wbif.sjx.MIA.Object.Parameters.ParamSeparatorP;
import wbif.sjx.MIA.Object.Parameters.ParameterCollection;
import wbif.sjx.MIA.Object.References.ImageMeasurementRefCollection;
import wbif.sjx.MIA.Object.References.MetadataRefCollection;
import wbif.sjx.MIA.Object.References.ObjMeasurementRef;
import wbif.sjx.MIA.Object.References.ObjMeasurementRefCollection;
import wbif.sjx.MIA.Object.References.ParentChildRefCollection;
import wbif.sjx.MIA.Object.References.PartnerRefCollection;

public class MeasureObjectMemory extends Module {
    public static final String INPUT_SEPARATOR = "Object and image input";
    public static final String INPUT_OBJECTS = "Input objects";

    // public static void main(String[] args) throws Exception {
        // MIA.addPluginPackageName(MeasureObjectMemory.class.getCanonicalName());

        // // The following fails with an error "zip file closed"
        // MIA.main(new String[] {});

    // }

    public MeasureObjectMemory(ModuleCollection modules) {
        super("Measure object memory", modules);

    }

    public interface Measurements {
        String OBJECT_MEMORY_KB = "OBJECT_MEMORY_(KB)";
        String OBJECT_MEMORY_MB = "OBJECT_MEMORY_(MB)";

    }

    @Override
    public String getPackageName() {
        return PackageNames.MISCELLANEOUS;

    }

    @Override
    public String getDescription() {
        return "Measure the approximate computer memory required to store this object";

    }

    public static void measureObjectMemory(Obj object) {
        long memoryBytes = PerformanceTest.evaluateObjectMemory(object, false);

        double memoryKB = ((double) memoryBytes / 1024d);
        object.addMeasurement(new Measurement(Measurements.OBJECT_MEMORY_KB, memoryKB));

        double memoryMB = ((double) memoryBytes / 1048576d);
        object.addMeasurement(new Measurement(Measurements.OBJECT_MEMORY_MB, memoryMB));

    }

    @Override
    protected Status process(Workspace workspace) {
        // Getting input objects
        String objectName = parameters.getValue(INPUT_OBJECTS);
        ObjCollection objects = workspace.getObjects().get(objectName);

        int count = 0;
        int total = objects.size();
        for (Obj object : objects.values()) {
            writeMessage("Processing object " + (count++) + " of " + total);
            measureObjectMemory(object);

        }
        
        if (showOutput) 
            objects.showMeasurements(this, modules);

        return Status.PASS;

    }

    @Override
    protected void initialiseParameters() {
        parameters.add(new ParamSeparatorP(INPUT_SEPARATOR, this));
        parameters.add(new InputObjectsP(INPUT_OBJECTS, this));

    }

    @Override
    public ParameterCollection updateAndGetParameters() {
        return parameters;
    }

    @Override
    public ImageMeasurementRefCollection updateAndGetImageMeasurementRefs() {
        return null;
    }

    @Override
    public ObjMeasurementRefCollection updateAndGetObjectMeasurementRefs() {
        ObjMeasurementRefCollection returnedRefs = new ObjMeasurementRefCollection();

        String inputObjectsName = parameters.getValue(INPUT_OBJECTS);
        ObjMeasurementRef meas = objectMeasurementRefs.getOrPut(Measurements.OBJECT_MEMORY_KB);
        meas.setObjectsName(inputObjectsName);
        meas.setDescription("Computer memory (approximate) required to store this instance of \"" + inputObjectsName
                + "\" object.  Measured in KB.");
        returnedRefs.add(meas);

        meas = objectMeasurementRefs.getOrPut(Measurements.OBJECT_MEMORY_MB);
        meas.setObjectsName(inputObjectsName);
        meas.setDescription("Computer memory (approximate) required to store this instance of \"" + inputObjectsName
                + "\" object.  Measured in MB.");
        returnedRefs.add(meas);

        return returnedRefs;

    }

    @Override
    public MetadataRefCollection updateAndGetMetadataReferences() {
        return null;
    }

    @Override
    public ParentChildRefCollection updateAndGetParentChildRefs() {
        return null;
    }

    @Override
    public PartnerRefCollection updateAndGetPartnerRefs() {
        return null;
    }

    @Override
    public boolean verify() {
        return true;
    }
}