package application.bootstrap.renderpipeline.photofiltermanager;

import application.bootstrap.renderpipeline.photofilter.PhotoFilterHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class PhotoFilterManager extends ManagerPackage {

    /*
     * Owns the photo filter palette: the looks photo mode steps through, in
     * the order their files list them. IDs are assigned in registration
     * order, and a name declared twice is rejected. Every filter is resolved
     * in awake(), and the plain one, laying nothing over the scene, is
     * cached as the look photo mode opens on.
     */

    // Palette
    private Object2IntOpenHashMap<String> photoFilterName2PhotoFilterID;
    private ObjectArrayList<PhotoFilterHandle> photoFilterID2PhotoFilterHandle;

    // Engine Photo Filters
    private short plainPhotoFilterID;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.photoFilterName2PhotoFilterID = RegistryUtility.createNameIndex();
        this.photoFilterID2PhotoFilterHandle = RegistryUtility.createPalette();
        create(PhotoFilterLoader.class);
    }

    @Override
    protected void awake() {

        internalLoader.requestAll();

        // Engine Photo Filters
        this.plainPhotoFilterID = getPhotoFilterIDFromPhotoFilterName(EngineSetting.PHOTO_FILTER_PLAIN);
    }

    // Management \\

    short registerPhotoFilterName(String photoFilterName) {
        return (short) RegistryUtility.registerID(
                photoFilterName2PhotoFilterID, photoFilterID2PhotoFilterHandle, photoFilterName,
                EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    void addPhotoFilterHandle(PhotoFilterHandle photoFilterHandle) {

        short photoFilterID = photoFilterHandle.getPhotoFilterID();

        if (photoFilterID2PhotoFilterHandle.get(photoFilterID) != null)
            throwException("Duplicate photo filter name: '" + photoFilterHandle.getPhotoFilterName()
                    + "' is declared more than once — every photo filter name must be unique");

        photoFilterID2PhotoFilterHandle.set(photoFilterID, photoFilterHandle);
    }

    // Accessible \\

    public boolean hasPhotoFilter(String photoFilterName) {
        return RegistryUtility.getHandle(
                photoFilterName2PhotoFilterID, photoFilterID2PhotoFilterHandle, photoFilterName) != null;
    }

    public short getPhotoFilterIDFromPhotoFilterName(String photoFilterName) {

        if (!hasPhotoFilter(photoFilterName) && internalLoader != null)
            ((PhotoFilterLoader) internalLoader).request(photoFilterName);

        if (!hasPhotoFilter(photoFilterName))
            throwException("Photo filter not found after load: \"" + photoFilterName + "\"");

        return (short) photoFilterName2PhotoFilterID.getInt(photoFilterName);
    }

    public PhotoFilterHandle getPhotoFilterHandleFromPhotoFilterID(short photoFilterID) {

        PhotoFilterHandle handle = RegistryUtility.getHandle(photoFilterID2PhotoFilterHandle, photoFilterID);

        if (handle == null)
            throwException("Photo filter ID not found: " + photoFilterID);

        return handle;
    }

    // How many filters the palette holds, the sentinel slot left out; their IDs run from 1 to this
    public int getPhotoFilterCount() {
        return photoFilterID2PhotoFilterHandle.size() - 1;
    }

    // The filter a number of places after another in palette order, round from the last to the first
    public short stepPhotoFilterID(short photoFilterID, int direction) {

        int count = photoFilterID2PhotoFilterHandle.size() - 1;

        return (short) (Math.floorMod(photoFilterID - 1 + direction, count) + 1);
    }

    // Engine Photo Filters \\

    public short getPlainPhotoFilterID() {
        return plainPhotoFilterID;
    }
}
