package application.bootstrap.weatherpipeline.seasonmanager;

import java.io.File;

import application.bootstrap.weatherpipeline.season.SeasonData;
import application.bootstrap.weatherpipeline.season.SeasonHandle;
import application.bootstrap.weatherpipeline.season.SkyPaletteStruct;
import application.bootstrap.weatherpipeline.season.SkyPhaseStruct;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import engine.util.mathematics.vectors.Vector3;

class SeasonBuilder extends BuilderPackage {

    /*
     * Parses season ARPG into a SeasonData and wraps it in a SeasonHandle.
     * The season's name is taken directly from the file name — whatever
     * named seasons the active calendar defines is whatever files should
     * exist here. Every fallback below when a climate field is omitted
     * comes from EngineSetting. The "sky" block is required: it carries the
     * season's full sky and cloud palette for every phase of the day named
     * in EngineSetting.SKY_PHASE_NAMES. Bootstrap-only and on-demand.
     */

    // Build \\

    SeasonHandle build(File file, File root) {

        String seasonName = FileUtility.getPathWithFileNameWithoutExtension(root, file);

        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);

        float baseWindSpeed = parseFloat(arpg, "baseWindSpeed", EngineSetting.DEFAULT_SEASON_BASE_WIND_SPEED);
        float windVariance = parseFloat(arpg, "windVariance", EngineSetting.DEFAULT_SEASON_WIND_VARIANCE);
        float prevailingWindDirectionDegrees = parseFloat(
                arpg, "prevailingWindDirectionDegrees",
                EngineSetting.DEFAULT_SEASON_PREVAILING_WIND_DIRECTION_DEGREES);
        float baseTemperature = parseFloat(arpg, "baseTemperature", EngineSetting.DEFAULT_BASE_TEMPERATURE);
        float temperatureVariance = parseFloat(
                arpg, "temperatureVariance", EngineSetting.DEFAULT_SEASON_TEMPERATURE_VARIANCE);
        float precipitationChanceScale = parseFloat(
                arpg, "precipitationChanceScale", EngineSetting.DEFAULT_SEASON_PRECIPITATION_CHANCE_SCALE);
        SkyPaletteStruct skyPalette = parseSkyPalette(arpg, seasonName);

        SeasonData seasonData = new SeasonData(
                seasonName,
                baseWindSpeed,
                windVariance,
                prevailingWindDirectionDegrees,
                baseTemperature,
                temperatureVariance,
                precipitationChanceScale,
                skyPalette);

        SeasonHandle seasonHandle = create(SeasonHandle.class);
        seasonHandle.constructor(seasonData);

        return seasonHandle;
    }

    // Parsing \\

    private float parseFloat(ArpgObjectStruct arpg, String field, float fallback) {

        if (!arpg.has(field))
            return fallback;

        return arpg.get(field).getAsFloat();
    }

    private SkyPaletteStruct parseSkyPalette(ArpgObjectStruct arpg, String seasonName) {

        ArpgObjectStruct skyObject = ArpgUtility.validateObject(arpg, "sky");
        ArpgObjectStruct phasesObject = ArpgUtility.validateObject(skyObject, "phases");

        String[] phaseNames = EngineSetting.SKY_PHASE_NAMES;
        SkyPhaseStruct[] phases = new SkyPhaseStruct[phaseNames.length];

        for (int i = 0; i < phaseNames.length; i++)
            phases[i] = parseSkyPhase(ArpgUtility.validateObject(phasesObject, phaseNames[i]), seasonName);

        float glowStrength = parseStrength(skyObject, "glowStrength", seasonName);
        float beltStrength = parseStrength(skyObject, "beltStrength", seasonName);
        float variety = parseStrength(skyObject, "variety", seasonName);

        return new SkyPaletteStruct(phases, glowStrength, beltStrength, variety);
    }

    private SkyPhaseStruct parseSkyPhase(ArpgObjectStruct phaseObject, String seasonName) {
        return new SkyPhaseStruct(
                parseColor(phaseObject, "zenith", seasonName),
                parseColor(phaseObject, "horizon", seasonName),
                parseColor(phaseObject, "glow", seasonName),
                parseColor(phaseObject, "belt", seasonName),
                parseColor(phaseObject, "cloud", seasonName),
                parseColor(phaseObject, "cloudLight", seasonName),
                parseColor(phaseObject, "cloudShadow", seasonName));
    }

    private float parseStrength(ArpgObjectStruct arpg, String field, String seasonName) {

        float strength = ArpgUtility.validateFloat(arpg, field);

        if (strength < 0f)
            throwException("Season \"" + seasonName + "\" sky " + field + " " + strength +
                    " is out of range — must be 0.0 or greater");

        return strength;
    }

    private Vector3 parseColor(ArpgObjectStruct arpg, String field, String seasonName) {

        ArpgArrayStruct array = ArpgUtility.validateArray(arpg, field, 3);
        Vector3 color = new Vector3(
                array.get(0).getAsFloat(),
                array.get(1).getAsFloat(),
                array.get(2).getAsFloat());

        if (color.x < 0f || color.y < 0f || color.z < 0f)
            throwException("Season \"" + seasonName + "\" sky color \"" + field +
                    "\" has a negative channel — every channel must be 0.0 or greater");

        return color;
    }
}
