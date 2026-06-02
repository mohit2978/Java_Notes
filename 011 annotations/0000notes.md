


![alt text](011annotations_240315_020553_250714_011431_1.jpg) ![alt text](011annotations_240315_020553_250714_011431_2.jpg) ![alt text](011annotations_240315_020553_250714_011431_3.jpg) ![alt text](011annotations_240315_020553_250714_011431_4.jpg) ![alt text](011annotations_240315_020553_250714_011431_5.jpg) ![alt text](011annotations_240315_020553_250714_011431_6.jpg) ![alt text](011annotations_240315_020553_250714_011431_7.jpg) ![alt text](011annotations_240315_020553_250714_011431_8.jpg)

Great question! Let's build custom annotations from scratch, step by step.

---

## What is an Annotation?

An annotation is just **metadata** you attach to classes, methods, or fields. It does **nothing by itself** — something else (like reflection) reads it and acts on it.

```java
@MyAnnotation   // ← just a label, does nothing alone
public class Car { }
```

---

## Step 1 — Basic Syntax to Create an Annotation

```java
public @interface MyAnnotation {   // @interface keyword creates an annotation
}
```

That's it — the simplest annotation. But it has no configuration yet.

---

## Step 2 — Add `@Retention` (When is it available?)

This tells Java **how long** the annotation lives.

```java
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)  // ← keep it available at runtime
public @interface MyAnnotation {
}
```

### The 3 Retention Policies:

| Policy | Meaning | Use case |
|---|---|---|
| `SOURCE` | Discarded after compile | `@Override`, `@SuppressWarnings` |
| `CLASS` | Kept in `.class` file but not at runtime | Bytecode tools |
| `RUNTIME` | Available at runtime via reflection | Spring, JUnit, our injector |

> **Always use `RUNTIME`** if you want to read the annotation via reflection.

---

## Step 3 — Add `@Target` (Where can it be applied?)

This restricts **where** your annotation can be used.

```java
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)           // only on classes
public @interface MyAnnotation {
}
```

### All Target Options:

| Target | Applied on |
|---|---|
| `TYPE` | Class, interface, enum |
| `FIELD` | Fields / variables |
| `METHOD` | Methods |
| `PARAMETER` | Method parameters |
| `CONSTRUCTOR` | Constructors |
| `LOCAL_VARIABLE` | Local variables |
| `ANNOTATION_TYPE` | Other annotations |
| `PACKAGE` | Packages |

### You can combine multiple targets:

```java
@Target({ElementType.TYPE, ElementType.METHOD})  // on both classes and methods
public @interface MyAnnotation {
}
```

---

## Step 4 — Add Elements (Parameters to your annotation)

Annotations can hold **values** like a config.

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Component {
    String name() default "";         // optional — has default
    boolean singleton() default true; // optional — has default
    int priority();                   // required — no default!
}
```

### Using it:

```java
@Component(name = "carBean", singleton = true, priority = 1)
public class Car { }
```

### Rules for elements:

| Rule | Detail |
|---|---|
| Types allowed | `String`, primitives, `Class`, `enum`, another annotation, or arrays of these |
| Default value | Use `default` keyword — makes it optional |
| No default | Field becomes **required** |
| Special name `value()` | Can be used without the key name |

### The special `value()` element:

```java
public @interface Component {
    String value() default "";   // named "value" is special!
}

// Now you can write this shorthand:
@Component("carBean")            // instead of @Component(value = "carBean")
public class Car { }
```

---

## Step 5 — Add `@Documented` (Show in Javadoc?)

```java
import java.lang.annotation.Documented;

@Documented                        // appears in generated Javadoc
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Component {
    String value() default "";
}
```

---

## Step 6 — Add `@Inherited` (Do subclasses inherit it?)

```java
import java.lang.annotation.Inherited;

@Inherited                         // subclasses automatically get this annotation
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Component {
}
```

```java
@Component
public class Animal { }

public class Dog extends Animal { }  // Dog automatically has @Component too! ✅
```

> Only works on **class-level** annotations, not methods or fields.

---

## Step 7 — Read it via Reflection (Making it actually DO something)

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Component {
    String value() default "";
    boolean singleton() default true;
}
```

```java
@Component(value = "carBean", singleton = false)
public class Car { }
```

```java
// Reading the annotation at runtime
Class<?> clazz = Car.class;

if (clazz.isAnnotationPresent(Component.class)) {
    Component annotation = clazz.getAnnotation(Component.class);

    System.out.println(annotation.value());      // carBean
    System.out.println(annotation.singleton());  // false
}
```

---

## Full Example — Putting It All Together

```java
// 1. Define annotation
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Log {
    String level() default "INFO";    // log level
    String message() default "";      // custom message
}
```

```java
// 2. Use annotation
@Log(level = "DEBUG", message = "Processing car")
public class Car {

    @Log(level = "WARN")
    public void drive() {
        System.out.println("Driving...");
    }
}
```

```java
// 3. Process annotation via reflection
public class AnnotationProcessor {
    public static void process(Class<?> clazz) throws Exception {

        // Check class-level annotation
        if (clazz.isAnnotationPresent(Log.class)) {
            Log log = clazz.getAnnotation(Log.class);
            System.out.println("[" + log.level() + "] " + log.message()
                             + " on class: " + clazz.getSimpleName());
        }

        // Check method-level annotations
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Log.class)) {
                Log log = method.getAnnotation(Log.class);
                System.out.println("[" + log.level() + "] "
                                 + "method: " + method.getName());
            }
        }
    }

    public static void main(String[] args) throws Exception {
        process(Car.class);
    }
}
```

### Output:
```
[DEBUG] Processing car on class: Car
[WARN] method: drive
```

---

## Everything You Can Add — Summary

```java
@Documented          // ← show in Javadoc
@Inherited           // ← subclasses inherit it
@Retention(RetentionPolicy.RUNTIME)   // ← when it lives
@Target({ElementType.TYPE, ElementType.METHOD})  // ← where it goes
public @interface MyAnnotation {

    // Elements (parameters)
    String value() default "";           // optional with default
    int priority();                      // required, no default
    boolean active() default true;       // optional boolean
    String[] tags() default {};          // array of strings
    Class<?> handler() default Void.class; // class reference
    RetentionPolicy policy()             // enum value
        default RetentionPolicy.RUNTIME;
}
```

---

## Quick Recap — Step by Step

```
1. @interface keyword       → creates the annotation
2. @Retention               → when is it available (use RUNTIME for reflection)
3. @Target                  → where can it be placed
4. Add elements             → give it configurable values
5. @Documented              → optional, shows in Javadoc
6. @Inherited               → optional, subclasses inherit it
7. Read via reflection      → makes it actually do something!
```

> An annotation alone is just a **label**. Reflection is what gives it **power**!












