# Java Input Methods

Java provides multiple ways to read input from the user or from streams.  
Each method has different use-cases, performance characteristics, and levels of abstraction.

---

## 1. `System.in` — Raw Byte Stream

`System.in` is an `InputStream` that reads **raw bytes** from the standard input (keyboard).  
It is the lowest-level input mechanism in Java.

```java
import java.io.IOException;

public class SystemInExample {
    public static void main(String[] args) throws IOException {
        System.out.print("Enter a character: ");
        int byteRead = System.in.read(); // reads one byte
        System.out.println("You entered: " + (char) byteRead);
    }
}
```

> ⚠️ **Limitation:** Reads only one byte at a time. Very tedious for reading strings or numbers.  
> Rarely used directly — usually wrapped inside `BufferedReader` or `InputStreamReader`.

---

## 2. `InputStreamReader` — Bridge: Bytes → Characters

`InputStreamReader` wraps an `InputStream` (like `System.in`) and converts bytes into characters using a character encoding (default: UTF-8).

```java
import java.io.InputStreamReader;
import java.io.IOException;

public class InputStreamReaderExample {
    public static void main(String[] args) throws IOException {
        InputStreamReader isr = new InputStreamReader(System.in);
        System.out.print("Enter a character: ");
        char c = (char) isr.read(); // reads one character
        System.out.println("You entered: " + c);
    }
}
```

> Converts `System.in` (byte stream) into a character stream.  
> Still reads one character at a time — typically wrapped in `BufferedReader` for efficiency.

---

## 3. `BufferedReader` — Fast Buffered Line Reading ⭐ (Competitive Programming Favorite)

`BufferedReader` wraps a `Reader` (like `InputStreamReader`) and buffers characters for efficient reading.  
It reads large chunks of data into an internal buffer, making it **much faster** than `Scanner`.

```java
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class BufferedReaderExample {
    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        System.out.print("Enter your name: ");
        String name = br.readLine(); // reads entire line as String

        System.out.print("Enter your age: ");
        int age = Integer.parseInt(br.readLine().trim()); // parse manually

        System.out.println("Hello, " + name + "! You are " + age + " years old.");
    }
}
```

### Reading Multiple Values on One Line

```java
BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
String[] parts = br.readLine().split(" ");
int a = Integer.parseInt(parts[0]);
int b = Integer.parseInt(parts[1]);
System.out.println("Sum = " + (a + b));
```

### Using `StringTokenizer` for Faster Parsing

```java
import java.io.*;
import java.util.StringTokenizer;

BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
StringTokenizer st = new StringTokenizer(br.readLine());
int x = Integer.parseInt(st.nextToken());
int y = Integer.parseInt(st.nextToken());
```

| Feature | Details |
|---|---|
| Speed | ⚡ Very fast (buffered) |
| Type parsing | Manual (`Integer.parseInt`, etc.) |
| Exception | Must handle `IOException` |
| Best for | Competitive programming, large inputs |

---

## 4. `Scanner` — Easy but Slower

`Scanner` is the most beginner-friendly class. It tokenizes input and can parse `int`, `double`, `String`, etc. directly.

```java
import java.util.Scanner;

public class ScannerExample {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter your name: ");
        String name = sc.nextLine();

        System.out.print("Enter your age: ");
        int age = sc.nextInt();

        System.out.println("Hello, " + name + "! You are " + age + " years old.");
        sc.close();
    }
}
```

### Common `Scanner` Methods

| Method | Reads |
|---|---|
| `sc.next()` | Next token (word) |
| `sc.nextLine()` | Entire line |
| `sc.nextInt()` | Integer |
| `sc.nextDouble()` | Double |
| `sc.nextLong()` | Long |
| `sc.nextBoolean()` | Boolean |
| `sc.hasNext()` | Check if more input exists |

### ⚠️ Common Pitfall: `nextInt()` + `nextLine()`

```java
int n = sc.nextInt();
sc.nextLine(); // consume leftover newline ← IMPORTANT
String line = sc.nextLine(); // now reads correctly
```

> After `nextInt()`, a `\n` is left in the buffer. Call `sc.nextLine()` to consume it before reading a line.

| Feature | Details |
|---|---|
| Speed | 🐢 Slower (uses regex internally) |
| Type parsing | Automatic |
| Exception | No checked exceptions |
| Best for | Small programs, beginners |

---

## 5. `Console` — For Secure Password Input

`System.console()` returns a `Console` object — useful for **reading passwords** without echoing characters.

```java
public class ConsoleExample {
    public static void main(String[] args) {
        java.io.Console console = System.console();

        if (console == null) {
            System.out.println("No console available (e.g., running in IDE)");
            return;
        }

        String username = console.readLine("Enter username: ");
        char[] password = console.readPassword("Enter password: "); // hidden input

        System.out.println("Username: " + username);
        // ⚠️ clear password array after use for security
        java.util.Arrays.fill(password, ' ');
    }
}
```

> ⚠️ `System.console()` returns `null` when running inside an IDE (like IntelliJ/Eclipse).  
> Works correctly in a real terminal.

---

## 6. `DataInputStream` — Reading Primitive Types from Streams

`DataInputStream` reads Java primitive types (`int`, `double`, `char`, etc.) from a binary stream.

```java
import java.io.*;

public class DataInputStreamExample {
    public static void main(String[] args) throws IOException {
        // Typically used with files or sockets, not keyboard input
        // Example: reading from a byte array simulating binary data
        byte[] data = {0, 0, 0, 42}; // represents int 42 in big-endian
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
        System.out.println("Int value: " + dis.readInt()); // prints 42
    }
}
```

> Mainly used for **binary file or network protocol** reading, not interactive console input.

---

## 7. Reading from a File

### Using `BufferedReader` with `FileReader`

```java
import java.io.*;

public class FileInputExample {
    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader("input.txt"));
        String line;
        while ((line = br.readLine()) != null) {
            System.out.println(line);
        }
        br.close();
    }
}
```

### Using `Scanner` with a `File`

```java
import java.util.Scanner;
import java.io.File;

public class FileScannerExample {
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(new File("input.txt"));
        while (sc.hasNextLine()) {
            System.out.println(sc.nextLine());
        }
        sc.close();
    }
}
```

---

## 8. `StreamTokenizer` — Ultra-Fast Tokenizer (Advanced)

`StreamTokenizer` is faster than `Scanner` and even faster than `BufferedReader` + `split()` for competitive programming.

```java
import java.io.*;

public class StreamTokenizerExample {
    public static void main(String[] args) throws IOException {
        StreamTokenizer st = new StreamTokenizer(new BufferedReader(new InputStreamReader(System.in)));

        st.nextToken();
        int a = (int) st.nval;

        st.nextToken();
        int b = (int) st.nval;

        System.out.println("Sum = " + (a + b));
    }
}
```

> ⚡ Fastest way to read numbers in competitive programming contexts.

---

## 9. `System.in` with `try-with-resources`

Always close readers when done using try-with-resources:

```java
import java.io.*;

public class TryWithResourcesExample {
    public static void main(String[] args) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.print("Enter something: ");
            System.out.println("You entered: " + br.readLine());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

---

## Summary Comparison

| Method | Speed | Ease of Use | Type Parsing | Use Case |
|---|---|---|---|---|
| `System.in` | — | ❌ Hard | Manual | Raw bytes only |
| `InputStreamReader` | — | ❌ Hard | Manual | Character conversion |
| `BufferedReader` | ⚡ Fast | 🟡 Medium | Manual | CP, large inputs |
| `Scanner` | 🐢 Slow | ✅ Easy | Automatic | Beginners, small I/O |
| `Console` | — | ✅ Easy | Manual | Password input |
| `DataInputStream` | ⚡ Fast | ❌ Hard | Binary only | Binary/network data |
| `StreamTokenizer` | ⚡⚡ Fastest | ❌ Hard | Numeric | Ultra-fast CP |

---

## Recommended Usage Guide

```
Beginner / Simple programs       →  Scanner
Competitive Programming          →  BufferedReader + StringTokenizer
Ultra-fast CP (numbers only)     →  StreamTokenizer
Reading passwords securely       →  Console
Reading binary data / network    →  DataInputStream
Reading files                    →  BufferedReader + FileReader
```

---

> 📌 **Key Rule:** `BufferedReader` is the go-to for performance-critical code.  
> `Scanner` is convenient but 3–5x slower due to regex-based tokenization.
