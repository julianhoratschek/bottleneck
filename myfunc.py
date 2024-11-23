import timeit

upto: int = 100000
times: int = 10000

def f1():
    a1: list[int] = []

    for i in range(0, upto):
        a1.append(i)

def f2():
    a2: list[int] = [0] * upto

    for i in range(0, upto):
        a2[i] = i

def f3():
    a3: list[int] = [i for i in range(0, upto)]



print("For-loop:")
print(timeit.timeit(f1, number=times))
print("For with alloc:")
print(timeit.timeit(f2, number=times))
print("Comprehension:")
print(timeit.timeit(f3, number=times))