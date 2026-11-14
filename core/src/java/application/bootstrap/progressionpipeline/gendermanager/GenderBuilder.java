package application.bootstrap.progressionpipeline.gendermanager;

import java.io.File;

import application.bootstrap.progressionpipeline.gender.GenderData;
import application.bootstrap.progressionpipeline.gender.GenderHandle;
import engine.root.BuilderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class GenderBuilder extends BuilderPackage {

    /*
     * Parses gender ARPG files and builds GenderHandle instances. Each file
     * lists its genders under a "genders" array, each named by its "name"
     * field and shown by its "display_name". Bootstrap-only.
     */

    // Internal
    private GenderManager genderManager;

    // Base \\

    @Override
    protected void get() {
        this.genderManager = get(GenderManager.class);
    }

    // Build \\

    ObjectArrayList<GenderHandle> build(File arpgFile) {

        ArpgObjectStruct rootArpg = ArpgUtility.loadObject(arpgFile);
        ArpgArrayStruct genderArray = ArpgUtility.validateArray(rootArpg, "genders");
        ObjectArrayList<GenderHandle> genders = new ObjectArrayList<>();

        for (int i = 0; i < genderArray.size(); i++)
            genders.add(parseGender(genderArray.get(i).getAsObject()));

        return genders;
    }

    // Parse \\

    private GenderHandle parseGender(ArpgObjectStruct genderArpg) {

        String genderName = ArpgUtility.validateString(genderArpg, "name");
        short genderID = genderManager.registerGenderName(genderName);

        GenderData genderData = new GenderData(
                genderName,
                genderID,
                ArpgUtility.getString(genderArpg, "display_name", genderName));

        GenderHandle genderHandle = create(GenderHandle.class);
        genderHandle.constructor(genderData);

        return genderHandle;
    }
}
