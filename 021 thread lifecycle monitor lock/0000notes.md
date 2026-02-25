# Notes


```java
@FunctionalInterface
public interface Runnable {
    // Exactly one abstract method. Takes nothing, returns nothing.
    public abstract void run();
}
```

Because Runnable is a functional interface, Java knows the target method is run(). You can just pass the behavior directly:

```java
// () represents the empty inputs for run()
Thread myThread = new Thread(() -> {
    System.out.println("Running in a separate thread!");
    // Do some heavy background work...
});

myThread.start();
```
## Separation of Concerns (The Worker vs. The Work)

 Thread and a Runnable represent two completely different concepts:

The Thread is the Worker: It is a heavy, complex object that talks to the Operating System, allocates memory, manages CPU time, and handles state (running, blocked, dead).

The Runnable is the Work: It is simply the instruction manual (the task) that needs to be executed.

If you don't separate them, you are tightly coupling the logic of what needs to be done with the mechanics of how the CPU runs it. By passing a Runnable into a Thread, you are handing the "instruction manual" to the "worker."


By default, a Thread object is completely empty. It knows how to run, but it doesn't know what to run.

If you just create a thread like this: Thread t = new Thread(); and call t.start(), the thread will spin up, look for work, find absolutely nothing, and die instantly.

Passing the Runnable into the constructor is how you hand the "instruction manual" to this empty worker. Here is exactly how that works under the hood.

## The Constructor Injection

When you call `new Thread( () -> System.out.println("Hello") )`, you are using a specific constructor of the Thread class.

That constructor takes your lambda (the Runnable) and saves it into that private target variable.

![alt text](<021thread lifecycle monitor lock_240521_232002_250714_011521_1.jpg>) ![alt text](<021thread lifecycle monitor lock_240521_232002_250714_011521_2.jpg>) ![alt text](<021thread lifecycle monitor lock_240521_232002_250714_011521_3.jpg>) ![alt text](<021thread lifecycle monitor lock_240521_232002_250714_011521_4.jpg>) ![alt text](<021thread lifecycle monitor lock_240521_232002_250714_011521_5.jpg>) ![alt text](<021thread lifecycle monitor lock_240521_232002_250714_011521_6.jpg>)

## how does calling .start() actually execute your lambda recipe?

This is a classic "under the hood" JVM question, and it involves a little bit of Java source code magic and the Operating System.

Here is exactly what happens step-by-step.

Step 1: Storing the Recipe
When you create the Thread and pass your lambda, you are using this constructor: new Thread(Runnable target).

Inside the Thread class, Java takes your lambda and saves it into a private variable called target.


```java
// Inside java.lang.Thread
private Runnable target; // This holds your lambda!

public Thread(Runnable target) {
    this.target = target;
}
```
Step 2: The .start() Magic (Native OS Call)
When you call myThread.start(), it does not immediately call your lambda. Instead, start() does some heavy lifting. It checks to make sure the thread isn't already running, and then it calls a hidden method called start0().
start0() is a native method (written in C/C++). It talks directly to your computer's Operating System (like Windows or Linux) and says: "Hey OS, please allocate memory and create a brand new, physical thread for me."

Step 3: The OS Triggers .run()
Once the Operating System has created the actual background thread and the CPU is ready to give it some processing time, the OS tells the JVM to wake up.

The JVM responds by automatically calling the Thread object's internal .run() method on that brand new background thread.

Step 4: Executing Your Lambda
If you look at the actual source code for java.lang.Thread, the run() method looks exactly like this:

```java
@Override
public void run() {
    // If you passed a lambda (Runnable) into the constructor...
    if (target != null) {
        // ...execute it right now!
        target.run(); 
    }
}
```
Boom. That target.run() is the exact moment your lambda recipe is finally executed.

⚠️ The Ultimate Interview Question: start() vs run()
Because of this architecture, interviewers love to ask: "What happens if I call myThread.run() directly instead of myThread.start()?"

If you call myThread.run() directly, the code will execute... but it will not run in a new thread.
Because you bypassed the .start() method, you never talked to the Operating System to create a background thread. Your lambda will just execute like a normal method call on the main thread, freezing your application until it finishes.

You must call .start() to transfer that responsibility to the Operating System!

 ![alt text](<021thread lifecycle monitor lock_240521_232002_250714_011521_7.jpg>) ![alt text](<021thread lifecycle monitor lock_240521_232002_250714_011521_8.jpg>) ![alt text](<021thread lifecycle monitor lock_240521_232002_250714_011521_9.jpg>)