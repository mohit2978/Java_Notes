# Java `BitSet` — Complete Guide

## What is `BitSet`?

`BitSet` is a class in Java that represents a **sequence of bits** (0s and 1s).  
Internally, it stores bits packed into **64-bit `long` arrays**, making it extremely **memory-efficient** and **fast** for bitwise operations.

```
Bit index:   0  1  2  3  4  5  6  7  8  ...
Bit value:   1  0  1  1  0  0  1  0  1  ...
```

> Instead of using a `boolean[]` (1 byte per element), `BitSet` uses **1 bit per element**.  
> 64 bits are packed into a single `long` → **64x memory savings** over `boolean[]`.

---

## Where is `BitSet` in Java Collections?

```
java.lang
    └── Object
            └── BitSet   ✅ (implements Cloneable, Serializable)
```

> ⚠️ `BitSet` is **NOT** part of the `java.util.Collection` framework (not a `List`, `Set`, or `Map`).  
> It lives in **`java.lang`** package and is a standalone utility class.  
> It does NOT implement `Iterable` — you iterate manually using index-based methods.

```java
import java.util.BitSet; // Note: actually in java.util, not java.lang
```

> Actually: `BitSet` is in **`java.util`** package.

---

## Internal Architecture 🔧

### How Bits are Stored

```java
// Internally, BitSet holds an array of longs:
private long[] words;
```

Each `long` holds **64 bits**. Bit at index `i` is stored in:

```
word index  = i / 64         (i >> 6)
bit position = i % 64        (i & 63)
```

So `BitSet` with 128 bits uses exactly **2 longs** = 16 bytes.  
A `boolean[]` with 128 elements uses **128 bytes** → 8x more memory.

---

### ❓ Why `long[]` and NOT a single `long`?

This is a very important question. A `long` in Java is 64 bits — the same width  
BitSet uses per word. So why not just use a **single `long`** with bit operations?

#### The Hard Limit: A single `long` can only hold 64 bits

```java
// Hypothetical — using a plain long
long bits = 0L;

// Set bit 5
bits |= (1L << 5);       // ✅ works

// Set bit 63
bits |= (1L << 63);      // ✅ works (last bit)

// Set bit 64 ← PROBLEM!
bits |= (1L << 64);      // ❌ WRONG! Java shift wraps: (1L << 64) == (1L << 0) == 1L
                         // Silently corrupts data — no exception thrown!
```

> ⚠️ Java's shift operator `<<` on `long` uses only the **lower 6 bits** of the shift amount.  
> So `1L << 64` is treated as `1L << 0` = `1`. You can never set a "bit 64" in a single `long`.  
> **A `long` is capped at 64 bits. Period.**

---

#### Visual: What happens with index > 63

```
Single long approach (BROKEN for n > 64):

Bit index:  0   1   2   ...  63  | 64  65  66  ...  ← IMPOSSIBLE in one long
            └─────────────────────┘   └──────────────
              fits in 1 long (64 bits)     can't fit! overflow silently wraps


long[] approach (CORRECT, unlimited):

words[0]:  bits 0..63
words[1]:  bits 64..127
words[2]:  bits 128..191
   ...
words[n]:  bits n*64 .. n*64+63     ← grows as needed
```

---

#### Side-by-Side Code Comparison

```java
// ─── APPROACH 1: Single long ─────────────────────────────────────
long bits = 0L;
bits |= (1L << 5);   // set bit 5 ✅

// To set bit 100 — IMPOSSIBLE!
// (1L << 100) == (1L << (100 % 64)) == (1L << 36) → WRONG index!


// ─── APPROACH 2: long[] (what BitSet does) ───────────────────────
long[] words = new long[2]; // holds bits 0..127

// Set bit 100:
int wordIndex = 100 >> 6;         // 100 / 64 = 1   → words[1]
int bitPos    = 100 & 63;         // 100 % 64 = 36
words[wordIndex] |= (1L << bitPos); // sets bit 36 of words[1] ✅
                                     // = bit 64+36 = bit 100 globally ✅
```

---

#### Why not use `BigInteger` instead of `long[]`?

| | `long` | `long[]` (BitSet) | `BigInteger` |
|---|---|---|---|
| Max bits | 64 (fixed) | Unlimited (dynamic) | Unlimited (immutable) |
| Mutable? | ✅ Yes | ✅ Yes | ❌ No (every op creates new object) |
| Memory | 8 bytes | n × 8 bytes | heap overhead + byte[] |
| Speed | ⚡ Fastest | ⚡ Very fast | 🐢 Slow (allocation per operation) |
| Growable? | ❌ No | ✅ Auto-resize | ❌ New object always |

> `BigInteger` is **immutable** — every bit flip creates a new object on the heap.  
> For a BitSet doing millions of set/clear operations, this would be catastrophically slow.  
> `long[]` gives you the speed of raw `long` bit ops **plus** unlimited size.

---

#### The Exact Reason — Summarized

```
A single long:
  ✅ 64-bit bit manipulation is very fast
  ❌ Capped at exactly 64 bits
  ❌ Shift wraps silently — no error for index > 63

long[] in BitSet:
  ✅ Same 64-bit speed per word (CPU native)
  ✅ Unlimited capacity — just add more longs
  ✅ Bulk ops (AND/OR/XOR) loop over words[] → still 64x faster than boolean[]
  ✅ Dynamic growth: starts small, expands on demand
```

---

### Dynamic Resizing

`BitSet` grows automatically when you set a bit beyond the current capacity:

```java
BitSet bs = new BitSet(); // starts with 1 long (64 bits)
bs.set(1000);             // auto-expands to hold bit 1000
                          // needs ceil(1001/64) = 16 longs
```

### Bit Manipulation Internals

Setting bit `i`:
```java
words[i >> 6] |= (1L << (i & 63));   // OR with a 1 at position
```

Clearing bit `i`:
```java
words[i >> 6] &= ~(1L << (i & 63));  // AND with a 0 at position
```

Getting bit `i`:
```java
(words[i >> 6] & (1L << (i & 63))) != 0
```

---

## Creating a `BitSet`

```java
import java.util.BitSet;

// Default constructor — starts empty, grows as needed
BitSet bs1 = new BitSet();

// With initial capacity hint (in bits) — still grows dynamically
BitSet bs2 = new BitSet(128);

// From a long array
long[] longs = {0b1011L}; // bits: ...1011
BitSet bs3 = BitSet.valueOf(longs);

// From a byte array
byte[] bytes = {0b00001011}; // bits: 11010000 (LSB first)
BitSet bs4 = BitSet.valueOf(bytes);
```

---

## All `BitSet` Methods — Complete Reference

### 🔵 Setting Bits

```java
BitSet bs = new BitSet(16);

bs.set(3);          // Set bit 3 to 1
bs.set(3, true);    // Equivalent to above
bs.set(3, false);   // Clears bit 3 (same as clear)

bs.set(2, 6);       // Set bits 2, 3, 4, 5 (fromIndex inclusive, toIndex exclusive)
```

### 🔴 Clearing Bits

```java
bs.clear(3);        // Clear bit 3 → set to 0
bs.clear(2, 6);     // Clear bits 2, 3, 4, 5
bs.clear();         // Clear ALL bits → all become 0
```

### 🟡 Flipping Bits

```java
bs.flip(3);         // Toggle bit 3: 0→1 or 1→0
bs.flip(2, 6);      // Toggle bits 2, 3, 4, 5
```

### 🟢 Reading / Querying Bits

```java
boolean val = bs.get(3);          // Returns true if bit 3 is set, false otherwise

BitSet sub = bs.get(2, 6);        // Returns a NEW BitSet with bits [2, 6)
                                  // sub.get(0) == bs.get(2), sub.get(1) == bs.get(3), etc.
```

### 🔍 Finding Set/Clear Bits

```java
int next1 = bs.nextSetBit(0);      // Index of first set bit >= 0; -1 if none
int next0 = bs.nextClearBit(0);    // Index of first clear bit >= 0
int prev1 = bs.previousSetBit(10); // Index of last set bit <= 10; -1 if none
int prev0 = bs.previousClearBit(10); // Index of last clear bit <= 10
```

**Example — iterating all set bits:**
```java
for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
    System.out.println("Bit set at: " + i);
}
```

### 📐 Size and Cardinality

```java
int size  = bs.size();        // Total number of bits in the backing long[] (multiple of 64)
int len   = bs.length();      // Index of the HIGHEST set bit + 1 (logical length)
int count = bs.cardinality(); // Number of bits that are SET (= 1)
boolean empty = bs.isEmpty(); // true if no bits are set
```

**Difference: `size()` vs `length()`**
```java
BitSet bs = new BitSet(64);
bs.set(5);
System.out.println(bs.size());       // 64  (allocated storage)
System.out.println(bs.length());     // 6   (highest set bit index + 1)
System.out.println(bs.cardinality()); // 1  (only one bit is set)
```

---

## Bitwise Operations Between Two `BitSet`s

These operations **modify the calling BitSet** in-place.

### AND
```java
BitSet a = BitSet.valueOf(new long[]{0b1100});
BitSet b = BitSet.valueOf(new long[]{0b1010});
a.and(b);
// a is now 0b1000  (bits set in BOTH a and b)
```

### OR
```java
a.or(b);
// a is now 0b1110  (bits set in EITHER a or b)
```

### XOR
```java
a.xor(b);
// a is now 0b0110  (bits set in one but NOT both)
```

### AND NOT (Subtraction)
```java
a.andNot(b);
// a is now 0b0100  (bits set in a but NOT in b)
```

### Non-mutating Intersection Check
```java
boolean overlaps = a.intersects(b); // true if ANY bit is set in BOTH
```

---

## Conversion Methods

```java
BitSet bs = new BitSet();
bs.set(0); bs.set(1); bs.set(3); // bits: 1011

// To long array
long[] longs = bs.toLongArray();   // [11L]  (bit 0,1,3 → 1+2+8=11)

// To byte array
byte[] bytes = bs.toByteArray();   // [11]

// To String (shows set bit indices)
System.out.println(bs.toString()); // "{0, 1, 3}"
```

---

## Cloning and Equality

```java
BitSet original = new BitSet();
original.set(5);

BitSet copy = (BitSet) original.clone(); // deep copy

System.out.println(original.equals(copy)); // true
System.out.println(original.hashCode());   // consistent with equals
```

---

## Full Working Example

```java
import java.util.BitSet;

public class BitSetDemo {
    public static void main(String[] args) {
        BitSet primes = new BitSet(50);

        // Sieve of Eratosthenes using BitSet
        primes.set(2, 50); // assume all 2..49 are prime

        for (int i = 2; i * i < 50; i++) {
            if (primes.get(i)) {
                // mark multiples as not prime
                for (int j = i * i; j < 50; j += i) {
                    primes.clear(j);
                }
            }
        }

        System.out.print("Primes up to 50: ");
        for (int i = primes.nextSetBit(0); i >= 0; i = primes.nextSetBit(i + 1)) {
            System.out.print(i + " ");
        }
        // Output: 2 3 5 7 11 13 17 19 23 29 31 37 41 43 47
    }
}
```

---

## Why is `BitSet` Useful?

| Use Case | Why BitSet |
|---|---|
| **Sieve of Eratosthenes** | Mark composites with 1 bit each — 8x less memory than `boolean[]` |
| **Presence / Visited tracking** | Mark visited nodes in graphs (BFS/DFS) |
| **Set operations** | Intersection (`and`), union (`or`), difference (`andNot`) in O(n/64) |
| **Bloom filters** | Core data structure for probabilistic membership |
| **Compression flags** | Store 64 boolean flags in a single `long` |
| **Character presence** | Track which characters exist in a string |
| **Network protocols** | Compact bit-flag fields |

### Performance: `BitSet` vs `boolean[]` vs `HashSet<Integer>`

| Operation | `boolean[]` | `BitSet` | `HashSet<Integer>` |
|---|---|---|---|
| Memory (n=1M) | ~1 MB | ~125 KB | ~32 MB |
| `set(i)` | O(1) | O(1) | O(1) amortized |
| `get(i)` | O(1) | O(1) | O(1) amortized |
| Bulk AND/OR/XOR | O(n) slow | O(n/64) fast | O(n) slow |
| Iteration (set bits) | O(n) | O(set bits) | O(set bits) |

> `BitSet` AND/OR/XOR are ~**64x faster** than equivalent `boolean[]` loops because  
> they operate on 64 bits simultaneously via CPU bitwise instructions.

---

## Common Patterns

### Check if a Character is in a String
```java
BitSet seen = new BitSet(128);
for (char c : "hello world".toCharArray()) {
    seen.set(c);
}
System.out.println(seen.get('h')); // true
System.out.println(seen.get('z')); // false
```

### Find First Missing Positive
```java
int[] arr = {3, 1, 4, 1, 5, 9, 2};
BitSet bs = new BitSet();
for (int n : arr) if (n > 0) bs.set(n);
System.out.println(bs.nextClearBit(1)); // first missing positive = 6
```

### Count Unique Elements
```java
int[] arr = {1, 2, 2, 3, 3, 3, 4};
BitSet bs = new BitSet();
for (int n : arr) bs.set(n);
System.out.println(bs.cardinality()); // 4 unique elements
```

---

## Limitations of `BitSet`

| Limitation | Detail |
|---|---|
| Indices only | Can only store non-negative integers (indices), not arbitrary objects |
| No generics | Not a generic collection — always works with bit indices |
| Not thread-safe | Must synchronize externally for concurrent access |
| Not in Collections API | Cannot use with `Collections.sort()`, streams, etc. directly |
| No negative indices | `set(-1)` throws `IndexOutOfBoundsException` |

---

## Quick Reference Card

```
Creation:      new BitSet()  /  new BitSet(n)  /  BitSet.valueOf(long[])
Set bit:       bs.set(i)     /  bs.set(from, to)
Clear bit:     bs.clear(i)   /  bs.clear(from, to)  /  bs.clear()
Flip bit:      bs.flip(i)    /  bs.flip(from, to)
Read bit:      bs.get(i)  → boolean
Read range:    bs.get(from, to)  → BitSet

Count set:     bs.cardinality()
Any set?:      !bs.isEmpty()
Highest bit:   bs.length() - 1
Storage size:  bs.size()

Next set:      bs.nextSetBit(from)
Next clear:    bs.nextClearBit(from)
Prev set:      bs.previousSetBit(from)
Prev clear:    bs.previousClearBit(from)

AND:           a.and(b)
OR:            a.or(b)
XOR:           a.xor(b)
AND NOT:       a.andNot(b)
Intersects?:   a.intersects(b)

To long[]:     bs.toLongArray()
To byte[]:     bs.toByteArray()
Clone:         (BitSet) bs.clone()
```
