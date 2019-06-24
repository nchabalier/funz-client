#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Tue Feb  6 15:32:07 2018
https://eli.thegreenplace.net/2012/01/16/python-parallelizing-cpu-bound-tasks-with-multiprocessing/

"""


def f(x,y):
    return(x*y)

X = {'x':[1,2,3],'y':[4,5,6]}














import multiprocessing


def F(queue, *vargs):
    queue.put(f(*vargs))


queue0 = multiprocessing.Queue()

process01 = multiprocessing.Process(target=F, args=(queue0, 3, 4))
process01.start()

process02 = multiprocessing.Process(target=F, args=(queue0, 4, 4))
process02.start()

process01.join()
process02.join()

print(queue0.get())

