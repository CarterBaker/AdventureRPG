package application.kernel.kernelpipeline.kernelmanager;

import org.lwjgl.system.JNI;
import org.lwjgl.system.SharedLibrary;

import application.kernel.kernelpipeline.kernel.KernelArgumentInstance;
import application.kernel.kernelpipeline.kernel.KernelHandle;
import application.kernel.profilerpipeline.profilermanager.ProfilerManager;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class KernelManager extends ManagerPackage {

    /*
     * Owns the native kernel library and every kernel handle, and is the one
     * place a kernel's function pointer is ever called: invoke() runs a kernel
     * on the caller's argument block through JNI.invokePI, times the call and
     * turns a failing status into an error naming the kernel. Callers resolve a
     * kernel's name to its ID once, requesting it on a miss, and index by the
     * ID afterwards. Once a frame the calls and time of every kernel go to the
     * profiler, and the library is freed when the engine disposes.
     */

    // Internal
    private ProfilerManager profilerManager;

    // Library
    private SharedLibrary library;
    private int cpuLevel;

    // Palette
    private Object2IntOpenHashMap<String> kernelName2KernelID;
    private ObjectArrayList<KernelHandle> kernelID2KernelHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.kernelName2KernelID = RegistryUtility.createNameIndex();
        this.kernelID2KernelHandle = RegistryUtility.createPalette();
        create(KernelLoader.class);
    }

    @Override
    protected void get() {
        this.profilerManager = get(ProfilerManager.class);
    }

    @Override
    protected void awake() {
        EngineUtility.assignKernelManager(this);
    }

    @Override
    protected void update() {

        for (int kernelID = RegistryUtility.RESERVED_ID + 1; kernelID < kernelID2KernelHandle.size(); kernelID++) {

            KernelHandle kernelHandle = kernelID2KernelHandle.get(kernelID);

            profilerManager.recordCount(kernelHandle.getCallsCounterName(), kernelHandle.drainCallCount());
            profilerManager.recordCount(
                    kernelHandle.getMillisCounterName(),
                    (float) kernelHandle.drainBusyNanos() / EngineSetting.NANOS_PER_MILLI);
        }
    }

    @Override
    protected void dispose() {

        if (library != null)
            library.free();

        this.library = null;
    }

    // Management \\

    void bindLibrary(SharedLibrary library) {
        this.library = library;
    }

    void bindCpuLevel(int cpuLevel) {
        this.cpuLevel = cpuLevel;
    }

    int registerKernelName(String kernelName) {
        return RegistryUtility.registerID(
                kernelName2KernelID,
                kernelID2KernelHandle,
                kernelName,
                EngineSetting.KERNEL_ID_COUNT);
    }

    void addKernelHandle(int kernelID, KernelHandle kernelHandle) {
        kernelID2KernelHandle.set(kernelID, kernelHandle);
    }

    // On-Demand \\

    public void request(String kernelName) {
        ((KernelLoader) internalLoader).request(kernelName);
    }

    // Accessible \\

    // The x86-64 level every kernel was resolved at, once the library is open
    public int getCpuLevel() {
        return cpuLevel;
    }

    public int getKernelIDFromKernelName(String kernelName) {

        int kernelID = kernelName2KernelID.getInt(kernelName);

        if (kernelID == RegistryUtility.ID_NONE) {

            request(kernelName);
            kernelID = kernelName2KernelID.getInt(kernelName);

            if (kernelID == RegistryUtility.ID_NONE)
                throwException("[KernelManager] Kernel not found after load: \"" + kernelName + "\"");
        }

        return kernelID;
    }

    // Invoke \\

    // Any thread — the only call of a kernel's function pointer
    public void invoke(int kernelID, KernelArgumentInstance arguments) {

        KernelHandle kernelHandle = kernelID2KernelHandle.get(kernelID);
        long start = System.nanoTime();
        int status = JNI.invokePI(arguments.getAddress(), kernelHandle.getFunctionAddress());

        kernelHandle.recordCall(System.nanoTime() - start);

        if (status != EngineSetting.KERNEL_STATUS_SUCCESS)
            throwException("[KernelManager] Kernel \"" + kernelHandle.getKernelName() + "\" ("
                    + kernelHandle.getSymbolName() + ") failed with status " + status);
    }
}
