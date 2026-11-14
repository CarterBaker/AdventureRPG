package application.kernel.kernelpipeline.kernel;

import engine.root.DataPackage;

public class KernelData extends DataPackage {

    /*
     * One resolved kernel: its name and ID, the variant symbol it was resolved
     * at, the address of that symbol in the native library, and the profiler
     * counter names its calls and time are reported under, built once so no
     * call ever concatenates a string.
     */

    // Identity
    private final String kernelName;
    private final int kernelID;
    private final String symbolName;

    // Native
    private final long functionAddress;

    // Profiler
    private final String callsCounterName;
    private final String millisCounterName;

    // Constructor \\

    public KernelData(
            String kernelName,
            int kernelID,
            String symbolName,
            long functionAddress,
            String callsCounterName,
            String millisCounterName) {

        // Identity
        this.kernelName = kernelName;
        this.kernelID = kernelID;
        this.symbolName = symbolName;

        // Native
        this.functionAddress = functionAddress;

        // Profiler
        this.callsCounterName = callsCounterName;
        this.millisCounterName = millisCounterName;
    }

    // Accessible \\

    public String getKernelName() {
        return kernelName;
    }

    public int getKernelID() {
        return kernelID;
    }

    public String getSymbolName() {
        return symbolName;
    }

    public long getFunctionAddress() {
        return functionAddress;
    }

    public String getCallsCounterName() {
        return callsCounterName;
    }

    public String getMillisCounterName() {
        return millisCounterName;
    }
}
