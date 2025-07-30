# Notes

## Natural alignment of data type

![alt text](image.png)

All these alignments are decided by architecture and compiler!!

short are stored in adress which is multiple of 2!!It is natural alignment ,it will not always be honoured!!

![alt text](image-1.png)

In left program:

- 1st 4 adress for k.t,now 4 bytes padding as k.p needs to be adress that is multiple of 8!!

- So that is why instead of giving sizeOf 12 sizeOf() operator gives size of 16 which include 4 B padding!!

In right program 

- We tell int and char * to take alignment of 4.But even then output is still same.

- alignof(k.p)=max( default alignof(char *) ,alignOf value we have given i.e. alignas) so that why here align of is still 4 ,can see bottom left for rule!!

we want each alignment of 4 so we use pragmapack

`#pragma pack` is a compiler directive used in C and C++ to control the alignment (padding) of members within structs, unions, or classes.

#### ❓ Why it matters

By default, compilers insert padding bytes between members of a struct to ensure proper memory alignment for performance reasons (depending on architecture). #pragma pack can override this behavior to reduce memory usage.

```cpp
#pragma pack(n)    // n can be 1, 2, 4, 8, or 16
```
It sets the alignment boundary to n bytes, which means structure members will be aligned on n-byte boundaries.

so we can use pragmapack(4) on our program!!

![alt text](image-2.png)

![alt text](image-3.png)

![alt text](image-4.png)

![alt text](image-5.png)

![alt text](image-6.png)


![alt text](image-7.png)

![alt text](image-8.png)


![alt text](image-9.png)


![alt text](image-10.png)

![alt text](image-11.png)



![alt text](image-12.png)

![alt text](image-13.png)

![alt text](image-14.png)


![alt text](image-15.png)


![alt text](image-16.png)


![alt text](image-17.png)

![alt text](image-18.png)


![alt text](image-19.png)


![alt text](image-20.png)


![alt text](image-21.png)

































