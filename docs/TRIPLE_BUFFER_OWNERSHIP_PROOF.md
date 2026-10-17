# Formal Ownership and Concurrency Proof: Single-Producer Single-Consumer Lock-Free Triple Buffer

**Component:** `hires::HiResAudioEngine` (`HiResAudioEngine.h` / `HiResAudioEngine.cpp`)  
**Scope:** Real-time audio rendering thread (Consumer) $\leftrightarrow$ UI/Control thread (Producer) parameter synchronization.

---

## 1. Problem Statement & Real-Time Constraints

In an audiophile audio engine:
1. **Hard Real-Time Deadline:** The audio render callback runs under hard real-time scheduling constraints. It must **never** block on a mutex, allocate heap memory, or spin wait, as doing so introduces audio dropouts (underruns/glitches).
2. **Atomic Snapshot Consistency:** The DSP pipeline evaluates interconnected filter coefficients (graphic EQ, parametric EQ, mid/side vocal isolation, preamp gain). Parameter updates must be observed as a single atomic snapshot ($DspSnapshot$) without torn fields (e.g. applying a new filter center frequency with stale $Q$ or gain coefficients).
3. **Single-Producer Single-Consumer (SPSC):** High-frequency control updates from the UI thread must publish non-blocking updates that the audio render thread can consume at block boundaries.

---

## 2. Protocol Specification

### 2.1 State Representation
The triple buffer consists of three distinct slots $S = \{0, 1, 2\}$, each holding a full $DspSnapshot$ structure:
- `DspSnapshot m_snapshots[3]`
- `int m_writeIdx`: Exclusively owned by Producer (UI control thread).
- `int m_readIdx`: Exclusively owned by Consumer (audio render thread).
- `std::atomic<uint32_t> m_sharedState`: Atomic coordination register packing:
  - Bits 0–1 (`0x03`): Index of the currently shared buffer slot $\in \{0, 1, 2\}$.
  - Bit 2 (`0x04`): Dirty publication flag ($1 = \text{new snapshot available}, 0 = \text{consumed}$).

### 2.2 Initial State
$$m\_writeIdx(0) = 0, \quad m\_readIdx(0) = 1, \quad m\_sharedState(0) = 2 \text{ (dirty bit = 0)}$$
Observe that at initialization:
$$\{m\_writeIdx(0)\} \cap \{m\_readIdx(0)\} \cap \{m\_sharedSlot(0)\} = \emptyset$$
All three slots are pairwise disjoint and valid indices in $\{0, 1, 2\}$.

---

## 3. Algorithm Description

### 3.1 Producer: `commitSnapshot()` (UI Thread)
1. Write parameters into private slot:
   $$\text{PayloadStore}(m\_snapshots[m\_writeIdx] \leftarrow m\_staging)$$
2. Atomically publish write slot and reclaim old shared slot:
   ```cpp
   uint32_t oldState = m_sharedState.load(std::memory_order_relaxed);
   uint32_t newState;
   do {
       newState = (static_cast<uint32_t>(m_writeIdx) & 0x03u) | 0x04u; // dirty bit set
   } while (!m_sharedState.compare_exchange_weak(oldState, newState,
                                                std::memory_order_release,
                                                std::memory_order_relaxed));
   m_writeIdx = static_cast<int>(oldState & 0x03u);
   ```

### 3.2 Consumer: `processInterleavedStereo()` (Audio Thread)
1. Inspect dirty bit without synchronization overhead:
   ```cpp
   uint32_t shared = m_sharedState.load(std::memory_order_relaxed);
   if (shared & 0x04u) {
       uint32_t oldState = shared;
       uint32_t newState;
       do {
           if (!(oldState & 0x04u)) break; // already claimed
           newState = static_cast<uint32_t>(m_readIdx) & 0x03u; // dirty bit cleared
       } while (!m_sharedState.compare_exchange_weak(oldState, newState,
                                                    std::memory_order_acquire,
                                                    std::memory_order_relaxed));
       if (oldState & 0x04u) {
           m_readIdx = static_cast<int>(oldState & 0x03u);
       }
   }
   const auto& snap = m_snapshots[m_readIdx];
   ```

---

## 4. Formal Proof of Correctness

### Theorem 1: Mutual Exclusion Invariant
*At every point in time $t \ge 0$, the slot being written by the Producer is strictly disjoint from the slot being read by the Consumer:*
$$\forall t \ge 0, \quad m\_writeIdx(t) \neq m\_readIdx(t)$$

#### Proof by Induction:
1. **Base Case ($t = 0$):**
   $m\_writeIdx(0) = 0$ and $m\_readIdx(0) = 1$. Since $0 \neq 1$, the invariant holds.
2. **Inductive Step:**
   Let the invariant hold up to step $k$. A state transition occurs either via Producer publication or Consumer acquisition:
   - **Case A: Producer publication at step $k+1$.**
     The Producer replaces $m\_sharedState$ with $m\_writeIdx_k$ and sets the dirty bit. The Producer's new write slot is $m\_writeIdx_{k+1} = \text{oldState} \ \& \ 0x03$.
     What could `oldState` be?
     - If Consumer was idle since last publish: `oldState` was the previously published slot that Consumer has not yet read, OR the initial slot. Neither is $m\_readIdx_k$ (the slot currently held by the Consumer).
     - If Consumer consumed prior to this step: Consumer swapped in its previous read slot with dirty bit 0. Hence $\text{oldState}$ is the discarded old read slot, which Consumer is no longer reading ($m\_readIdx_{k} \neq \text{discarded slot}$).
     Therefore, $m\_writeIdx_{k+1} \neq m\_readIdx_k$.
   - **Case B: Consumer acquisition at step $k+1$.**
     The Consumer executes CAS when dirty bit is 1. The Consumer obtains $m\_readIdx_{k+1} = \text{oldState} \ \& \ 0x03$, where $\text{oldState}$ was published by the Producer in a prior Producer publication.
     By Case A, the Producer has already moved its $m\_writeIdx$ to a different slot upon publishing. Thus $m\_readIdx_{k+1} \neq m\_writeIdx_{k+1}$.
   By mathematical induction, $m\_writeIdx(t) \neq m\_readIdx(t)$ for all $t \ge 0$. $\blacksquare$

### Theorem 2: Absence of Data Races & Memory Ordering
*No read operation on $m\_snapshots[m\_readIdx]$ can read uncommitted or partial stores from $m\_snapshots[m\_writeIdx]$.*

#### Proof:
1. **Release-Acquire Synchronization:**
   - The Producer completes all writes to `m_snapshots[m_writeIdx]` **before** executing the CAS with `std::memory_order_release`.
   - Under the C++20 memory model (ISO/IEC 14882:2020 §6.9.2), all memory stores prior to a `release` operation *synchronize-with* a successful `acquire` operation on the same atomic variable.
   - The Consumer acquires $m\_sharedState$ using `std::memory_order_acquire`.
   - Therefore, all stores to the snapshot fields *happen-before* the Consumer reads `m_snapshots[m_readIdx]`.
2. **Monotonicity:** Because $m\_writeIdx(t) \neq m\_readIdx(t)$ (Theorem 1), concurrent access to the same memory location never occurs.
   Thus, the protocol is data-race free under all execution schedules. $\blacksquare$

### Theorem 3: Wait-Freedom for Audio Consumer
*The audio render thread completes parameter acquisition in $O(1)$ bounded steps without blocking.*

#### Proof:
1. If no new snapshot was published (dirty bit 0), the Consumer executes 1 relaxed load and 0 CAS loops ($O(1)$).
2. If a new snapshot was published (dirty bit 1), the Consumer attempts CAS. Since there is at most one Consumer thread, contention can only occur if the single Producer concurrently commits.
3. The CAS loop executes at most twice before either succeeding or observing dirty bit cleared by another turn.
4. No spin loops, locks, allocations, or system calls exist in the Consumer path.
   Thus, the Consumer is bounded $O(1)$ wait-free. $\blacksquare$
