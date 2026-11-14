#include <stdint.h>

#include "application/kernel/kernelpipeline/kernelmanager/KernelUtility.h"

#if defined(__x86_64__)
#include <cpuid.h>
#endif

/*
 * The library's fixed exports, compiled once at the baseline level.
 * ARPG_KernelABI() answers the ABI version KernelLoader checks for a stale
 * library, and ARPG_KernelCpuLevel() the highest level this CPU runs: on
 * x86-64 read from cpuid and the OS-enabled register state, so the library
 * needs no C runtime; on arm64 always the baseline, the only level built
 * there. On Windows the library also carries its own empty entry point.
 */

#if defined(__x86_64__)

// Leaves
#define LEAF_FEATURES 1u
#define LEAF_EXTENDED_FEATURES 7u
#define LEAF_EXTENDED_INFO 0x80000001u

// Register State — SSE and AVX state enabled by the OS in XCR0
#define XCR0_SSE_AVX 0x6u

static uint64_t readEnabledState(void) {

    uint32_t low;
    uint32_t high;

    __asm__ volatile("xgetbv" : "=a"(low), "=d"(high) : "c"(0u));

    return ((uint64_t) high << 32) | low;
}

static int hasBits(uint32_t value, uint32_t bits) {
    return (value & bits) == bits;
}

// SSE3, SSSE3, SSE4.1, SSE4.2, POPCNT, CMPXCHG16B and LAHF/SAHF
static int supportsLevelV2(uint32_t featuresC, uint32_t extendedInfoC) {
    return hasBits(featuresC, bit_SSE3 | bit_SSSE3 | bit_SSE41 | bit_SSE42 | bit_POPCNT | bit_CMPXCHG16B)
           && hasBits(extendedInfoC, bit_LAHF_LM);
}

// AVX, AVX2, BMI1, BMI2, F16C, FMA, LZCNT and MOVBE, with the AVX register state enabled by the OS
static int supportsLevelV3(uint32_t featuresC, uint32_t extendedB, uint32_t extendedInfoC) {
    return hasBits(featuresC, bit_AVX | bit_F16C | bit_FMA | bit_MOVBE | bit_OSXSAVE)
           && hasBits(extendedB, bit_AVX2 | bit_BMI | bit_BMI2)
           && hasBits(extendedInfoC, bit_LZCNT)
           && hasBits((uint32_t) readEnabledState(), XCR0_SSE_AVX);
}

// CPU Level
KERNEL_EXPORT int ARPG_KernelCpuLevel(void) {

    uint32_t eax = 0u;
    uint32_t featuresB = 0u;
    uint32_t featuresC = 0u;
    uint32_t featuresD = 0u;
    uint32_t extendedB = 0u;
    uint32_t extendedC = 0u;
    uint32_t extendedD = 0u;
    uint32_t extendedInfoB = 0u;
    uint32_t extendedInfoC = 0u;
    uint32_t extendedInfoD = 0u;

    __get_cpuid(LEAF_FEATURES, &eax, &featuresB, &featuresC, &featuresD);
    __get_cpuid_count(LEAF_EXTENDED_FEATURES, 0u, &eax, &extendedB, &extendedC, &extendedD);
    __get_cpuid(LEAF_EXTENDED_INFO, &eax, &extendedInfoB, &extendedInfoC, &extendedInfoD);

    if (!supportsLevelV2(featuresC, extendedInfoC))
        return KERNEL_CPU_LEVEL_BASELINE;

    if (!supportsLevelV3(featuresC, extendedB, extendedInfoC))
        return KERNEL_CPU_LEVEL_V2;

    return KERNEL_CPU_LEVEL_V3;
}

#else

// CPU Level — ARMv8-A, the baseline every arm64 CPU runs
KERNEL_EXPORT int ARPG_KernelCpuLevel(void) {
    return KERNEL_CPU_LEVEL_V2;
}

#endif

// ABI
KERNEL_EXPORT int ARPG_KernelABI(void) {
    return KERNEL_ABI_VERSION;
}

// Entry
#ifdef _WIN32
int __stdcall DllMainCRTStartup(void *module, uint32_t reason, void *reserved) {

    (void) module;
    (void) reason;
    (void) reserved;

    return 1;
}
#endif
