#ifndef ARPG_KERNEL_SETTING_H
#define ARPG_KERNEL_SETTING_H

/*
 * C-side constants every kernel shares: the ABI version KernelLoader compares
 * with EngineSetting.KERNEL_ABI_VERSION, the status a kernel returns, and the
 * CPU levels with the symbol suffix each variant carries. Level 2 is every
 * architecture's baseline (x86-64-v2, or ARMv8-A on arm64); level 3,
 * x86-64-v3, exists on x86-64 only.
 */

// ABI
#define KERNEL_ABI_VERSION 1

// Status
#define KERNEL_STATUS_SUCCESS 0

// CPU Levels — BASELINE is an x86-64 CPU below v2, which no kernel is built for
#define KERNEL_CPU_LEVEL_BASELINE 1
#define KERNEL_CPU_LEVEL_V2 2
#define KERNEL_CPU_LEVEL_V3 3

// Variant Suffixes
#define KERNEL_VARIANT_SUFFIX_V2 _V2
#define KERNEL_VARIANT_SUFFIX_V3 _V3

#endif
