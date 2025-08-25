# Notes

## Extras of prev lecture

![alt text](image-23.png)

vTable has adresss to virtual function also Pointer to RTTI table and offset to top (used in multiple inheritance).

![alt text](image-24.png)

![alt text](image-25.png)

![alt text](image-26.png)


----

## C++ I/O

![alt text](image.png)

To get Input/output from all above listed sources we use IO streams.

![alt text](image-1.png)

### IO library

![alt text](image-2.png)

For string IO ,we use istringstream for input and ostringstream for output , for both Io we use string stream.

![alt text](image-3.png)

FD--> file descriptor

![alt text](image-4.png)

showm two CMS here one of windows and one of linux in both we have `1>output.txt 2>error.txt`.In windows one 1 is misprinted here.

1 is FD for stdout .so whatever we write to cout woll go to logfile.txt and 2 is FD for stderr ,whatever we write to cerr will go to error.txt !!

clog is also tied to FD 2 but we want cerr goes to different file and clog to different file . So for that we create ofStream object `lf`. we have `rdbuf()` one is 0-arg and other is 1-arg buffer.

To attach clog to rdbuf() of logfile.txt we call `clog.rdbuf(If.rdbuf())`.

## Stream Manipulations


![alt text](image-5.png)

hex is manipulator which converts int to hex. This program shows various manipulators.

`endl` is also a manipultor.

`showpos` to show `+` sign

`internal` to show spaces between 752 and `+`

`iostream` has zero arg manipulators but `iomanip` has `>=1` arguments in manipulator.

`0-arg` manipulators are sticky ,it effect willnot go until you specify any other manipulator. above we have specified `hex` but thenwe need integer values so to get integer value we must specify `decimal` else `hex` effect will still be there.

boolean values print 1 or 0 so to get `true` or `false` use manipulator `boolalpha`.

Manipulators are not `keywords` that are `functions`.

![alt text](image-6.png)

On left you can see endl flow endl is passed as function pointer.Similarly we can pass `tab` to function pointer.

![alt text](image-7.png)

![alt text](image-8.png)

 respective roman number will be printed out.

![alt text](image-9.png)

![alt text](image-10.png)


![alt text](image-11.png)

![alt text](image-12.png)

![alt text](image-13.png)

## exception handling

![alt text](image-14.png)

![alt text](image-15.png)

![alt text](image-16.png)


![alt text](image-17.png)

![alt text](image-18.png)

![alt text](image-19.png)

![alt text](image-20.png)

![alt text](image-21.png)

![alt text](image-22.png)


In C++ , noexcept is a specification that tells the compiler (and readers of your code) whether a function is guaranteed not to throw exceptions.

```cpp
void foo() noexcept;   // foo() will not throw
void bar() noexcept(false); // bar() *may* throw
```