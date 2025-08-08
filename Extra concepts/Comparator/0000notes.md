# Comparator

Cpp

follow a comes first so for ascending rule is a comes first if `a<b` 

and decending a comes firs when `a>b`!! so yha sb a ke perpective mai hota!!

a alwways come first uske hisab se condition chnage hogi!!

```cpp
#include <bits/stdc++.h>
using namespace std;

class Complex {
public:
    int i, j, k;
    Complex(int i, int j, int k) : i(i), j(j), k(k) {}
};

int main() {
    vector<Complex> arr = {
        {1, 5, 9},
        {2, 7, 3},
        {3, 2, 8},
        {4, 7, 1}
    };

    // Sort by j in descending order using lambda
    sort(arr.begin(), arr.end(), [](const Complex &a, const Complex &b) {
        return a.j > b.j; // descending
    });

    for (auto &c : arr) {
        cout << "i=" << c.i << " j=" << c.j << " k=" << c.k << "\n";
    }
}

```
Java

```cpp
import java.util.*;

class Complex {
    int i, j, k;
    Complex(int i, int j, int k) {
        this.i = i;
        this.j = j;
        this.k = k;
    }
}

public class Main {
    public static void main(String[] args) {
        List<Complex> list = Arrays.asList(
            new Complex(1, 5, 9),
            new Complex(2, 7, 3),
            new Complex(3, 2, 8),
            new Complex(4, 7, 1)
        );

        // Sort by j in descending order using lambda
        list.sort((a, b) -> b.j - a.j);

        for (Complex c : list) {
            System.out.println("i=" + c.i + " j=" + c.j + " k=" + c.k);
        }
    }
}
```
Output:
```
i=2 j=7 k=3
i=4 j=7 k=1
i=1 j=5 k=9
i=3 j=2 k=8
```

sort follows a come's first logic in cpp,Multiset follows same rule

```cpp
#include <bits/stdc++.h>
using namespace std;

int main() {
    // Lambda comparator for descending order
    auto cmp = [](int a, int b) {
        return a > b; // bigger values come first
    };

    multiset<int, decltype(cmp)> ms(cmp);

    ms.insert(5);
    ms.insert(1);
    ms.insert(9);
    ms.insert(3);
    ms.insert(5); // duplicates allowed in multiset

    for (int x : ms) {
        cout << x << " ";
    }
}

//9 5 5 3 1

```
Other container follow same rule is set,multiset,map,multimap

💡 Rule of thumb:
If the container is tree-based (set/map variants), it uses the same comparator semantics as std::sort.
If it’s heap-based (priority_queue) or hash-based (unordered_), it does not.

## PQ

```cpp
#include <bits/stdc++.h>
using namespace std;

class Complex {
public:
    int i, j, k;
    Complex(int i, int j, int k) : i(i), j(j), k(k) {}
};

int main() {
    // Lambda comparator for priority_queue
    auto cmp = [](const Complex &a, const Complex &b) {
        return a.k < b.k; // max-heap on k (descending)
    };

    priority_queue<Complex, vector<Complex>, decltype(cmp)> pq(cmp);

    pq.push({1, 5, 9});
    pq.push({2, 7, 3});
    pq.push({3, 2, 8});
    pq.push({4, 7, 1});

    while (!pq.empty()) {
        auto c = pq.top();
        pq.pop();
        cout << "i=" << c.i << " j=" << c.j << " k=" << c.k << "\n";
    }
}

```
Java

```java
import java.util.*;

class Complex {
    int i, j, k;
    Complex(int i, int j, int k) {
        this.i = i;
        this.j = j;
        this.k = k;
    }
}

public class Main {
    public static void main(String[] args) {
        // Comparator for k descending
        PriorityQueue<Complex> pq = new PriorityQueue<>((a, b) -> b.k - a.k);

        pq.add(new Complex(1, 5, 9));
        pq.add(new Complex(2, 7, 3));
        pq.add(new Complex(3, 2, 8));
        pq.add(new Complex(4, 7, 1));

        while (!pq.isEmpty()) {
            Complex c = pq.poll();
            System.out.println("i=" + c.i + " j=" + c.j + " k=" + c.k);
        }
    }
}

```














