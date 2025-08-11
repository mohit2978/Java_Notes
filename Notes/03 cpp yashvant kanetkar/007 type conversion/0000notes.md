# Notes

![alt text](image.png)

Ex e2(e1) --> calls copy constructor!! We want e2 should have it's own copy of p and q so we use `new`.

*z.p --> * of z.p , if in doubt use `*(z.p)`

destructor called twice once for e1 and then for e2.


![alt text](image-1.png)

by deleting p and q we are deleting old p and q!!

we returning  `*this ` as we want to do `e3=e2=e1`, without returning `*this` this cascading will not work.

![alt text](image-2.png)

Now here see move constructor differnce!! Old values whose owner was e1 is now moved to e2!!

Do not use e1 after move as it has now `nullptr` in it. so do not use.

![alt text](image-3.png)

deleting first e2' p and q. e1's p and q are `x.p` and `x.q`.

we do `this !=&x` as someone should not do `e2=move(e2)` .

![alt text](image-4.png)

Ex temp(z) calls copy constructor ,we have deep copy copy constructor so t has new p and q.

it is just cleaner code. Called as `Copy and swap`.

when we do `e1=e1`, a new adress of p and q will be pointed by p and q of e1. 

It is slower as first need to create temp by deep copy but it is clean. Also no if condition for `e1=e1` .

### Very imp

![alt text](image-5.png)

so if you call move constructor or move assignment but you have not created that then copy constructor or assignment will be called.

## Type conversion

![alt text](image-6.png)

![alt text](image-7.png)

### Casting

![alt text](image-8.png)

Dynamic casting needs inheritance so we will see it later.

### Static Casting

![alt text](image-9.png)

if i do i=int(f) --> compile time checks will not be applicable.`static_cast` checks whether casting is feasible or not.

In 2nd example `char *` converted to `void *`.

we cannot cast anything to anything . If you simply use  `sptr=(Sample *)eptr` it will not give any error but `static_cast` will give error.

![alt text](image-10.png)

Const casting is bad idea as you should not chnage const , but sometimes we need it.Then we can use `Const casting`!!

![alt text](image-11.png)

We telling reInterpret a pointer as const char *.

![alt text](image-12.png)

whenw we do `int i=int (s2)` s2 is "123"

then with `iss>> num` we extracting int from string.

no returm type of `operatot int()` as it is obvious it will return int.

`istringstream`--> tells instead of reading from console ,we reading from stream.

similarly,we have `ostringstream`--> menas giving output to string.

### User defined to User defined.

![alt text](image-13.png)

can see above dmy and Date.

dmy--> source

Date--> destination

---

![alt text](image-14.png)

![alt text](image-15.png)

in dmy we have conversion function.

`ostringStream` we writing something into string.

Let put conversion in destination Object .

![alt text](image-16.png)

![alt text](image-17.png)


![alt text](image-18.png)

---

![alt text](image-19.png)

![alt text](image-20.png)


![alt text](image-21.png)

![alt text](image-22.png)














