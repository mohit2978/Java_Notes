# Java Deadlocks: Production Verification & Prevention Guide (With Full Examples)

A deadlock occurs when two or more threads are permanently blocked, each waiting for a lock or resource that another thread in the cycle holds.

In production (PRD), deadlocks are particularly dangerous because:
- **No Crash or Exception is Thrown:** The JVM keeps running, CPU usage often drops to near 0% on stuck threads, but requests hang indefinitely.
- **Cascading Exhaustion:** Web container thread pools (Tomcat/Undertow) and database connection pools (HikariCP) saturate, causing widespread HTTP `504 Gateway Timeout` or `503 Service Unavailable` errors.

---

## 1. Concrete Deadlock Reproduction Example

Before looking at diagnosis and detection, here is a complete, runnable Java program that triggers a deterministic deadlock between two threads.

```java
package com.example.deadlock;

public class DeadlockSimulation {
    private static final Object LockA = new Object();
    private static final Object LockB = new Object();

    public static void main(String[] args) {
        // Thread 1: Locks LockA first, then tries to lock LockB
        Thread thread1 = new Thread(() -> {
            synchronized (LockA) {
                System.out.println("[Thread-1] Holding LockA...");
                try {
                    // Small sleep to ensure Thread-2 acquires LockB
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("[Thread-1] Waiting for LockB...");
                synchronized (LockB) {
                    System.out.println("[Thread-1] Acquired LockA and LockB!");
                }
            }
        }, "Worker-Thread-1");

        // Thread 2: Locks LockB first, then tries to lock LockA (Inverted Order)
        Thread thread2 = new Thread(() -> {
            synchronized (LockB) {
                System.out.println("[Thread-2] Holding LockB...");
                try {
                    // Small sleep to ensure Thread-1 acquires LockA
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("[Thread-2] Waiting for LockA...");
                synchronized (LockA) {
                    System.out.println("[Thread-2] Acquired LockB and LockA!");
                }
            }
        }, "Worker-Thread-2");

        thread1.start();
        thread2.start();
    }
}
```

### Console Output:
```text
[Thread-1] Holding LockA...
[Thread-2] Holding LockB...
[Thread-1] Waiting for LockB...
[Thread-2] Waiting for LockA...
<APPLICATION HANGS INDEFINITELY WITH 0% CPU>
```

---

## 2. The 4 Coffman Conditions (Theory Behind Deadlocks)

A deadlock can occur **if and only if** all four conditions are met at the same time:

| # | Condition | Concrete Example | Prevention Strategy |
|---|---|---|---|
| **1** | **Mutual Exclusion** | A mutex/synchronized block permits only one thread at a time. | Use read-write locks (`StampedLock`, `ReentrantReadWriteLock`) or atomic CAS structures. |
| **2** | **Hold and Wait** | Thread 1 holds `LockA` while waiting for `LockB`. | Acquire all required resources simultaneously, or release held resources before acquiring new ones. |
| **3** | **No Preemption** | Once a thread holds a lock, it cannot be forcibly revoked. | Use `lock.tryLock(timeout)` so a thread gives up if a lock is unavailable. |
| **4** | **Circular Wait** | Thread 1 waits for Thread 2; Thread 2 waits for Thread 1. | **Enforce a strict, global lock acquisition order** across the entire codebase. |

---

## 3. How to Verify Deadlocks in Production (PRD)

When an alert triggers in PRD (e.g., latency spikes, 504 Gateway Timeouts, thread pool exhaustion):

> [!CAUTION]
> **Do NOT immediately restart the service!**
> A restart clears all in-memory JVM states. The deadlock will recur once traffic resumes, and you will have zero diagnostic data to identify which lock or line of code caused it.
> **First capture 3 thread dumps**, then restart or recycle the instance.

```
[PRD Alert: 504s / Thread Spike / HikariCP Starvation]
                       │
                       ▼
            Do NOT immediately restart!
  (Take diagnostics first, or you lose all root cause data)
                       │
                       ▼
       ┌───────────────────────────────┐
       │ Capture Diagnostics (0-2 min) │
       │ • 3x Thread Dumps (10s apart) │
       │ • DB Engine Status / Locks    │
       │ • APM / JFR snapshot          │
       └──────────────┬────────────────┘
                       │
                       ▼
         Analyze Dumps for Circular Wait
      - Java monitor locks (synchronized)
      - ReentrantLock / java.util.concurrent
      - HikariCP connection pool starvation
                       │
                       ▼
   Remediate (Restart / Rollback / Drain Pod)
```

---

### Step 1: Finding the Java Process ID (PID)

Run either of these commands on the production host or inside the container:

```bash
# Option A: jps (Java Process Status tool)
jps -v

# Output example:
# 48122 DeadlockSimulation -Xms2g -Xmx2g
# 49103 Jps -Dapplication.home=/usr/lib/jvm/java-21

# Option B: Standard Linux ps
ps aux | grep java

# Output example:
# appuser  48122  0.1  8.4 4523120 1378944 ?  Sl   14:20   0:15 /usr/bin/java -jar app.jar
```

Here, the Java process ID is **`48122`**.

---

### Step 2: Capturing Thread Dumps with Examples

Always take **3 dumps at 5- to 10-second intervals**. If a thread is stuck on the exact same lock in all 3 dumps, it is frozen (deadlocked), not just executing a slow operation.

#### Example A: Using `jcmd` (Recommended for Modern JDK 8/11/17/21+)

```bash
# Capture 3 dumps with lock details (-l flag)
jcmd 48122 Thread.print -l > dump_1.tdump
sleep 5
jcmd 48122 Thread.print -l > dump_2.tdump
sleep 5
jcmd 48122 Thread.print -l > dump_3.tdump
```

#### Example B: Using `jstack`

```bash
jstack -l 48122 > jstack_dump_1.tdump
```

#### Example C: Inside Docker Containers

```bash
# Run jcmd inside a running Docker container
docker exec -it my-java-app jcmd 1 Thread.print -l > docker_dump.txt

# Or send SIGQUIT (kill -3), which writes the thread dump directly to standard error/logs
docker kill --signal=SIGQUIT my-java-app
docker logs --tail 500 my-java-app > docker_logs_with_threaddump.txt
```

#### Example D: Inside Kubernetes (K8s) Pods

```bash
# 1. Identify the pod
kubectl get pods -n production -l app=order-service

# Output:
# order-service-67f789d6b5-x8w9q   1/1   Running   0   4h

# 2. Capture thread dump directly from the pod
kubectl exec -n production order-service-67f789d6b5-x8w9q -- jcmd 1 Thread.print -l > k8s_thread_dump.txt

# 3. Alternatively, trigger kill -3 (SIGQUIT) to dump to pod stdout:
kubectl exec -n production order-service-67f789d6b5-x8w9q -- kill -3 1
kubectl logs -n production order-service-67f789d6b5-x8w9q --tail=1000 | grep -A 60 "Found one Java-level deadlock"
```

#### Example E: Using Spring Boot Actuator

If the Spring Boot Actuator endpoint is exposed:

```bash
# Request JSON thread dump from actuator
curl -X GET "http://localhost:8081/actuator/threaddump" \
  -H "Accept: application/json" > actuator_threaddump.json
```

---

### Step 3: Analyzing the Thread Dump (Exact HotSpot JVM Output)

When you open `dump_1.tdump`, scroll directly to the bottom. The HotSpot JVM automatically runs cycle-detection algorithms and prints a dedicated deadlock block:

```text
2026-09-25 14:22:15
Full thread dump OpenJDK 64-Bit Server VM (21.0.2+13-LTS mixed mode, sharing):

Threads class SMR info:
_java_thread_list=0x00007f9c340026a0, length=14, elements={
0x00007f9c980287a0, 0x00007f9c982390c0, ...
}

"Worker-Thread-1" #22 prio=5 os_prio=0 cpu=1.24ms elapsed=45.12s tid=0x00007f9c982390c0 nid=0xbc42 waiting for monitor entry [0x00007f9c7e7fe000]
   java.lang.Thread.State: BLOCKED (on object monitor)
        at com.example.deadlock.DeadlockSimulation.lambda$main$0(DeadlockSimulation.java:20)
        - waiting to lock <0x00000007159c3a30> (a java.lang.Object)
        - locked <0x00000007159c3a20> (a java.lang.Object)
        at com.example.deadlock.DeadlockSimulation$$Lambda$1/0x0000000801000a00.run(Unknown Source)
        at java.lang.Thread.run(java.base@21.0.2/Thread.java:1583)

   Locked ownable synchronizers:
        - None

"Worker-Thread-2" #23 prio=5 os_prio=0 cpu=1.18ms elapsed=45.11s tid=0x00007f9c9823a8e0 nid=0xbc43 waiting for monitor entry [0x00007f9c7e6fd000]
   java.lang.Thread.State: BLOCKED (on object monitor)
        at com.example.deadlock.DeadlockSimulation.lambda$main$1(DeadlockSimulation.java:37)
        - waiting to lock <0x00000007159c3a20> (a java.lang.Object)
        - locked <0x00000007159c3a30> (a java.lang.Object)
        at com.example.deadlock.DeadlockSimulation$$Lambda$2/0x0000000801000c20.run(Unknown Source)
        at java.lang.Thread.run(java.base@21.0.2/Thread.java:1583)

   Locked ownable synchronizers:
        - None

================================================================================
Found one Java-level deadlock:
=============================
"Worker-Thread-1":
  waiting to lock monitor 0x00007f9c38006200 (object 0x00000007159c3a30, a java.lang.Object),
  which is held by "Worker-Thread-2"

"Worker-Thread-2":
  waiting to lock monitor 0x00007f9c38004e00 (object 0x00000007159c3a20, a java.lang.Object),
  which is held by "Worker-Thread-1"

Java stack information for the threads listed above:
===================================================
"Worker-Thread-1":
        at com.example.deadlock.DeadlockSimulation.lambda$main$0(DeadlockSimulation.java:20)
        - waiting to lock <0x00000007159c3a30> (a java.lang.Object)
        - locked <0x00000007159c3a20> (a java.lang.Object)
"Worker-Thread-2":
        at com.example.deadlock.DeadlockSimulation.lambda$main$1(DeadlockSimulation.java:37)
        - waiting to lock <0x00000007159c3a20> (a java.lang.Object)
        - locked <0x00000007159c3a30> (a java.lang.Object)

Found 1 deadlock.
```

#### What This Output Tells You:
1. **Thread Names:** `Worker-Thread-1` and `Worker-Thread-2`.
2. **State:** Both threads are in `BLOCKED (on object monitor)`.
3. **Circular Lock Dependency:**
   - `Worker-Thread-1` holds `<0x00000007159c3a20>` and wants `<0x00000007159c3a30>`.
   - `Worker-Thread-2` holds `<0x00000007159c3a30>` and wants `<0x00000007159c3a20>`.
4. **Source Code Location:** Exact line numbers (`DeadlockSimulation.java:20` and `DeadlockSimulation.java:37`).

---

### Step 4: Java Flight Recorder (JFR) Profiling Example

When deadlocks or severe lock contentions are intermittent and hard to catch with manual thread dumps, use Java Flight Recorder (JFR).

```bash
# 1. Start a 60-second profiling recording with high lock-contention sensitivity
jcmd 48122 JFR.start name=DeadlockInspection settings=profile duration=60s filename=/tmp/prod_deadlock.jfr

# 2. Or dump immediately if the incident is actively occurring
jcmd 48122 JFR.dump name=DeadlockInspection filename=/tmp/prod_deadlock_dump.jfr

# 3. Print monitor block events from the JFR file via command line
jfr print --events jdk.JavaMonitorEnter /tmp/prod_deadlock.jfr | grep -B 2 -A 10 "duration ="
```

#### Example JFR Event Output:
```text
jdk.JavaMonitorEnter {
  startTime = 14:22:15.102
  duration = 59.8 s
  monitorClass = java.lang.Object (classLoader = bootstrap)
  address = 0x7159c3a30
  previousOwner = "Worker-Thread-2" (javaThreadId = 23)
  eventThread = "Worker-Thread-1" (javaThreadId = 22)
  stackTrace = [
    com.example.deadlock.DeadlockSimulation.lambda$main$0() line: 20
    java.lang.Thread.run() line: 1583
  ]
}
```
Notice `duration = 59.8 s` on `jdk.JavaMonitorEnter`. A thread waiting 60+ seconds to enter a synchronized monitor is a definitive proof of deadlock.

---

### Step 5: Programmatic Deadlock Detection (Spring Boot Health Indicator)

You can automatically detect deadlocks and alert your team via Slack, PagerDuty, or Kubernetes liveness probes using `ThreadMXBean`.

#### Complete Working Implementation:

```java
package com.example.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DeadlockHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(DeadlockHealthIndicator.class);
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    @Override
    public Health health() {
        // findDeadlockedThreads() finds both synchronized monitors and ReentrantLock / java.util.concurrent locks
        long[] deadlockedIds = threadMXBean.findDeadlockedThreads();

        if (deadlockedIds == null || deadlockedIds.length == 0) {
            return Health.up().withDetail("deadlocks", "None detected").build();
        }

        ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(deadlockedIds, true, true);
        List<Map<String, Object>> deadlockDetails = new ArrayList<>();

        for (ThreadInfo info : threadInfos) {
            Map<String, Object> detail = new HashMap<>();
            detail.put("threadId", info.getThreadId());
            detail.put("threadName", info.getThreadName());
            detail.put("threadState", info.getThreadState().toString());
            detail.put("lockName", info.getLockName());
            detail.put("lockOwnerName", info.getLockOwnerName());
            detail.put("lockOwnerId", info.getLockOwnerId());
            deadlockDetails.add(detail);
        }

        // Return DOWN status -> Kubernetes will fail liveness/readiness probe
        return Health.down()
                .withDetail("errorMessage", "Deadlock detected in JVM!")
                .withDetail("deadlockedThreadCount", deadlockedIds.length)
                .withDetail("threads", deadlockDetails)
                .build();
    }

    // Optional: Background check running every 60 seconds that sends immediate Slack/PagerDuty alert
    @Scheduled(fixedRate = 60000)
    public void scheduledDeadlockCheck() {
        long[] deadlockedIds = threadMXBean.findDeadlockedThreads();
        if (deadlockedIds != null && deadlockedIds.length > 0) {
            log.error("CRITICAL ALERT: Detected {} deadlocked threads in JVM!", deadlockedIds.length);
            ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(deadlockedIds, true, true);
            for (ThreadInfo ti : threadInfos) {
                log.error("Deadlocked Thread: '{}' [ID: {}], Blocked on Lock: '{}' owned by '{}' [ID: {}]",
                        ti.getThreadName(), ti.getThreadId(), ti.getLockName(), ti.getLockOwnerName(), ti.getLockOwnerId());
            }
            // Trigger alert notification (PagerDuty / Slack webhook) here
        }
    }
}
```

#### Example Actuator Response When Deadlock Occurs (`/actuator/health`):
```json
{
  "status": "DOWN",
  "components": {
    "deadlock": {
      "status": "DOWN",
      "details": {
        "errorMessage": "Deadlock detected in JVM!",
        "deadlockedThreadCount": 2,
        "threads": [
          {
            "threadId": 22,
            "threadName": "Worker-Thread-1",
            "threadState": "BLOCKED",
            "lockName": "java.lang.Object@7159c3a30",
            "lockOwnerName": "Worker-Thread-2",
            "lockOwnerId": 23
          },
          {
            "threadId": 23,
            "threadName": "Worker-Thread-2",
            "threadState": "BLOCKED",
            "lockName": "java.lang.Object@7159c3a20",
            "lockOwnerName": "Worker-Thread-1",
            "lockOwnerId": 22
          }
        ]
      }
    }
  }
}
```

---

### Step 6: Database-Level Deadlocks in Production

In database-driven Java backends, deadlocks often happen in the RDBMS rather than in Java memory.

#### A. PostgreSQL Deadlock Example

##### Reproduction:
```sql
-- Transaction 1 (Thread A)
BEGIN;
UPDATE accounts SET balance = balance - 100 WHERE id = 1; -- Holds row lock on id=1
-- Then Thread A attempts:
UPDATE accounts SET balance = balance + 100 WHERE id = 2; -- Blocked waiting for id=2

-- Transaction 2 (Thread B, concurrently)
BEGIN;
UPDATE accounts SET balance = balance - 50 WHERE id = 2;  -- Holds row lock on id=2
-- Then Thread B attempts:
UPDATE accounts SET balance = balance + 50 WHERE id = 1;  -- Blocked waiting for id=1
```

##### PostgreSQL Log Output:
```text
2026-09-25 14:30:00.123 UTC [14820] ERROR:  deadlock detected
2026-09-25 14:30:00.123 UTC [14820] DETAIL:  Process 14820 waits for ShareLock on transaction 88921; blocked by process 14821.
Process 14821 waits for ShareLock on transaction 88920; blocked by process 14820.
Process 14820: UPDATE accounts SET balance = balance + 100 WHERE id = 2;
Process 14821: UPDATE accounts SET balance = balance + 50 WHERE id = 1;
2026-09-25 14:30:00.123 UTC [14820] HINT:  See server log for query details.
2026-09-25 14:30:00.123 UTC [14820] STATEMENT:  UPDATE accounts SET balance = balance + 100 WHERE id = 2;
```

##### Checking Active Locks in PostgreSQL via SQL:
```sql
SELECT
    blocked_locks.pid     AS blocked_pid,
    blocked_activity.usename  AS blocked_user,
    blocking_locks.pid    AS blocking_pid,
    blocking_activity.usename AS blocking_user,
    blocked_activity.query    AS blocked_statement,
    blocking_activity.query   AS blocking_statement
FROM  pg_catalog.pg_locks         blocked_locks
JOIN pg_catalog.pg_stat_activity blocked_activity ON blocked_activity.pid = blocked_locks.pid
JOIN pg_catalog.pg_locks         blocking_locks 
    ON blocking_locks.locktype = blocked_locks.locktype
    AND blocking_locks.database IS NOT DISTINCT FROM blocked_locks.database
    AND blocking_locks.relation IS NOT DISTINCT FROM blocked_locks.relation
    AND blocking_locks.page IS NOT DISTINCT FROM blocked_locks.page
    AND blocking_locks.tuple IS NOT DISTINCT FROM blocked_locks.tuple
    AND blocking_locks.virtualxid IS NOT DISTINCT FROM blocked_locks.virtualxid
    AND blocking_locks.transactionid IS NOT DISTINCT FROM blocked_locks.transactionid
    AND blocking_locks.classid IS NOT DISTINCT FROM blocked_locks.classid
    AND blocking_locks.objid IS NOT DISTINCT FROM blocked_locks.objid
    AND blocking_locks.objsubid IS NOT DISTINCT FROM blocked_locks.objsubid
    AND blocking_locks.pid != blocked_locks.pid
JOIN pg_catalog.pg_stat_activity blocking_activity ON blocking_activity.pid = blocking_locks.pid
WHERE NOT blocked_locks.granted;
```

---

#### B. MySQL / InnoDB Deadlock Verification Example

Run this query directly in MySQL:

```sql
SHOW ENGINE INNODB STATUS\G
```

Look for the **`LATEST DETECTED DEADLOCK`** section in the output:

```text
------------------------
LATEST DETECTED DEADLOCK
------------------------
2026-09-25 14:35:10 0x7f8d380c2700
*** (1) TRANSACTION:
TRANSACTION 45012, ACTIVE 2 sec starting index read
mysql tables in use 1, locked 1
LOCK WAIT 3 lock struct(s), heap size 1136, 2 row lock(s)
MySQL thread id 142, OS thread handle 140244463478528, query id 2841 10.0.0.1 dbuser updating
UPDATE accounts SET balance = balance + 100 WHERE id = 2
*** (1) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 58 page no 3 n bits 72 index PRIMARY of table `ecommerce`.`accounts` trx id 45012 lock_mode X locks rec but not gap waiting
Record lock, heap no 3 PHYSICAL RECORD: n_fields 4; compact format; info bits 0
 0: len 8; hex 8000000000000002; asc ;;

*** (2) TRANSACTION:
TRANSACTION 45013, ACTIVE 2 sec starting index read
mysql tables in use 1, locked 1
4 lock struct(s), heap size 1136, 3 row lock(s)
MySQL thread id 143, OS thread handle 140244438300416, query id 2842 10.0.0.2 dbuser updating
UPDATE accounts SET balance = balance + 50 WHERE id = 1
*** (2) HOLDS THE LOCK(S):
RECORD LOCKS space id 58 page no 3 n bits 72 index PRIMARY of table `ecommerce`.`accounts` trx id 45013 lock_mode X locks rec but not gap
*** (2) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 58 page no 3 n bits 72 index PRIMARY of table `ecommerce`.`accounts` trx id 45013 lock_mode X locks rec but not gap waiting

*** WE ROLL BACK TRANSACTION (1)
```

Notice MySQL rolls back Transaction 1 automatically and throws SQL error code `1213: Deadlock found when trying to get lock; try restarting transaction`.

---

### Step 7: HikariCP Connection Pool Starvation Deadlock Example

A connection pool deadlock occurs when threads consume all available connections and then attempt to acquire another connection within the same flow (e.g., using `Propagation.REQUIRES_NEW`).

#### Reproducing Code:
```java
@Service
public class OrderProcessingService {

    @Autowired
    private AuditLogService auditLogService;

    // Default pool size = 5
    // 5 concurrent requests hit this method simultaneously
    @Transactional
    public void processOrder(Long orderId) {
        // Step 1: Thread acquires Connection-1 from HikariCP
        System.out.println("Processing order: " + orderId);

        // Step 2: Calls audit log method which requires a BRAND NEW connection!
        // All 5 connections are held by step 1 across 5 threads.
        // Step 2 blocks waiting for a free connection from the pool.
        // Result: Thread pool completely deadlocked waiting on HikariCP!
        auditLogService.logAuditRecord(orderId, "ORDER_PROCESSED");
    }
}

@Service
public class AuditLogService {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAuditRecord(Long orderId, String action) {
        // Needs a new connection from HikariCP
        System.out.println("Audit recorded for: " + orderId);
    }
}
```

#### How it Appears in a Java Thread Dump:
Every worker thread is frozen in `LockSupport.parkNanos` waiting for HikariCP:

```text
"http-nio-8080-exec-1" #35 daemon prio=5 os_prio=0 cpu=12.4ms elapsed=62.1s tid=0x00007f9c78210000 nid=0x1a2b waiting on condition [0x00007f9c522f9000]
   java.lang.Thread.State: TIMED_WAITING (parking)
        at jdk.internal.misc.Unsafe.park(java.base@21.0.2/Native Method)
        - parking to wait for  <0x0000000715cb9820> (a java.util.concurrent.SynchronousQueue$TransferStack)
        at java.util.concurrent.locks.LockSupport.parkNanos(java.base@21.0.2/LockSupport.java:269)
        at com.zaxxer.hikari.util.ConcurrentBag.borrow(ConcurrentBag.java:144)
        at com.zaxxer.hikari.pool.HikariPool.getConnection(HikariPool.java:181)
        at com.zaxxer.hikari.pool.HikariPool.getConnection(HikariPool.java:146)
        at com.zaxxer.hikari.HikariDataSource.getConnection(HikariDataSource.java:100)
        at org.springframework.jdbc.datasource.DataSourceTransactionManager.doBegin(DataSourceTransactionManager.java:265)
        at org.springframework.transaction.support.AbstractPlatformTransactionManager.startTransaction(AbstractPlatformTransactionManager.java:400)
        at org.springframework.transaction.support.AbstractPlatformTransactionManager.handleExistingTransaction(AbstractPlatformTransactionManager.java:476)
        at com.example.service.AuditLogService.logAuditRecord(AuditLogService.java:12)
        at com.example.service.OrderProcessingService.processOrder(OrderProcessingService.java:18)
```

---

## 4. How to Prevent Deadlocks in Java Backends (With Concrete Solutions)

---

### Solution 1: Enforce Strict Global Lock Ordering (Breaks Circular Wait)

Always acquire locks in a consistent, globally deterministic order (e.g., sorting by ID or using `System.identityHashCode`).

#### Bad (Causes Deadlock):
```java
public void transferMoney(Account from, Account to, BigDecimal amount) {
    synchronized (from) {
        synchronized (to) {
            from.debit(amount);
            to.credit(amount);
        }
    }
}
```

#### Good (Order by ID):
```java
package com.example.service;

import java.math.BigDecimal;

public class SafeAccountService {

    public void transferMoney(Account from, Account to, BigDecimal amount) {
        if (from.getId() == to.getId()) {
            throw new IllegalArgumentException("Cannot transfer money to the same account");
        }

        // Deterministic ordering: always lock the smaller ID first
        Account firstLock = from.getId() < to.getId() ? from : to;
        Account secondLock = from.getId() < to.getId() ? to : from;

        synchronized (firstLock) {
            synchronized (secondLock) {
                from.debit(amount);
                to.credit(amount);
            }
        }
    }
}
```

#### What If Objects Don't Have an ID? Use `System.identityHashCode`:
```java
public void safeLockBoth(Object lock1, Object lock2, Runnable task) {
    int hash1 = System.identityHashCode(lock1);
    int hash2 = System.identityHashCode(lock2);

    if (hash1 < hash2) {
        synchronized (lock1) {
            synchronized (lock2) {
                task.run();
            }
        }
    } else if (hash1 > hash2) {
        synchronized (lock2) {
            synchronized (lock1) {
                task.run();
            }
        }
    } else {
        // Hash collision tie-breaker
        Object tieLock = new Object();
        synchronized (tieLock) {
            synchronized (lock1) {
                synchronized (lock2) {
                    task.run();
                }
            }
        }
    }
}
```

---

### Solution 2: Explicit Timeouts with `tryLock()` (Breaks No Preemption & Hold-and-Wait)

Never block indefinitely with `synchronized`. Use `ReentrantLock.tryLock()` with a timeout. If the second lock cannot be acquired within the timeout window, release the first lock, wait with random jitter, and retry.

#### Complete Production-Ready Example:

```java
package com.example.service;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SafeLockWithTimeout {
    private final Lock lockA = new ReentrantLock();
    private final Lock lockB = new ReentrantLock();

    public boolean executeWithBothLocks(long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;

        while (System.currentTimeMillis() < deadline) {
            // Attempt to acquire Lock A with a small timeout (50ms)
            if (lockA.tryLock(50, TimeUnit.MILLISECONDS)) {
                try {
                    // Attempt to acquire Lock B with a small timeout (50ms)
                    if (lockB.tryLock(50, TimeUnit.MILLISECONDS)) {
                        try {
                            // CRITICAL SECTION: Both locks acquired safely!
                            doWork();
                            return true;
                        } finally {
                            lockB.unlock();
                        }
                    }
                } finally {
                    // Release Lock A so other threads can proceed (avoids hold-and-wait)
                    lockA.unlock();
                }
            }

            // Random jitter sleep (10ms - 30ms) to avoid live-lock contention
            long jitter = ThreadLocalRandom.current().nextLong(10, 30);
            Thread.sleep(jitter);
        }

        // Timed out without acquiring both locks
        return false;
    }

    private void doWork() {
        System.out.println("Safely performing work with both locks held!");
    }
}
```

---

### Solution 3: Spring `@Retryable` with Deadlock Auto-Recovery

When relational databases detect a deadlock, they abort one transaction. The backend must catch this exception and automatically retry with exponential backoff.

#### Maven Dependency:
```xml
<dependency>
    <groupId>org.springframework.retry</groupId>
    <artifactId>spring-retry</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-aspects</artifactId>
</dependency>
```

#### Complete Implementation:
```java
package com.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ResilientPaymentService {

    private static final Logger log = LoggerFactory.getLogger(ResilientPaymentService.class);

    @Retryable(
        retryFor = { 
            CannotAcquireLockException.class, 
            PessimisticLockingFailureException.class 
        },
        maxAttempts = 4,
        backoff = @Backoff(delay = 100, multiplier = 2.0, maxDelay = 1000, random = true)
    )
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void executeAccountTransfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        log.info("Attempting transfer from account {} to account {}", fromAccountId, toAccountId);
        
        // Ensure SQL updates are ordered by ID to minimize deadlock chance in DB
        Long firstId = Math.min(fromAccountId, toAccountId);
        Long secondId = Math.max(fromAccountId, toAccountId);

        // Fetch accounts in ascending ID order with SELECT ... FOR UPDATE
        Account first = accountRepository.findByIdForUpdate(firstId);
        Account second = accountRepository.findByIdForUpdate(secondId);

        if (fromAccountId.equals(first.getId())) {
            first.debit(amount);
            second.credit(amount);
        } else {
            second.debit(amount);
            first.credit(amount);
        }
        
        accountRepository.save(first);
        accountRepository.save(second);
        log.info("Transfer completed successfully!");
    }

    // Fallback if all 4 retry attempts fail
    @Recover
    public void recoverFromDeadlock(CannotAcquireLockException ex, Long fromAccountId, Long toAccountId, BigDecimal amount) {
        log.error("FAILED to execute transfer after 4 attempts due to DB deadlock: {}. Sending to DLQ.", ex.getMessage());
        // Forward message to Dead Letter Queue (DLQ) or alert ops
    }
}
```

---

### Solution 4: Avoid Keeping Slow I/O Inside `@Transactional`

Holding a database lock while performing an external network call increases the lock contention window by 100x–1000x, inviting deadlocks and connection pool starvation.

#### Bad (High Deadlock & Starvation Risk):
```java
@Transactional
public void completeCheckout(Long orderId) {
    Order order = orderRepo.findByIdForUpdate(orderId); // Holds DB lock & connection
    
    // Slow HTTP REST call to external payment gateway (takes 500ms - 2000ms)
    PaymentResponse response = paymentGatewayClient.charge(order.getTotal()); 
    
    order.setPaymentReference(response.getTxId());
    order.setStatus(OrderStatus.PAID);
    orderRepo.save(order);
}
```

#### Good (Isolate External Network Call Outside Transaction):
```java
@Service
public class CheckoutService {

    @Autowired
    private OrderRepository orderRepo;
    @Autowired
    private PaymentGatewayClient paymentGatewayClient;
    @Autowired
    private TransactionalOrderUpdater orderUpdater;

    public void completeCheckout(Long orderId) {
        // Step 1: Read without holding lock
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // Step 2: Make external network call OUTSIDE any DB transaction
        PaymentResponse response = paymentGatewayClient.charge(order.getTotal());

        // Step 3: Fast, short DB update transaction (holds lock for < 5 milliseconds)
        orderUpdater.updateOrderSuccess(orderId, response.getTxId());
    }
}

@Service
class TransactionalOrderUpdater {

    @Transactional
    public void updateOrderSuccess(Long orderId, String paymentTxId) {
        Order order = orderRepo.findByIdForUpdate(orderId);
        order.setPaymentReference(paymentTxId);
        order.setStatus(OrderStatus.PAID);
        orderRepo.save(order);
    }
}
```

---

### Solution 5: HikariCP Connection Pool Sizing Rule

To avoid connection pool deadlocks where threads wait on themselves:

$$\text{Pool Size} \ge \text{Maximum Threads} \times (\text{Connections Per Thread} - 1) + 1$$

#### Example `application.yml` Configuration:
```yaml
spring:
  datasource:
    hikari:
      pool-name: ProdHikariCP
      maximum-pool-size: 30
      minimum-idle: 10
      connection-timeout: 10000     # 10 seconds: fails fast instead of hanging forever
      idle-timeout: 300000          # 5 minutes
      max-lifetime: 1800000         # 30 minutes
      leak-detection-threshold: 5000 # Logs warning if connection held > 5 seconds
```

---

### Solution 6: Virtual Threads Pinning (Java 21+)

In Java 21+, virtual threads run on top of carrier OS threads. If a virtual thread enters a `synchronized` block and encounters blocking I/O, it **pins** the carrier OS thread. If multiple virtual threads do this, carrier threads become exhausted, freezing the entire application.

#### Diagnosing Pinning via JVM Flag:
Add this flag to JVM startup arguments:
```bash
-Djdk.tracePinnedThreads=full
```

#### Example Output:
```text
Thread[#45,ForkJoinPool-1-worker-3,5,CarrierThreads]
    java.base/java.lang.VirtualThread$VThreadContinuation.onPinned(VirtualThread.java:183)
    com.example.service.FileExportService.writeReport(FileExportService.java:34) <== PINNED HERE
```

#### Fixing Pinning: Replace `synchronized` with `ReentrantLock`:
```java
// BAD: Virtual thread pins underlying OS carrier thread during blocking I/O
public synchronized void writeReport(byte[] data) throws IOException {
    fileOutputStream.write(data); // Blocking I/O inside synchronized!
}

// GOOD: ReentrantLock allows virtual thread to unmount cleanly without pinning carrier thread
private final ReentrantLock lock = new ReentrantLock();

public void writeReport(byte[] data) throws IOException {
    lock.lock();
    try {
        fileOutputStream.write(data);
    } finally {
        lock.unlock();
    }
}
```

---

## 5. Complete Production Cheat Sheet / Runbook

| Step | Goal | Exact Command / Code |
|---|---|---|
| **1. Identify PID** | Locate Java process | `jps -v` or `ps aux \| grep java` |
| **2. Capture Thread Dumps** | Take 3 dumps (10s apart) | `jcmd <PID> Thread.print -l > dump1.tdump` |
| **3. Container / K8s Dump** | Dump inside container | `kubectl exec <pod> -- jcmd 1 Thread.print -l > dump.txt` |
| **4. Check Built-in Deadlock** | Search for deadlock block | `grep -A 40 "Found one Java-level deadlock" dump1.tdump` |
| **5. Inspect DB Locks (Postgres)** | Check blocked transactions | Run `SELECT * FROM pg_locks WHERE NOT granted;` |
| **6. Inspect DB Locks (MySQL)** | View latest InnoDB deadlock | `SHOW ENGINE INNODB STATUS\G` |
| **7. Recover Service** | Restore availability | Restart pod / roll back deployment |
| **8. Long-term Code Fix** | Prevent recurrence | • Order lock acquisition by ID<br>• Use `tryLock(timeout)` with jitter<br>• Move slow HTTP calls outside `@Transactional`<br>• Configure `@Retryable` on DB deadlocks |

---

## 6. Where Do Locks Actually Live? (JVM vs Distributed vs Database Locks)

A common misconception among Java backend developers is:
> *"I can just put `synchronized` or `ReentrantLock` in my Java service method to prevent race conditions and concurrent double-spends."*

In modern production architectures, **this is usually false and dangerous.**

---

### The Fundamental Trap: Single JVM vs Multi-Pod Architecture

`synchronized` and `ReentrantLock` are **in-memory, JVM-local primitives**. They operate exclusively within the memory space (Heap and Metaspace) of a **single operating system process**.

In modern cloud production (Kubernetes, AWS ECS, GCP Cloud Run), your application is almost never a single JVM instance. You run 2, 5, 20, or 100 pods behind a Load Balancer:

```
                          [User Traffic / API Gateway]
                                       │
                                       ▼
                       [Load Balancer (AWS ALB / Nginx)]
                                 ┌─────┴─────┐
                                 │           │
                       Request 1 │           │ Request 2 (Concurrent)
                                 ▼           ▼
                      ┌───────────────┐ ┌───────────────┐
                      │  Pod A (JVM)  │ │  Pod B (JVM)  │
                      │               │ │               │
                      │ synchronized  │ │ synchronized  │
                      │   (Heap 1)    │ │   (Heap 2)    │
                      └───────┬───────┘ └───────┬───────┘
                              │                 │
                              │ Both execute    │ Both execute
                              │ simultaneously! │ simultaneously!
                              ▼                 ▼
                       ┌───────────────────────────────┐
                       │     Shared PostgreSQL / MySQL │
                       │    (DATA CORRUPTION / OVERDRAW)│
                       └───────────────────────────────┘
```

#### Why `synchronized` Fails Here:
1. **Heap 1 and Heap 2 are completely isolated:** Pod A's JVM has no way of knowing that Pod B is executing the exact same method on the exact same customer account.
2. Both pods acquire their local JVM locks simultaneously.
3. Both pods proceed to read and write to the database concurrently, causing **duplicate charges, negative bank balances, or inventory overselling**.

---

### The 3 Tiers of Locking in Backend Architecture

To protect shared state correctly, locks must live at the appropriate layer of your infrastructure:

```
┌────────────────────────────────────────────────────────────────────────┐
│ Tier 1: JVM In-Memory Locks (synchronized, ReentrantLock, StampedLock) │
│ • Scope: Local to 1 JVM heap (Nanoseconds)                             │
│ • Use for: Local caches (Caffeine), rate-limit buckets, buffer pooling │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│ Tier 2: Distributed Locks (Redis / Redisson, ZooKeeper / Curator)      │
│ • Scope: Cross-Pod, Cross-JVM cluster (1 - 5 milliseconds)             │
│ • Use for: Cross-pod job deduplication, distributed cron (ShedLock)    │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│ Tier 3: Database Locks (Pessimistic SELECT FOR UPDATE, Optimistic @Ver)│
│ • Scope: Shared Central Storage (Disk / Engine Latency)                │
│ • Use for: Financial ledgers, inventory deductions, balance mutations  │
└────────────────────────────────────────────────────────────────────────┘
```

#### Detailed Comparison Table:

| Property | Tier 1: JVM-Level Lock | Tier 2: Distributed Lock (Redis) | Tier 3: Database Lock |
|---|---|---|---|
| **Mechanism** | `synchronized`, `ReentrantLock` | Redis (`SET NX PX`, Redisson `RLock`) | `SELECT FOR UPDATE` or `@Version` |
| **Scope** | Single JVM / Single Pod | All Pods in Cluster | All Services touching the Database |
| **Speed / Latency** | Nanoseconds (CPU memory ops) | 1 to 5 ms (Network round-trip) | 5 to 50 ms (DB disk & transaction log) |
| **Resilience to Pod Crash** | Lock vanishes if pod restarts | Auto-released via TTL / Lease time | Auto-released when DB connection drops |
| **Deadlock Scope** | Inside JVM thread pool | Distributed cycle across pods | DB engine lock graph (InnoDB/Postgres) |
| **When to Use** | • Local in-memory caches<br>• Local thread worker coordination<br>• Singletons / lazy initialization | • Scheduled cron jobs across pods<br>• Cross-service operation deduplication<br>• Fast-failing duplicate API calls | • Money / ledger updates<br>• Inventory decrement<br>• Final authority on data integrity |

---

### Tier 1 Example: Where JVM-Level Locks (`ReentrantLock`) ARE Correct

Use JVM-level locks when coordinating threads that access **in-memory data structures private to that specific JVM**.

#### Example: In-Memory Metric Aggregator or Local Batching Queue

```java
package com.example.local;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class LocalBatchingBuffer<T> {
    private final List<T> buffer = new ArrayList<>();
    private final int batchSize;
    private final ReentrantLock lock = new ReentrantLock();

    public LocalBatchingBuffer(int batchSize) {
        this.batchSize = batchSize;
    }

    public void add(T item) {
        lock.lock(); // Correct: Only threads inside this JVM process touch this local buffer
        try {
            buffer.add(item);
            if (buffer.size() >= batchSize) {
                flushLocalBatch();
            }
        } finally {
            lock.unlock();
        }
    }

    private void flushLocalBatch() {
        System.out.println("Flushing " + buffer.size() + " items to external broker...");
        buffer.clear();
    }
}
```

---

### Tier 2 Example: Distributed Locking with Redis (Redisson)

When you run multiple pods and need to coordinate actions (such as preventing two pods from processing the same order ID or invoice simultaneously), use a **Distributed Lock** backed by Redis.

#### Maven Dependency:
```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.27.2</version>
</dependency>
```

#### Production-Grade Distributed Lock Implementation:

```java
package com.example.distributed;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class DistributedOrderService {

    private static final Logger log = LoggerFactory.getLogger(DistributedOrderService.class);

    @Autowired
    private RedissonClient redissonClient;

    public boolean processOrderClusterWide(String orderId) {
        // Unique lock key shared across ALL Kubernetes pods via Redis
        String lockKey = "lock:order:" + orderId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // tryLock(waitTime, leaseTime, unit):
            // waitTime = 2 seconds (time to wait if another pod holds the lock)
            // leaseTime = 10 seconds (auto-release TTL if this pod crashes/dies to prevent deadlocks)
            boolean isLocked = lock.tryLock(2, 10, TimeUnit.SECONDS);

            if (!isLocked) {
                log.warn("Another pod is currently processing orderId: {}. Skipping request.", orderId);
                return false;
            }

            try {
                log.info("Acquired distributed lock for orderId: {} across cluster.", orderId);
                // Execute business logic safely across all pods
                doOrderProcessing(orderId);
                return true;
            } finally {
                // Ensure lock is unlocked ONLY if held by current thread
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    log.info("Released distributed lock for orderId: {}", orderId);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread was interrupted while waiting for distributed lock", e);
            return false;
        }
    }

    private void doOrderProcessing(String orderId) {
        // Safe cross-pod business logic
    }
}
```

> [!TIP]
> **Why Redisson Prevents Distributed Deadlocks:**
> If a pod crashes while holding the lock, Redisson's **lease time (TTL)** ensures Redis automatically expires and drops the lock after the configured timeout (e.g., 10 seconds), preventing the entire cluster from deadlocking forever.

---

### Tier 2 Example: Distributed Cron Jobs Across Pods (`ShedLock`)

When multiple pods run the same `@Scheduled` cron job, you want **exactly one pod** to execute the job, not all 10 pods concurrently.

#### Implementation:
```java
package com.example.scheduler;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ClusterWideCronJob {

    private static final Logger log = LoggerFactory.getLogger(ClusterWideCronJob.class);

    // Runs every 5 minutes across the cluster
    // lockAtLeastFor = keeps lock for 30s to prevent fast runs from triggering twice
    // lockAtMostFor  = releases lock after 4m if the pod crashes, preventing deadlock
    @Scheduled(cron = "0 */5 * * * *")
    @SchedulerLock(name = "GenerateDailyReportTask", lockAtMostFor = "4m", lockAtLeastFor = "30s")
    public void generateDailyReport() {
        log.info("This scheduled task runs on EXACTLY ONE pod across the entire Kubernetes cluster!");
        // Report generation logic
    }
}
```

---

### Tier 3 Example: Database-Level Locking (Pessimistic vs Optimistic)

When data consistency is critical (e.g., deducting money or reserving limited inventory seats), the **database** must enforce the lock because it is the single source of truth across all pods and all external systems.

#### A. Database Optimistic Locking (`@Version`): Best for Low Contention

Optimistic locking uses a version column. It requires **no database locks** during read. If another pod updated the row in between, Hibernate throws `OptimisticLockException` during commit.

```java
package com.example.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "bank_accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal balance;

    @Version // JPA Optimistic Locking Field
    private Long version;

    public void debit(BigDecimal amount) {
        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }
        this.balance = this.balance.subtract(amount);
    }
    // Getters and setters
}
```

##### What Happens Under the Hood:
Hibernate generates the following SQL when updating:
```sql
UPDATE bank_accounts 
SET balance = 900, version = version + 1 
WHERE id = 1 AND version = 5;
```
If Pod B already updated `version` from 5 to 6:
- The `UPDATE` statement matches 0 rows.
- Hibernate detects 0 rows updated and throws `OptimisticLockingFailureException`.
- The application catches it and retries or notifies the user. Zero database deadlocks!

---

#### B. Database Pessimistic Locking (`SELECT ... FOR UPDATE`): Best for High Contention

When contention is high and frequent rollbacks from optimistic locking are unacceptable, use pessimistic row locks.

```java
package com.example.repository;

import com.example.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    // Generates: SELECT * FROM bank_accounts WHERE id = :id FOR UPDATE
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdWithPessimisticLock(@Param("id") Long id);
}
```

##### Service Layer Usage:
```java
@Service
public class AccountTransferService {

    @Autowired
    private AccountRepository accountRepository;

    @Transactional
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        // Enforce deterministic ID ordering to PREVENT database deadlocks:
        Long firstId = fromId < toId ? fromId : toId;
        Long secondId = fromId < toId ? toId : fromId;

        // Row locks acquired in exact ascending order across all pods
        Account first = accountRepository.findByIdWithPessimisticLock(firstId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + firstId));
        Account second = accountRepository.findByIdWithPessimisticLock(secondId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + secondId));

        if (fromId.equals(first.getId())) {
            first.debit(amount);
            second.credit(amount);
        } else {
            second.debit(amount);
            first.credit(amount);
        }
    }
}
```

---

### Architectural Decision Tree: "Where Should My Lock Live?"

```
                       Do you have more than 1 Pod / JVM?
                                     │
                        ┌────────────┴────────────┐
                        │ No                      │ Yes
                        ▼                         ▼
            [JVM In-Memory Locks]       Is the shared state in a
          • synchronized / Reentrant    Relational Database?
          • StampedLock                           │
                                         ┌────────┴────────┐
                                         │ Yes             │ No
                                         ▼                 ▼
                              Is contention high?    [Distributed Lock]
                                   │                 • Redis (Redisson)
                       ┌───────────┴───────────┐     • ZooKeeper / Curator
                       │ Low                   │ High• ShedLock (Cron jobs)
                       ▼                       ▼
              [Optimistic Lock]       [Pessimistic Lock]
              • @Version column       • SELECT FOR UPDATE
              • Fast, no lock wait    • Order by Primary Key
              • Retry on collision    • Short transactions
```
