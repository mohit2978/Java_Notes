# Java Garbage Collection: Architecture, Algorithms, Diagnostics & Production Tuning Guide

Garbage Collection (GC) in Java is the automatic memory management process provided by the Java Virtual Machine (JVM). It tracks object allocations, determines which objects are no longer reachable by the running program, and reclaims their memory from the **Heap** without manual intervention (`free()` or `delete`).

---

## 1. JVM Memory Architecture & The Generational Hypothesis

To understand how GC works, we must first examine how memory is structured inside the JVM and how objects live and die.

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                       JVM MEMORY                                       │
├────────────────────────────────────────────────────────────┬───────────────────────────┤
│                        HEAP MEMORY                         │      NON-HEAP MEMORY      │
│  (Shared across all threads; Managed by Garbage Collector) │  (Metaspace, Threads)     │
│                                                            │                           │
│  ┌───────────────────────────────┬──────────────────────┐  │  ┌─────────────────────┐  │
│  │       YOUNG GENERATION        │   OLD GENERATION     │  │  │      METASPACE      │  │
│  │                               │    (Tenured)         │  │  │ (Class definitions, │  │
│  │ ┌──────────┬────────┬───────┐ │                      │  │  │  method bytecode,   │  │
│  │ │   Eden   │ Survivor│Survivor│                      │  │  │  constant pool)     │  │
│  │ │  Space   │   S0   │  S1   │ │                      │  │  └─────────────────────┘  │
│  │ │          │ (From) │ (To)  │ │                      │  │  ┌─────────────────────┐  │
│  │ └──────────┴────────┴───────┘ │                      │  │  │    THREAD STACKS    │  │
│  └───────────────────────────────┴──────────────────────┘  │  │ (Local primitives,  │  │
│                                                            │  │  object references) │  │
│                                                            │  └─────────────────────┘  │
└────────────────────────────────────────────────────────────┴───────────────────────────┘
```

### Stack vs Heap

| Attribute | Stack Memory | Heap Memory |
|---|---|---|
| **Purpose** | Method execution frames, local primitive variables, references to heap objects. | Stores all actual object instances and array allocations. |
| **Scope & Lifetime** | Thread-isolated; created when entering a method, freed immediately when exiting. | Globally shared across all threads; managed exclusively by the GC. |
| **Speed** | Very fast (L1/L2 cache-friendly, simple pointer increment/decrement). | Slower allocation and management overhead compared to stack. |
| **Failure Mode** | `java.lang.StackOverflowError` (e.g., infinite recursion). | `java.lang.OutOfMemoryError: Java heap space`. |

---

### The Weak Generational Hypothesis

Decades of runtime telemetry across thousands of Java workloads established two empirical truths:
1. **Most objects die young:** The vast majority (>90–95%) of allocated objects become unreachable shortly after creation (e.g., local DTOs, string builders, intermediate lambda results).
2. **References from old to young objects are rare:** Objects that survive multiple collection rounds tend to stay alive for long durations (e.g., caches, Spring singleton beans, connection pools).

Because of this, the JVM divides the Heap into **Generations**:
- **Young Generation:**
  - **Eden Space:** Where all new objects are initially allocated via the `new` keyword (except very large / humongous objects which may go straight to Old Gen).
  - **Survivor Spaces (`S0 / From` and `S1 / To`):** Two equally-sized semi-spaces. Surviving objects from Eden are copied between S0 and S1 during Minor GCs. At any given moment, one Survivor space is active (`From`), while the other is completely empty (`To`).
- **Old (Tenured) Generation:**
  - Holds long-lived objects that survived a configurable number of collection cycles (the **Tenuring Threshold**, configured via `-XX:MaxTenuringThreshold=15`).
- **Metaspace (Java 8+ Native Memory):**
  - Replaced Java 7's `PermGen`. It lives outside the Java heap in native OS memory and holds class metadata, constant pools, and method bytecode. It is also garbage collected when ClassLoaders become unreachable.

---

### Object Reachability & GC Roots

How does the GC determine if an object is "alive" or "dead"? 

The JVM does **not** use reference counting (which fails on cyclic references). Instead, it uses **Tracing Reachability** starting from a set of **GC Roots**.

```
    [ GC Roots ]
    ├── Thread Stack Local References (vars inside running methods)
    ├── Static Class Fields
    ├── JNI (Java Native Interface) Global / Local References
    └── JVM System Classes & Thread Monitors (synchronized objects)
           │
           ▼
    ┌─────────────┐       ┌─────────────┐
    │  Object A   │──────▶│  Object B   │ (ALIVE - Reachable from Root)
    └─────────────┘       └─────────────┘
                                 │
                                 ▼
                          ┌─────────────┐
                          │  Object C   │ (ALIVE)
                          └─────────────┘

    ┌─────────────┐       ┌─────────────┐
    │  Object X   │◀─────▶│  Object Y   │ (DEAD - Cyclic reference, but
    └─────────────┘       └─────────────┘  unreachable from any GC Root -> Collected!)
```

An object is garbage collected if there is **no chain of references** connecting it to any active GC Root.

#### Reference Types in Java

Java provides 4 levels of object references in `java.lang.ref.*`:

1. **Strong Reference (`Object obj = new Object();`):**
   - Default reference. The GC will **never** collect this object as long as a strong reference exists, even if JVM throws `OutOfMemoryError`.
2. **Soft Reference (`SoftReference<T>`):**
   - Collected **only** when the JVM is under critical memory pressure before throwing an `OutOfMemoryError`. Useful for memory-sensitive caches.
3. **Weak Reference (`WeakReference<T>`):**
   - Collected as soon as the next GC cycle runs, regardless of heap memory availability. Used in `WeakHashMap` and thread-local cleanups.
4. **Phantom Reference (`PhantomReference<T>`):**
   - Does not prevent object finalization. Used with a `ReferenceQueue` to perform post-mortem resource cleanup without resurrection hazards (replaces deprecated `finalize()`).

---

## 2. When Does Garbage Collection Run?

The Garbage Collector is executed by background JVM daemon threads. It is **non-deterministic**; application code cannot predict down to the millisecond when a collection will occur.

### GC Event Triggers

| GC Type | Trigger Condition | Scope | Impact |
|---|---|---|---|
| **Minor GC (Young GC)** | **Eden Space Full:** A thread tries to allocate an object and the Eden space has insufficient contiguous memory. | Young Gen only (Eden + Survivors). | Fast; typically 1ms to 20ms. Most objects are reclaimed immediately. |
| **Major GC (Old GC)** | **Old Gen Full / Promotion Failure:** Tenured generation fills up, or surviving objects from Young Gen cannot fit into Old Gen. | Old Generation. | Slower; scans larger memory volumes and may compact data. |
| **Full GC** | **Critical Memory Saturation:** Old Gen is exhausted, Metaspace threshold (`MetaspaceSize`) exceeded, or explicit invocation. | Entire JVM Heap (Young + Old + Metaspace). | Slowest; long Stop-The-World (STW) pauses. Can cause service latency spikes. |
| **Concurrent Marking Cycle** | **Occupancy Threshold:** Old Gen usage reaches `InitiatingHeapOccupancyPercent` (IHOP, default 45% in G1). | Background marking of live objects in Old Gen. | Runs concurrently alongside application threads with minimal pauses. |

---

### The Myth of `System.gc()`

Calling `System.gc()` or `Runtime.getRuntime().gc()` in Java code is an anti-pattern:
- It only **suggests** that the JVM expend effort toward recycling unused objects; the JVM is free to ignore it.
- If not ignored, it typically triggers a **Full Stop-The-World GC**, freezing all application threads across all CPU cores.
- Third-party libraries (e.g., RMI, old database drivers) sometimes invoke `System.gc()`.

> [!WARNING]
> **Production Recommendation:** Always disable explicit GC calls by passing this JVM flag:
> ```bash
> -XX:+DisableExplicitGC
> ```
> This makes `System.gc()` a no-op, preventing unexpected latency spikes caused by rogue library calls.

---

### What is a "Stop-The-World" (STW) Pause?

During certain GC phases, all application execution threads must be suspended so that memory pointers can be safely traversed or relocated without race conditions:
1. JVM brings all application threads to a **Safepoint** (points in compiled code where thread execution state is consistent, e.g., loop boundaries, method returns).
2. All user threads pause.
3. The GC threads perform root scanning, object evacuation, or pointer compaction.
4. Application threads are resumed.

Minimizing STW pause duration (p99 and p99.9 latency) is the primary motivation behind modern collectors like **G1**, **ZGC**, and **Shenandoah**.

---

## 3. Core Garbage Collection Algorithms

All JVM collectors are built upon three foundational algorithmic techniques:

```
1. MARK-SWEEP
[ Live ][ Dead ][ Live ][ Dead ][ Live ]  ──Sweep──▶  [ Live ][ Free ][ Live ][ Free ][ Live ]
(Fast, but causes MEMORY FRAGMENTATION)

2. MARK-SWEEP-COMPACT
[ Live ][ Free ][ Live ][ Free ][ Live ]  ──Compact▶  [ Live ][ Live ][ Live ][ Free Free Free ]
(Eliminates fragmentation, but moving objects updates all memory references -> Higher STW)

3. MARK-COPY (SCAVENGE)
From Space: [ Live A ][ Dead ][ Live B ]  ──Copy───▶  To Space:   [ Live A ][ Live B ][ Free   ]
                                                      From Space: [ Free   ][ Free   ][ Free   ]
(Extremely fast for spaces with high death rates like Eden / Survivor)
```

### 1. Mark-Sweep
- **Phase 1 (Mark):** Traverses the object graph from GC roots and marks all reachable objects as "alive".
- **Phase 2 (Sweep):** Scans the heap linearly and adds the addresses of unreferenced objects to a "free list".
- **Downside:** Causes **memory fragmentation**. Even if 500MB of total heap is free, an allocation for a contiguous 10MB byte array might fail if free memory is split into tiny disjoint holes.

### 2. Mark-Sweep-Compact
- **Phase 1 & 2:** Marks live objects and sweeps dead ones.
- **Phase 3 (Compact):** Shifts all surviving live objects toward the beginning of the memory space, leaving a single large, contiguous block for new allocations.
- **Downside:** Moving objects requires rewriting all references pointing to them across the entire heap, which increases STW pause times.

### 3. Mark-Copy (Scavenge)
- Memory is split into two equal regions. Live objects are identified and copied contiguously into the second region. The first region is then wiped clean in one sweep.
- **Used In:** Young Generation (Eden $\rightarrow$ Survivor S0/S1). Since >90% of young objects are dead, copying the 5-10% survivors is fast and leaves no fragmentation.

---

## 4. Deep Dive: All HotSpot Garbage Collectors

HotSpot includes several distinct collectors, each tailored for different trade-offs between **Throughput**, **Latency**, and **Footprint**.

```
                           JVM GARBAGE COLLECTORS
                                     │
      ┌──────────────────┬───────────┴───────────┬──────────────────┐
      ▼                  ▼                       ▼                  ▼
[ Serial GC ]     [ Parallel GC ]            [ G1 GC ]      [ Low-Latency GCs ]
Single-threaded   Multi-threaded STW       Region-based      Concurrent relocation
Small heaps/CLI   Max Throughput/Batch    Balanced (Default)  ZGC & Shenandoah (<1ms)
```

---

### 1. Serial Collector (`-XX:+UseSerialGC`)
- **How it works:** Uses a single thread for both Young (Mark-Copy) and Old (Mark-Sweep-Compact) collections. Completely freezes all application threads during collection.
- **Pros:** Minimal memory footprint; no multi-threading synchronization overhead.
- **Cons:** Long STW pauses on large heaps. Cannot utilize multi-core CPUs.
- **Best For:** Single-core virtual machines, lightweight microservices with heaps $< 100\text{ MB}$, embedded devices, or quick command-line tools.

---

### 2. Parallel Collector / Throughput GC (`-XX:+UseParallelGC`)
- **How it works:** Also known as the Throughput Collector. Uses multiple worker threads in parallel to perform Minor and Major GCs. Application threads are completely paused during collections (STW).
- **Goal:** Maximizes **Throughput** (percentage of CPU time spent executing application code vs. spent in GC).
  $$\text{Throughput} = \frac{T_{\text{application}}}{T_{\text{application}} + T_{\text{GC}}}$$
- **Pros:** High processing capacity for batch workloads; compacts memory efficiently.
- **Cons:** Pause times scale directly with heap size (can easily reach multiple seconds on heaps $> 8\text{ GB}$).
- **Best For:** Non-interactive batch jobs, ETL pipelines, machine learning training tasks, and offline report generators where peak throughput matters more than individual request pause times.

---

### 3. Concurrent Mark Sweep (CMS) (`-XX:+UseConcMarkSweepGC`)
- **History:** Designed as the first low-pause collector. Performed marking and sweeping concurrently with application threads in Old Gen.
- **Why it was deprecated & removed:**
  - Deprecated in Java 9, **completely removed in Java 14**.
  - It did **not compact** memory, resulting in severe heap fragmentation over time.
  - When memory fragmented, it suffered catastrophic **Concurrent Mode Failures**, falling back to a single-threaded Full GC that froze apps for tens of seconds.
- **Replaced by:** G1 GC and ZGC.

---

### 4. Garbage-First Collector (G1 GC) (`-XX:+UseG1GC`)
- **Status:** **Default garbage collector since Java 9** (Java 9, 11, 17, 21+).
- **Architecture:** Replaces contiguous generational memory partitions with hundreds or thousands of uniform **Regions** (typically 1MB to 32MB each).

```
┌───────┬───────┬───────┬───────┬───────┬───────┬───────┬───────┐
│   E   │   S   │  Free │   O   │   E   │   H   │  Free │   O   │
├───────┼───────┼───────┼───────┼───────┼───────┼───────┼───────┤
│  Free │   O   │   E   │  Free │   S   │   O   │   E   │  Free │
└───────┴───────┴───────┴───────┴───────┴───────┴───────┴───────┘
 Legend: [E] Eden  [S] Survivor  [O] Old (Tenured)  [H] Humongous (>50% Region Size)
```

#### How G1 Works:
1. **Dynamic Roles:** Regions dynamically take on the role of Eden, Survivor, Old, or Humongous.
2. **Garbage-First Principle:** G1 continuously monitors regions to track how much reclaimable garbage each holds. When running a collection, it prioritizes regions with the most garbage first (hence "Garbage-First").
3. **Soft Target Pause Times:** You give G1 a target maximum pause time (default: 200ms):
   ```bash
   -XX:MaxGCPauseMillis=200
   ```
   G1 adjusts the number of regions collected in each cycle to meet this target pause time SLA.
4. **Mixed Collections:** Reclaims both Young Gen regions and a selected subset of the most fragmented Old Gen regions concurrently.
5. **Remembered Sets (RSets) & Card Tables:** Enable G1 to collect a region without scanning the rest of the entire heap for cross-region references.
6. **Humongous Objects:** Objects that exceed 50% of a region size are allocated in contiguous Humongous regions directly in Old Gen.

---

### 5. Z Garbage Collector (ZGC) (`-XX:+UseZGC`)
- **Status:** Production-ready in Java 15+. Massively upgraded to **Generational ZGC in Java 21**.
- **Performance:** Guarantees **sub-millisecond (< 1ms)** pause times, regardless of heap size (from 16MB up to 16 Terabytes!).
- **How it achieves ultra-low latency:**
  - Performs **all** heavy lifting concurrently: marking, reference processing, and **relocation/compaction** while application threads are actively executing!
  - **Colored Pointers:** Uses reference metadata bits inside the 64-bit object pointer itself (Marked0, Marked1, Remapped).
  - **Load Barriers:** When an application thread dereferences an object pointer that is being relocated, the JIT-compiled load barrier detects the stale pointer, redirects the read to the new object location immediately, and updates the reference in place ("self-healing pointers").

```
64-bit Reference Pointer in ZGC:
+-------------------+----------------+-----------------------------------------------+
| 16 bits (Unused)  | 4 bits (Color) | 44 bits (Object Virtual Memory Address)       |
| 00000000 00000000 | Finalizable    | Supports up to 16 Terabytes of address space  |
|                   | Remapped       |                                               |
|                   | Marked1        |                                               |
|                   | Marked0        |                                               |
+-------------------+----------------+-----------------------------------------------+
```

#### Generational ZGC (Java 21+)
In JDK 21, JEP 439 introduced Generational ZGC, separating young and old objects:
```bash
# Enable Generational ZGC in Java 21+
-XX:+UseZGC -XX:+ZGenerational
```
Generational ZGC delivers higher throughput and lower CPU overhead while preserving sub-millisecond p99.9 latency SLAs.

---

### 6. Shenandoah GC (`-XX:+UseShenandoahGC`)
- **Status:** Developed by Red Hat, integrated into OpenJDK 12+ (and backported to 8u/11u).
- **Core Philosophy:** Ultra-low pause time collector designed to perform concurrent evacuation alongside application threads.
- **Mechanism:**
  - Uses **Brooks Pointers** (forwarding pointer in each object header) and modern **Load-Reference Barriers (LRB)** to intercept reads/writes while objects are copied concurrently.
  - Pauses are restricted to brief root scans (typically 1ms - 5ms), independent of total heap size.

---

### 7. Epsilon GC (`-XX:+UseEpsilonGC`)
- **Status:** Available since Java 11 (JEP 318).
- **The "No-Op" Collector:** Handles memory allocation via pointer bumping, but **never frees or reclaims memory**.
- **Behavior:** Once the heap is exhausted, the JVM immediately terminates with `java.lang.OutOfMemoryError: Java heap space`.
- **Use Cases:**
  - Performance benchmarking to isolate application allocation overhead from GC overhead.
  - Ultra-short-lived tasks, batch jobs, CLI utilities, and AWS Lambda serverless functions that allocate little memory and terminate in seconds.
  - Ultra-low latency trading systems with zero-allocation design (all objects pre-allocated at boot).

---

### Comprehensive GC Comparison Matrix

| Collector | JVM Flag | Target SLA | Pause Time (Typical) | Heap Range | Default In | Best For |
|---|---|---|---|---|---|---|
| **Serial** | `-XX:+UseSerialGC` | Minimal footprint | High ($100\text{ms} - 5\text{s}+$) | $< 100\text{ MB}$ | Client VMs | CLI tools, embedded, tiny micro-containers. |
| **Parallel** | `-XX:+UseParallelGC` | Maximum throughput | High ($200\text{ms} - 10\text{s}+$) | $1\text{ GB} - 32\text{ GB}$ | Java 8 | Batch jobs, data warehousing, offline computation. |
| **G1 GC** | `-XX:+UseG1GC` | Balanced throughput & latency | Tunable ($50\text{ms} - 200\text{ms}$) | $4\text{ GB} - 64\text{ GB}+$ | Java 9 – 21+ | Enterprise backends, REST APIs, general-purpose microservices. |
| **ZGC** | `-XX:+UseZGC` | Ultra-low latency | Sub-millisecond ($< 1\text{ms}$) | $8\text{ GB} - 16\text{ TB}$ | Optional | Financial trading, gaming backends, high-SLA microservices. |
| **Shenandoah** | `-XX:+UseShenandoahGC` | Ultra-low latency | Low ($1\text{ms} - 10\text{ms}$) | $4\text{ GB} - 100\text{ GB}+$ | Optional | High responsiveness requirements across medium-to-large heaps. |
| **Epsilon** | `-XX:+UseEpsilonGC` | Zero GC overhead | 0ms (No GC!) | Up to available RAM | Optional | Short-lived Serverless/Lambda, zero-allocation benchmarking. |

---

## 5. How to Know Which GC is Running & Which We Are Using

There are several ways to detect which Garbage Collector is active, both from outside the JVM (terminal/diagnostics) and inside application code.

### 1. Default GC by Java Version

If you do not specify any `-XX:+Use...` flag, the JVM automatically selects the default collector using its **Ergonomics** engine:

| Java Version | Server-Class Machine Default ( $\ge 2$ CPUs, $\ge 2\text{ GB}$ RAM) | Single-core / Small RAM Default |
|---|---|---|
| **Java 8** | **Parallel GC** (`ParallelGC`) | **Serial GC** |
| **Java 9 to 21+** | **G1 GC** (`G1 Garbage Collector`) | **Serial GC** |

---

### 2. Inspecting via Command Line (Before Starting)

To see what flags the JVM would choose by default on your current system and JDK:

```bash
java -XX:+PrintCommandLineFlags -version
```

#### Example Output on JDK 21:
```text
-XX:ConcGCThreads=3 -XX:G1ConcRefinementThreads=10 -XX:GCDrainStackTargetSize=64 
-XX:InitialHeapSize=268435456 -XX:MarkStackSize=4194304 -XX:MaxHeapSize=4294967296 
-XX:MinHeapSize=6815736 -XX:+PrintCommandLineFlags -XX:ReservedCodeCacheSize=251658240 
-XX:+SegmentedCodeCache -XX:+UseCompressedClassPointers -XX:+UseCompressedOops 
-XX:+UseG1GC 
openjdk version "21.0.2" 2024-01-16 LTS
```
Look for `-XX:+UseG1GC` (or `-XX:+UseParallelGC`). That indicates the default active collector.

---

### 3. Inspecting a Running Production Process (Using Process ID / PID)

First, find your Java application's PID:
```bash
# List Java processes
jps -v
# Output example: 48122 OrderServiceApplication -Xms2g -Xmx4g ...
```

#### Option A: Using `jcmd VM.flags`
```bash
jcmd 48122 VM.flags | grep Use
```
*Output:*
```text
-XX:+UseCompressedClassPointers -XX:+UseCompressedOops -XX:+UseG1GC
```

#### Option B: Using `jcmd GC.heap_info`
```bash
jcmd 48122 GC.heap_info
```
*Output (G1 GC):*
```text
 garbage-first heap   total 4194304K, used 1248230K [0x0000000600000000, 0x0000000700000000)
  region size 2048K, 128 young (262144K), 16 survivors (32768K)
 Metaspace       used 58492K, committed 59648K, reserved 1114112K
```
*Output (Parallel GC):*
```text
 PSYoungGen      total 1123328K, used 452110K
  eden space 962560K, 46% used
  from space 160768K, 5% used
  to   space 160768K, 0% used
 ParOldGen       total 2489856K, used 842100K
```

#### Option C: Querying specific flags with `jinfo`
```bash
# Check if G1 is active
jinfo -flag UseG1GC 48122
# Output: -XX:+UseG1GC

# Check if ZGC is active
jinfo -flag UseZGC 48122
# Output: -XX:-UseZGC  (the '-' minus indicates it is disabled)
```

#### Option D: Real-Time Monitoring with `jstat -gc`
To watch live garbage collection counts and pause durations every 1 second:
```bash
jstat -gc 48122 1000
```
*Output:*
```text
 S0C    S1C    S0U    S1U      EC       EU        OC         OU       MC     MU     YGC     YGCT    FGC    FGCT     GCT   
32768  32768   0.0   4128.0 262144.0 184512.0  1048576.0   312450.0  65536  58490    24    0.412     0    0.000    0.412
```

**Key `jstat` Columns Explained:**
- `S0C / S1C`: Survivor 0 and Survivor 1 Capacity (KB).
- `S0U / S1U`: Survivor 0 and Survivor 1 Utilization (KB).
- `EC / EU`: Eden Space Capacity / Utilization (KB).
- `OC / OU`: Old Space Capacity / Utilization (KB).
- `MC / MU`: Metaspace Capacity / Utilization (KB).
- `YGC`: Total number of Young (Minor) GC events.
- `YGCT`: Total cumulative time spent in Young GC (in seconds).
- `FGC`: Total number of Full GC events.
- `FGCT`: Total cumulative time spent in Full GC (in seconds).
- `GCT`: Total cumulative GC pause time ($YGCT + FGCT$).

---

### 4. Programmatic Check in Java Code

You can inspect the active collector at runtime using `java.lang.management.ManagementFactory`:

```java
package com.example.gc;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.util.List;

public class DetectActiveGC {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  ACTIVE JVM GARBAGE COLLECTOR & MEMORY MONITOR   ");
        System.out.println("==================================================");

        // 1. Query all registered GarbageCollectorMXBeans
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

        for (GarbageCollectorMXBean gcBean : gcBeans) {
            System.out.println("Collector Name    : " + gcBean.getName());
            System.out.println("Collections Count : " + gcBean.getCollectionCount());
            System.out.println("Accumulated Time  : " + gcBean.getCollectionTime() + " ms");
            System.out.print("Managed Pools     : ");
            for (String poolName : gcBean.getMemoryPoolNames()) {
                System.out.print("[" + poolName + "] ");
            }
            System.out.println("\n--------------------------------------------------");
        }

        // 2. Identify the Collector Type from Bean Names
        String primaryCollector = identifyCollectorFromBeans(gcBeans);
        System.out.println("\n>>> DIAGNOSTIC RESULT: You are running [" + primaryCollector + "] <<<\n");

        // 3. Query Memory Pools
        System.out.println("Memory Pools Breakdown:");
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            long usedMb = pool.getUsage().getUsed() / (1024 * 1024);
            long maxMb = pool.getUsage().getMax() / (1024 * 1024);
            System.out.printf(" - %-25s : %6d MB used / %6d MB max%n", pool.getName(), usedMb, maxMb);
        }
    }

    private static String identifyCollectorFromBeans(List<GarbageCollectorMXBean> gcBeans) {
        for (GarbageCollectorMXBean bean : gcBeans) {
            String name = bean.getName();
            if (name.contains("G1")) {
                return "G1 GC (Garbage-First)";
            } else if (name.contains("ZGC")) {
                return "ZGC (Z Garbage Collector)";
            } else if (name.contains("Shenandoah")) {
                return "Shenandoah GC";
            } else if (name.contains("PS MarkSweep") || name.contains("Parallel")) {
                return "Parallel GC (Throughput Collector)";
            } else if (name.contains("ConcurrentMarkSweep")) {
                return "CMS (Concurrent Mark Sweep)";
            } else if (name.contains("Copy") || name.contains("MarkSweepCompact")) {
                return "Serial GC";
            } else if (name.contains("Epsilon")) {
                return "Epsilon GC (No-Op)";
            }
        }
        return "Unknown / Custom GC";
    }
}
```

#### Bean Names Mapping Table:

| If `GarbageCollectorMXBean.getName()` displays... | The Active Collector is: |
|---|---|
| `G1 Young Generation` and `G1 Old Generation` | **G1 GC** |
| `PS Scavenge` and `PS MarkSweep` | **Parallel GC** |
| `Copy` and `MarkSweepCompact` | **Serial GC** |
| `ParNew` and `ConcurrentMarkSweep` | **CMS GC** |
| `ZGC Cycles` and `ZGC Pauses` (or `ZGC Major/Minor` in JDK 21) | **ZGC** |
| `Shenandoah Cycles` and `Shenandoah Pauses` | **Shenandoah GC** |

---

### 5. Inspecting via Spring Boot Actuator

If your application uses Spring Boot Actuator:
```bash
# Query GC pause metrics
curl -s http://localhost:8080/actuator/metrics/jvm.gc.pause | jq .
```
*Output snippet:*
```json
{
  "name": "jvm.gc.pause",
  "description": "Time spent in GC pause",
  "baseUnit": "seconds",
  "measurements": [
    { "statistic": "COUNT", "value": 38 },
    { "statistic": "TOTAL_TIME", "value": 0.482 },
    { "statistic": "MAX", "value": 0.024 }
  ],
  "availableTags": [
    { "tag": "action", "values": ["end of minor GC", "end of major GC"] },
    { "tag": "cause", "values": ["G1 Evacuation Pause", "Metadata GC Threshold"] }
  ]
}
```
Notice `cause: G1 Evacuation Pause` immediately informs you that **G1 GC** is running.

---

## 6. Can We Change the Garbage Collector Used?

**Yes, absolutely.** You can switch the garbage collector at JVM launch time by passing the appropriate `-XX:+Use...` startup flag.

> [!IMPORTANT]
> **Key Rule:** You **cannot** change the Garbage Collector dynamically while the JVM is running. The GC is a foundational engine initialized during JVM bootstrapping; changing it requires a process restart with updated JVM startup flags.

---

### The Switch Flags

To select a specific collector, add one of the following flags to your `java` command line:

```bash
# 1. Use G1 GC (Recommended default for general services)
java -XX:+UseG1GC -jar app.jar

# 2. Use ZGC (Java 15+, for sub-millisecond pause times)
java -XX:+UseZGC -jar app.jar

# 3. Use Generational ZGC (Java 21+)
java -XX:+UseZGC -XX:+ZGenerational -jar app.jar

# 4. Use Shenandoah GC (Ultra-low latency alternative)
java -XX:+UseShenandoahGC -jar app.jar

# 5. Use Parallel GC (High throughput for batch workloads)
java -XX:+UseParallelGC -jar app.jar

# 6. Use Serial GC (Single-threaded for tiny memory footprints)
java -XX:+UseSerialGC -jar app.jar

# 7. Use Epsilon GC (No-Op, for testing/benchmarks)
java -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC -jar app.jar
```

---

### How to Configure the GC in Different Environments

#### 1. Direct Command Line / Systemd Service
In your startup script or `/etc/systemd/system/myapp.service`:
```ini
[Unit]
Description=Payment Gateway Service

[Service]
User=appuser
Environment="JAVA_OPTS=-Xms4g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=150 -XX:+DisableExplicitGC"
ExecStart=/usr/bin/java $JAVA_OPTS -jar /opt/myapp/app.jar

[Install]
WantedBy=multi-user.target
```

#### 2. Global Environment Variable (`JAVA_TOOL_OPTIONS`)
The JVM automatically parses options set in `JAVA_TOOL_OPTIONS` across containers, build tools, and terminal sessions without modifying scripts:
```bash
export JAVA_TOOL_OPTIONS="-XX:+UseZGC -XX:+ZGenerational -Xms8g -Xmx8g"
java -jar app.jar
```

#### 3. Docker Container (`Dockerfile`)
```dockerfile
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY target/order-service.jar app.jar

# Configure optimal container GC flags
# -XX:+UseContainerSupport enables JVM to respect cgroup memory limits automatically
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -XX:MaxGCPauseMillis=100 \
               -XX:+DisableExplicitGC \
               -Xlog:gc*:file=/app/logs/gc.log:time,uptime,pid:filecount=5,filesize=50M"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

#### 4. Kubernetes Deployment Manifest
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
  namespace: production
spec:
  replicas: 3
  template:
    spec:
      containers:
        - name: order-service
          image: myregistry.com/order-service:1.2.0
          resources:
            requests:
              memory: "4Gi"
              cpu: "2000m"
            limits:
              memory: "4Gi"
              cpu: "2000m"
          env:
            - name: JAVA_TOOL_OPTIONS
              value: >-
                -Xms3g -Xmx3g
                -XX:+UseG1GC
                -XX:MaxGCPauseMillis=100
                -XX:InitiatingHeapOccupancyPercent=45
                -XX:+DisableExplicitGC
                -Xlog:gc=info:stdout:time,uptime
```

#### 5. Maven (`pom.xml` for Spring Boot Plugin)
If running locally via `mvn spring-boot:run`:
```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <jvmArguments>-XX:+UseZGC -XX:+ZGenerational -Xms2g -Xmx2g</jvmArguments>
    </configuration>
</plugin>
```

#### 6. Gradle (`build.gradle.kts`)
```kotlin
tasks.withType<JavaExec> {
    jvmArgs = listOf(
        "-XX:+UseG1GC",
        "-XX:MaxGCPauseMillis=100",
        "-Xms2g",
        "-Xmx2g"
    )
}
```

---

## 7. Production GC Logging: Modern Unified JVM Logging (Java 9 – 21+)

In modern Java (Java 9+), legacy logging flags like `-XX:+PrintGCDetails` and `-XX:+PrintGCDateStamps` have been superseded by the **Unified JVM GC Logging** framework (`-Xlog`).

### Recommended Production GC Logging Configuration:

```bash
-Xlog:gc*,gc+phases=debug:file=/var/log/app/gc-%t.log:time,uptime,pid,level,tags:filecount=5,filesize=100M
```

#### Syntax Breakdown:
- `gc*`: Captures all GC-related log tags (`gc,heap`, `gc,metaspace`, `gc,ergo`, etc.).
- `gc+phases=debug`: Logs detailed sub-phase durations (useful to diagnose safepoints and evacuation pauses).
- `file=/var/log/app/gc-%t.log`: Writes to a designated file. `%t` appends a launch timestamp to prevent overwriting across restarts.
- `time,uptime,pid,level,tags`: Decorators prefixing each log line with UTC ISO-8601 timestamp, uptime in seconds, process ID, log level, and tag name.
- `filecount=5,filesize=100M`: Automatic log rotation: retains the latest 5 files of 100MB each (max 500MB disk usage).

#### Example Output from Modern Unified GC Log:
```text
[2026-09-28T02:15:30.125+0000][0.845s][48122][info][gc,start    ] GC(12) Pause Young (Normal) (G1 Evacuation Pause)
[2026-09-28T02:15:30.126+0000][0.846s][48122][debug][gc,phases   ] GC(12) Pre Evacuate Collection Set: 0.1ms
[2026-09-28T02:15:30.134+0000][0.854s][48122][debug][gc,phases   ] GC(12) Evacuate Collection Set: 7.8ms
[2026-09-28T02:15:30.135+0000][0.855s][48122][debug][gc,phases   ] GC(12) Post Evacuate Collection Set: 1.2ms
[2026-09-28T02:15:30.136+0000][0.856s][48122][info ][gc,heap     ] GC(12) Eden regions: 128->0(130)
[2026-09-28T02:15:30.136+0000][0.856s][48122][info ][gc,heap     ] GC(12) Survivor regions: 16->14(18)
[2026-09-28T02:15:30.136+0000][0.856s][48122][info ][gc,heap     ] GC(12) Old regions: 45->47
[2026-09-28T02:15:30.136+0000][0.856s][48122][info ][gc          ] GC(12) Pause Young (Normal) (G1 Evacuation Pause) 304M->122M(2048M) 11.234ms
[2026-09-28T02:15:30.136+0000][0.856s][48122][info ][gc,cpu      ] GC(12) User=0.03s Sys=0.00s Real=0.01s
```
**Reading the Summary Line:**
- `304M->122M(2048M)`: Heap usage dropped from 304MB to 122MB out of a 2048MB total capacity.
- `11.234ms`: The total Stop-The-World pause duration for this cycle was 11.2ms.

---

## 8. Essential Production Tuning Knobs & Best Practices

### 1. Always Set Initial Heap Equal to Max Heap (`-Xms` == `-Xmx`)
```bash
-Xms4g -Xmx4g
```
- **Why:** If `-Xms` is smaller than `-Xmx`, the JVM starts small and dynamically requests virtual memory resizing from the OS as load rises. Heap expansion triggers costly Full GC cycles and OS page allocation pauses. Setting `-Xms == -Xmx` pre-commits all heap memory upfront.

---

### 2. G1 GC Key Tuning Knobs

| Flag | Default | Description & Recommended Value |
|---|---|---|
| `-XX:MaxGCPauseMillis=200` | `200` | Target maximum pause time in milliseconds. For responsive REST APIs, tune between `100` and `150`. Setting it too low ($< 50\text{ms}$) forces tiny collections and reduces throughput. |
| `-XX:InitiatingHeapOccupancyPercent=45` | `45` | Heap occupancy percentage threshold that triggers a concurrent marking cycle (IHOP). If experiencing Full GCs under high load, lower this to `35`–`40` to start marking earlier. |
| `-XX:G1ReservePercent=10` | `10` | Percentage of false-ceiling spare memory kept as a buffer against promotion/evacuation failures. Increase to `15` if you see "to-space exhausted" warnings. |
| `-XX:G1HeapRegionSize=n` | Auto (1M–32M) | Explicit region size. Must be a power of 2 (1, 2, 4, 8, 16, 32MB). If logs show frequent Humongous allocations, increase region size (e.g., `-XX:G1HeapRegionSize=16m`). |

---

### 3. ZGC Key Tuning Knobs

```bash
# Java 21+ Generational ZGC
-XX:+UseZGC -XX:+ZGenerational -Xms16g -Xmx16g
```
- ZGC is largely self-tuning ("ergonomic"). Unlike G1, there is **no** `-XX:MaxGCPauseMillis` flag because ZGC pauses are already guaranteed $< 1\text{ms}$.
- Tuning knob: `-XX:ConcGCThreads=n`: Number of concurrent GC worker threads. Default is roughly $12.5\%$ of available CPU cores. Increase if the allocation rate exceeds the collection rate.

---

### 4. How to Choose the Right Collector: Production Decision Guide

```
                               What is your primary requirement?
                                                │
                 ┌──────────────────────────────┴──────────────────────────────┐
                 ▼                                                             ▼
        Max Throughput / Batch Jobs                                   Interactive / Low-Latency
        (No user waiting on requests)                                (REST APIs, Microservices, UI)
                 │                                                             │
                 ▼                                                             ▼
       Use: PARALLEL GC                                               What is your heap size?
     (-XX:+UseParallelGC)                                                      │
                                                ┌──────────────────────────────┴──────────────────────────────┐
                                                ▼                                                             ▼
                                          < 4GB to 32GB                                                    > 32GB+
                                                │                                                             │
                                                ▼                                                             ▼
                                     Is p99 latency SLA < 10ms?                                    Is p99 latency SLA < 5ms?
                                      ┌─────────┴─────────┐                                         ┌─────────┴─────────┐
                                      ▼                   ▼                                         ▼                   ▼
                                     NO                  YES                                       NO                  YES
                                      │                   │                                         │                   │
                                      ▼                   ▼                                         ▼                   ▼
                                 Use: G1 GC          Use: ZGC /                                Use: G1 GC          Use: ZGC
                               (-XX:+UseG1GC)        Shenandoah                              (-XX:+UseG1GC)     (-XX:+UseZGC)
```

---

## 9. Summary Cheatsheet

1. **What GC is:** The JVM mechanism that automatically scans heap memory, identifies unreferenced objects using GC Roots reachability, and frees their memory.
2. **When it runs:** Non-deterministic. Triggered by allocation failures in Eden (Minor GC), Old Gen / Metaspace thresholds (Major/Full GC), or heuristic occupancy thresholds (Concurrent Marking).
3. **Core algorithms:** Mark-Sweep (fragmentation), Mark-Sweep-Compact (compacted, higher STW), and Mark-Copy (fast, used in Eden/Survivors).
4. **Current default:** **G1 GC** (Java 9 through Java 21+). In Java 8, it was **Parallel GC**.
5. **How to check:**
   - In terminal before launch: `java -XX:+PrintCommandLineFlags -version`
   - In running process: `jcmd <PID> VM.flags` or `jstat -gc <PID> 1000`
   - In Java code: `ManagementFactory.getGarbageCollectorMXBeans()`
6. **Can we change it:** **Yes**, at JVM launch time using flags like `-XX:+UseG1GC`, `-XX:+UseZGC`, `-XX:+UseParallelGC`, or `-XX:+UseSerialGC`. It cannot be changed dynamically on a live process without restart.
7. **Production Best Practice:** Pair `-XX:+UseG1GC` (or `-XX:+UseZGC -XX:+ZGenerational` in JDK 21) with `-Xms == -Xmx`, `-XX:+DisableExplicitGC`, and `-Xlog:gc*` rotation logging.
