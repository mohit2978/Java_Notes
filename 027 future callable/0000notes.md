



```java
public static void main(String args[]) {

    ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(1, 1, 1, TimeUnit.HOURS, new ArrayBlockingQueue<>(10),
            Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());

    //new thread will be created and it will perform the task
    poolExecutor.submit(() -> {
        System.out.println("this is the task, which thread will execute");
    });

    //main thread will continue processing
}
```
- New thread will be created here: lets say thread1.
- `submit()` creates a new Thread.
- Now, what if caller want to know the status of the thread1. Whether its completed or failed etc.

```java
@NotNull
public Future<?> submit(@NotNull Runnable task) {
    if (task == null) throw new NullPointerException();
    RunnableFuture<Void> ftask = newTaskFor(task, null);
    execute(ftask);
    return ftask;
}



@NotNull
public <T> Future<T> submit(@NotNull Runnable task, T result) {
    if (task == null) throw new NullPointerException();
    RunnableFuture<T> ftask = newTaskFor(task, result);
    execute(ftask);
    return ftask;
}
```
- See return type of Submit is Future, so we can hold this in Future object.

Future:
---------
- Interface which Represents the result of the Async task.
- Means, it allow you to check if:
  - Computation is complete
  - Get the result
  - Take care of exception if any
  - etc.

Example:

```java
public static void main(String args[]) {

    ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(1, 1, 1, TimeUnit.HOURS, new ArrayBlockingQueue<>(10),
            Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());

    //new thread will be created and it will perform the task
    Future<?> futureObj = poolExecutor.submit(() -> {
        System.out.println("this is the task, which thread will execute");
    });

    //caller is checking the status of the thread it created
    System.out.println(futureObj.isDone());
}
```
- Storing Reference in Future object; `<?>` → wild card.



| S.No. | Method Available in Future Interface | Purpose |
|---|---|---|
| 1. | `boolean cancel(boolean mayInterruptIfRunning)` | Attempts to cancel the execution of the task. Returns false, if task can not be cancelled (typically bcoz task already completed); returns true otherwise. |
| 2. | `boolean isCancelled()` | Returns true, if task was cancelled before it get completed. |
| 3. | `boolean isDone()` | Returns true if this task completed. Completion may be due to normal termination, an exception, or cancellation -- in all of these cases, this method will return true. |
| 4. | `V get()` | Wait if required, for the completion of the task. After task completed, retrieve the result if available. |
| 5. | `V get(long timeout, TimeUnit unit)` | Wait if required, for at most the given timeout period. Throws 'TimeoutException' if timeout period finished and task is not yet completed. |



Example:
------------

```java
public static void main(String args[]) {

    ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(1, 1, 1, TimeUnit.HOURS, new ArrayBlockingQueue<>(10),
            Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());

    Future<?> futureObj = poolExecutor.submit(() -> {
        try {
            Thread.sleep(7000);
            System.out.println("this is the task, which thread will execute");
        } catch (Exception e) {
        }
    });

    System.out.println("is Done: " + futureObj.isDone());
    try {
        futureObj.get(2, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
        System.out.println("TimeoutException happened");
    }
    catch (Exception e) {
    }

    try {
        futureObj.get();
    } catch (Exception e) {
    }

    System.out.println("is Done: " + futureObj.isDone());
    System.out.println("is Cancelled: " + futureObj.isCancelled());
}
```
- `System.out.println("is Done: ...")` — Main Thread.
- 2sec me task that hoga nhi as it need 7 sec so it throw Exception.
- `futureObj.get();` — Indefinite Wait by Main Till task completed.
- Till this point, Task is Completed.

Now it works

```java
// Since: 1.6
protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
    return new FutureTask<T>(runnable, value);
}
```
- Calls FutureTask is implementing RunnableFuture.
- RunnableFuture is Extended from Future so FutureTask is child of Future only.



On Submit we are Submitting, FutureTask, which is a composed of:
- Runnable
- State

This FutureTask we send to ThreadPool Executor.

```
Submit() → Submit(Runnable)
         → Submit(Runnable, T)
         → Submit(Callable<>)
```

poolExecutor.submit( () -> {} ) → uses Runnable, not Callable.

Callable:
-----------
- Callable represents the task which need to be executed just like Runnable.
- But difference is:
  - Runnable do not have any Return type.
  - Callable has the capability to return the value.

Runnable Interface:

```java
@FunctionalInterface
public interface Runnable {
    public abstract void run();
}
```
- Runnable dont return Anything.

Callable Interface:

```java
@FunctionalInterface
public interface Callable<V> {
    V call() throws Exception;
}
```
- returns a Value.

```java
public static void main(String args[]) {

    ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(3, 3, 1, TimeUnit.HOURS,
            new ArrayBlockingQueue<>(10), Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());

    //UseCase1
    Future<?> futureObject1 = poolExecutor.submit(() -> {
        System.out.println("Task1 with Runnable");
    });
//calls runnable as not returning any value
    try {
        Object object = futureObject1.get();
        System.out.println(object == null);
    } catch (Exception e) {
    }

    //UseCase2
    List<Integer> output = new ArrayList<>();
    Future<List<Integer>> futureObject2 = poolExecutor.submit(() -> {
        output.add(100);
        System.out.println("Task2 with Runnable and Return object");
    }, output);

    try {
        List<Integer> outputFromFutureObject2 = futureObject2.get();
        System.out.println(outputFromFutureObject2.get(0));
    } catch (Exception e) {
    }

    //UseCase3
    Future<List<Integer>> futureObject3 = poolExecutor.submit(() -> {
        System.out.println("Task3 with Callable");
        List<Integer> listObj = new ArrayList<>();
        listObj.add(200);
        return listObj;
    });
//retuning so it is called using Callable
    try {
        List<Integer> outputFromFutureObject3 = futureObject3.get();
        System.out.println(outputFromFutureObject3.get(0));
    } catch (Exception e) {
    }
}
```

see actual code 
```java
@NotNull
public Future<?> submit(@NotNull Runnable task) {
    if (task == null) throw new NullPointerException();
    RunnableFuture<Void> ftask = newTaskFor(task, null);
    //no output sent only runnable so in above call null is what is output of usecase-1
    //if we see usecase-2 output is not null so that is sent
    execute(ftask);
    return ftask;
}
```
This below one is used for use-case2
```java
@NotNull
public <T> Future<T> submit(@NotNull Runnable task, T result) {
    if (task == null) throw new NullPointerException();
    RunnableFuture<T> ftask = newTaskFor(task, result);
    execute(ftask);
    return ftask;
}
```


CompletableFuture (helps in Async Programming, Line Above)

CompletableFuture:
--------------------------
- Introduced in Java8
- To help in async programming.
- We can considered it as an advanced version of Future provides additional capability like chaining.
- Whatever Future can do, it can do — it is child of Future (implements Future).

How to use this:

1. CompletableFuture.supplyAsync:
------------------------------------------------

```
public static<T> CompletableFuture<T> supplyAsync(Supplier<T> supplier)

public static<T> CompletableFuture<T> supplyAsync(Supplier<T> supplier, Executor executor)
```

- `supplyAsync` method initiates an Async operation.
- 'supplier' is executed asynchronously in a separate thread.
- If we want more control on Threads, we can pass Executor in the method.
- By default its uses, shared **Fork-Join Pool** executor. It dynamically adjust its pool size based on processors.
- No control of/on this if we use this.

By default it takes thread from Fork-Join Pool if you don't want that provide yours. It initiates Async operation just like Submit.






```java
public class CompletableFutureExample {

    public static void main(String args[]) {

        try {
            ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(1, 1, 1,
                    TimeUnit.HOURS, new ArrayBlockingQueue<>(10),
                    Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());

            CompletableFuture<String> asyncTask1 = CompletableFuture.supplyAsync(() -> {
                //"this is the task which need to be completed by thread";
                return "task completed";
            }, poolExecutor); // Executor

            System.out.println(asyncTask1.get());

        } catch (Exception e) {

        }
    }
}
```
- `() -> {...}` is Supplier.
- `poolExecutor` is Executor.
- As it is child of Future, so can use `get()` method.


2. thenApply & thenApplyAsync: (used for Chaining) like `then` of JS


- Apply a function to the result of previous Async computation.
- Return a new CompletableFuture object.

```java
CompletableFuture<String> asyncTask1 = CompletableFuture.supplyAsync(() -> {
    //Task which thread need to execute
    return "Concept and ";
}, poolExecutor).thenApply((String val) -> {
    //functionality which can work on the result of previous async task
    return val + "Coding";
});
```

'thenApply' method:
------------------------------
- Its a Synchronous execution.
- Means, it uses same thread which completed the previous Async task.

'thenApplyAsync' method:
------------------------------
- Its a Asynchronous execution.
- Means, it uses different thread (from 'fork-join' pool, if we do not provide the executor in the method), to complete this function.
- If Multiple 'thenApplyAsync' is used, ordering can not be guarantee, they will run concurrently.



```java
public class CompletableFutureExample {

    public static void main(String args[]) {

        try {
            ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(1, 1, 1,
                    TimeUnit.HOURS, new ArrayBlockingQueue<>(10),
                    Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());

            CompletableFuture<String> asyncTask1 = CompletableFuture.supplyAsync(() -> {
                System.out.println("Thread Name which runs 'supplyAsync': " + Thread.currentThread().getName());
                return "Concept and ";
            }, poolExecutor).thenApplyAsync((String val) -> {
                System.out.println("Thread Name which runs 'thenApply': " + Thread.currentThread().getName());
                return val + "Coding";
            });

            System.out.println("Thread Name for 'after CF': " + Thread.currentThread().getName());
            // System.out.println(asyncTask1.get());

        } catch (Exception e) {

        }
    }
}
```


- Not providing Executor so By default use Fork Join Pool.

Output:
```text
Thread Name which runs 'supplyAsync': pool-1-thread-1
Thread Name for 'after CF': main
Thread Name which runs 'thenApply': ForkJoinPool.commonPool-worker-9
```

```java
    return "Concept";
}, poolExecutor).thenApplyAsync((String val) -> {
    System.out.println("ThreadName of thenApply: " + Thread.currentThread().getName());
    return "And";
}, poolExecutor);
```
- Can use thread from same Pool Now.
- Now if we need ordering of Task like after Task1, Task2 should start like this then use `thenCompose()`.



3. thenCompose and thenComposeAsync:
------------------------------------------

- Chain together dependent Async operations.
- Means when next Async operation depends on the result of the previous Async one. We can tied them together.
- For async tasks, we can bring some Ordering using this.

```java
CompletableFuture<String> asyncTask1 = CompletableFuture
        .supplyAsync(() -> {
            System.out.println("Thread Name which runs 'supplyAsync': " + Thread.currentThread().getName());
            return "Concept and ";
        }, poolExecutor)
        .thenCompose((String val) -> {
            return CompletableFuture.supplyAsync(() -> {
                System.out.println("Thread Name which runs 'thenCompose': " + Thread.currentThread().getName());
                return val + "Coding";
            });
        });
```

- thenCompose() → Same thread works on Task.
- thenComposeAsync() → New thread works on Task.

```java
public <U> CompletableFuture<U> thenCompose(
        Function<? super T, ? extends CompletionStage<U>> fn) {
    return uniComposeStage(null, fn);
}

public <U> CompletableFuture<U> thenComposeAsync(
        Function<? super T, ? extends CompletionStage<U>> fn) {
    return uniComposeStage(asyncPool, fn);
}
```
- Only with Async you need Executor, as if not Async then Some thread will Execute it!!



```java
CompletableFuture<String> compFutureObj = CompletableFuture.supplyAsync(() -> {
    return "hello";//1st stage
}, poolExecutor)
        .thenComposeAsync((String val) -> {
            return CompletableFuture.supplyAsync(() -> val + "world");//2nd Stage
        })
        .thenComposeAsync((String val) -> {
            return CompletableFuture.supplyAsync(() -> val + "world");//3rd Stage
        });
```
- Now here ordering will be maintained, 1st one will be Completed then 2nd one & After that 3rd one.
- It is guaranteed About Ordering.

4. thenAccept and thenAcceptAsync:
--------------------------------------

- Generally end stage, in the chain of Async operations
- It does not return anything.

```java
CompletableFuture<Void> asyncTask1 = CompletableFuture
        .supplyAsync(() -> {
            System.out.println("Thread Name which runs 'supplyAsync': " + Thread.currentThread().getName());
            return "Concept and ";
        }, poolExecutor)
        .thenAccept((String val) -> System.out.println("All stages completed"));
```

- If you want to Apply more chaining, but it doesnt return Anything but for chaining we need (Return type + Above That) so here return type is Void & Above that is Nothing so cant use Anything more, has to be Above.



5. thenCombine and thenCombineAsync:

- Used to combine the result of 2 Comparable Future.

```java
CompletableFuture<Integer> asyncTask1 = CompletableFuture
        .supplyAsync(() -> {
            return 10;
        }, poolExecutor);

CompletableFuture<String> asyncTask2 = CompletableFuture
        .supplyAsync(() -> {
            return "k ";
        }, poolExecutor);

CompletableFuture<String> combinedFutureObj = asyncTask1.thenCombine(asyncTask2, (Integer val1, String val2) -> val1 + val2);
```
- T, U as i/p & R is return type.
- `combinedFutureObj.get();` gives 10k.
- Combining Result.

```java
public <U, V> CompletableFuture<V> thenCombine(
        CompletionStage<? extends U> other,
        BiFunction<? super T, ? super U, ? extends V> fn) {
    return biApplyStage(null, other, fn);
}

public <U, V> CompletableFuture<V> thenCombineAsync(
        CompletionStage<? extends U> other,
        BiFunction<? super T, ? super U, ? extends V> fn) {
    return biApplyStage(asyncPool, other, fn);
}
```
- Chaining is rarely used by CompletableFuture; combining is highly used!!

```java
@FunctionalInterface
public interface BiFunction<T, U, R> {

    // Applies this function to the given arguments.
    // Params: t – the first function argument
    //         u – the second function argument
    // Returns: the function result
    R apply(T t, U u);

}
```
- T, U as i/p & R is return type.
