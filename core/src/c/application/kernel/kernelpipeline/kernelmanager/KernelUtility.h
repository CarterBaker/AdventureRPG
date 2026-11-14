#ifndef ARPG_KERNEL_UTILITY_H
#define ARPG_KERNEL_UTILITY_H

#include <stdint.h>

#include "application/kernel/kernelpipeline/kernelmanager/KernelSetting.h"

/*
 * The base every kernel source includes, the way every Java utility extends
 * EngineUtility. KERNEL(Name) declares or defines a kernel's one entry point,
 * int ARPG_<Name>_V2 or _V3 (void *arguments), with the suffix chosen by the
 * KERNEL_CPU_LEVEL the build compiles the source at, so a kernel is written
 * once and never names its variant. Only symbols marked KERNEL_EXPORT leave
 * the library.
 */

// Export
#ifdef _WIN32
#define KERNEL_EXPORT __declspec(dllexport)
#else
#define KERNEL_EXPORT __attribute__((visibility("default")))
#endif

// Variant
#if KERNEL_CPU_LEVEL == KERNEL_CPU_LEVEL_V3
#define KERNEL_VARIANT_SUFFIX KERNEL_VARIANT_SUFFIX_V3
#else
#define KERNEL_VARIANT_SUFFIX KERNEL_VARIANT_SUFFIX_V2
#endif

// Symbol
#define KERNEL_JOIN(prefix, name, suffix) prefix##name##suffix
#define KERNEL_SYMBOL(prefix, name, suffix) KERNEL_JOIN(prefix, name, suffix)
#define KERNEL(name) KERNEL_EXPORT int KERNEL_SYMBOL(ARPG_, name, KERNEL_VARIANT_SUFFIX)(void *arguments)

#endif
