package application.bootstrap.dialoguepipeline.verdictmanager;

import java.io.File;

import application.bootstrap.dialoguepipeline.util.DialogueFeatureUtility;
import application.bootstrap.dialoguepipeline.verdict.VerdictData;
import application.bootstrap.dialoguepipeline.verdict.VerdictHandle;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class VerdictBuilder extends BuilderPackage {

    /*
     * Parses verdict ARPG files and builds VerdictHandle instances. Each file
     * lists its verdicts under a "verdicts" array, each named by its "name"
     * field: the "display_name" and "description" it is shown by and what it
     * is worth to "understanding", "response" and "wording", each from -1 to
     * 1. Bootstrap-only.
     */

    // Internal
    private VerdictManager verdictManager;

    // Base \\

    @Override
    protected void get() {
        this.verdictManager = get(VerdictManager.class);
    }

    // Build \\

    ObjectArrayList<VerdictHandle> build(File arpgFile) {

        ArpgObjectStruct rootArpg = ArpgUtility.loadObject(arpgFile);
        ArpgArrayStruct verdictArray = ArpgUtility.validateArray(rootArpg, "verdicts");
        ObjectArrayList<VerdictHandle> verdicts = new ObjectArrayList<>();

        for (int i = 0; i < verdictArray.size(); i++)
            verdicts.add(parseVerdict(verdictArray.get(i).getAsObject(), arpgFile));

        return verdicts;
    }

    // Parse \\

    private VerdictHandle parseVerdict(ArpgObjectStruct verdictArpg, File arpgFile) {

        String verdictName = ArpgUtility.validateString(verdictArpg, "name");
        float understanding = ArpgUtility.getFloat(verdictArpg, "understanding", 0f);
        float response = ArpgUtility.getFloat(verdictArpg, "response", 0f);
        float wording = ArpgUtility.getFloat(verdictArpg, "wording", 0f);

        if (Math.abs(understanding) > 1f || Math.abs(response) > 1f || Math.abs(wording) > 1f)
            throwException("Verdict '" + verdictName + "' in " + arpgFile.getPath()
                    + " must be worth between -1 and 1 to \"understanding\", \"response\" and \"wording\".");

        VerdictData verdictData = new VerdictData(
                verdictName,
                verdictManager.registerVerdictName(verdictName),
                DialogueFeatureUtility.hashName(verdictName),
                ArpgUtility.getString(verdictArpg, "display_name", verdictName),
                ArpgUtility.getString(verdictArpg, "description", EngineSetting.DIALOGUE_TEXT_NONE),
                understanding,
                response,
                wording);

        VerdictHandle verdictHandle = create(VerdictHandle.class);
        verdictHandle.constructor(verdictData);

        return verdictHandle;
    }
}
