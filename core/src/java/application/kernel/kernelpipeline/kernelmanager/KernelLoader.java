package application.kernel.kernelpipeline.kernelmanager;

import org.lwjgl.system.JNI;
import org.lwjgl.system.Library;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Platform;
import org.lwjgl.system.SharedLibrary;

import application.kernel.kernelpipeline.kernel.KernelHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

class KernelLoader extends LoaderPackage {

    /*
     * Resolves kernels on demand from the one native library, AdventureRPG,
     * which it opens on the first request so an engine that never calls a
     * kernel never touches native code. Opening it checks the platform,
     * loads it the way LWJGL loads its own natives (from
     * <os>/<arch>/adventurerpg on the classpath, for windows-x64, linux-x64,
     * macos-x64 or macos-arm64), compares its ABI with KERNEL_ABI_VERSION
     * and picks the highest CPU level it reports, capped by the
     * KERNEL_CPU_LEVEL_PROPERTY when set. Each kernel is then resolved at
     * that level's symbol, and a kernel missing from the library is fatal.
     */

    // Internal
    private KernelManager internalKernelManager;
    private KernelBuilder internalBuilder;

    // Catalog
    private ObjectOpenHashSet<String> kernelNames;

    // Library
    private SharedLibrary library;
    private String variantSuffix;

    // Base \\

    @Override
    protected void scan() {
        this.kernelNames = new ObjectOpenHashSet<>(EngineSetting.KERNEL_NAMES);
    }

    @Override
    protected void create() {
        this.internalBuilder = create(KernelBuilder.class);
    }

    @Override
    protected void get() {
        this.internalKernelManager = get(KernelManager.class);
    }

    // Kernels are requested while the game runs, so the loader stays for the engine lifetime
    @Override
    protected boolean persists() {
        return true;
    }

    // On-Demand \\

    void request(String kernelName) {

        if (!kernelNames.contains(kernelName))
            throwException("[KernelManager] No kernel is named \"" + kernelName + "\" in EngineSetting.KERNEL_NAMES");

        if (library == null)
            openLibrary();

        String symbolName = EngineSetting.KERNEL_SYMBOL_PREFIX + kernelName + variantSuffix;
        long functionAddress = library.getFunctionAddress(symbolName);

        if (functionAddress == MemoryUtil.NULL)
            throwException("[KernelManager] Kernel \"" + kernelName + "\" has no symbol " + symbolName
                    + " in the native library. " + EngineSetting.KERNEL_REBUILD_ADVICE);

        int kernelID = internalKernelManager.registerKernelName(kernelName);
        KernelHandle kernelHandle = internalBuilder.build(kernelName, kernelID, symbolName, functionAddress);
        internalKernelManager.addKernelHandle(kernelID, kernelHandle);
    }

    // Library \\

    private void openLibrary() {

        verifyPlatform();

        try {
            this.library = Library.loadNative(
                    KernelLoader.class,
                    EngineSetting.KERNEL_LIBRARY_MODULE,
                    EngineSetting.KERNEL_LIBRARY_NAME,
                    true);
        } catch (UnsatisfiedLinkError e) {
            throwException("[KernelManager] The native kernel library " + EngineSetting.KERNEL_LIBRARY_NAME
                    + " could not be loaded. " + EngineSetting.KERNEL_REBUILD_ADVICE, e);
        }

        internalKernelManager.bindLibrary(library);
        verifyABI();

        int cpuLevel = resolveCpuLevel();

        internalKernelManager.bindCpuLevel(cpuLevel);
        this.variantSuffix = EngineSetting.KERNEL_VARIANT_SUFFIXES[cpuLevel];
    }

    // windows-x64, linux-x64, macos-x64 and macos-arm64
    private void verifyPlatform() {

        Platform platform = Platform.get();
        Platform.Architecture architecture = Platform.getArchitecture();
        boolean built = platform == Platform.WINDOWS || platform == Platform.LINUX || platform == Platform.MACOSX;
        boolean supported = built && (architecture == Platform.Architecture.X64
                || platform == Platform.MACOSX && architecture == Platform.Architecture.ARM64);

        if (!supported)
            throwException("[KernelManager] Native kernels are built for windows-x64, linux-x64, macos-x64 and "
                    + "macos-arm64 only; this is " + platform.getName() + " " + architecture);
    }

    // A library built before the last change to the kernel ABI answers a different version, or none at all
    private void verifyABI() {

        long abiAddress = library.getFunctionAddress(EngineSetting.KERNEL_ABI_SYMBOL);

        if (abiAddress == MemoryUtil.NULL || JNI.invokeI(abiAddress) != EngineSetting.KERNEL_ABI_VERSION)
            throwException("[KernelManager] The native kernel library is stale: it was built for another kernel ABI "
                    + "than version " + EngineSetting.KERNEL_ABI_VERSION + ". " + EngineSetting.KERNEL_REBUILD_ADVICE);
    }

    // The highest level the CPU runs, held to the level the property names when it is set
    private int resolveCpuLevel() {

        int cpuLevel = JNI.invokeI(library.getFunctionAddress(EngineSetting.KERNEL_CPU_LEVEL_SYMBOL));

        if (cpuLevel < EngineSetting.KERNEL_CPU_LEVEL_V2)
            throwException("[KernelManager] This CPU lacks x86-64-v2 (SSE4.2 and POPCNT), the lowest level the "
                    + "native kernels are built for");

        String cappedLevel = System.getProperty(EngineSetting.KERNEL_CPU_LEVEL_PROPERTY);

        return cappedLevel == null ? cpuLevel : Math.min(cpuLevel, parseCpuLevel(cappedLevel.trim()));
    }

    private int parseCpuLevel(String cappedLevel) {

        for (int level = EngineSetting.KERNEL_CPU_LEVEL_V2; level <= EngineSetting.KERNEL_CPU_LEVEL_V3; level++)
            if (cappedLevel.equals(Integer.toString(level)))
                return level;

        return throwException("[KernelManager] " + EngineSetting.KERNEL_CPU_LEVEL_PROPERTY + " must be "
                + EngineSetting.KERNEL_CPU_LEVEL_V2 + " or " + EngineSetting.KERNEL_CPU_LEVEL_V3
                + ", not \"" + cappedLevel + "\"");
    }
}
