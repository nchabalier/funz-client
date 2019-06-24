#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Tue Feb  6 15:32:07 2018
https://eli.thegreenplace.net/2012/01/16/python-parallelizing-cpu-bound-tasks-with-multiprocessing/

"""


def f(ab):
    a = ab[0]
    b = ab[1]
    return(a*b)

X = {'a':[1,2,3],'b':[4,5,6]}
def _getInMapArray(m,i) :
    a=[]
    for k in m.keys():
        a.append(m[k][i])
    return(a)
    
 _getInMapArray(X,1)

n = len(X[list(X.keys())[0]])


## serial
[f(_getInMapArray(X,i)) for i in range(n)]

## thread
#nthreads = 2
#def worker(x, y):
#    y = f(x)
#
#chunksize = int(math.ceil(n / float(nthreads)))
#threads = []
#outs = [{} for i in range(nthreads)]
#
#for i in range(nthreads):
#        # Create each thread, passing it its chunk of numbers to factor
#        # and output dict.
#    t = threading.Thread(
#            target=worker,
#            args=(nums[chunksize * i:chunksize * (i + 1)], outs[i]))
#        threads.append(t)
#        t.start()
#
#    # Wait for all threads to finish
#    for t in threads:
#        t.join()
#
#    # Merge all partial output dicts into a single dict and return it
#    return {k: v for out_d in outs for k, v in out_d.iteritems()}
#
#


## multiprocessing

import multiprocessing

def worker(X, Y_q):
        """ The worker function, invoked in a process. 'nums' is a
            list of numbers to factor. The results are placed in
            a dictionary that's pushed to a queue.
        """
        outdict = {}
        for n in nums:
            outdict[n] = f(n)
        Y_q.put(outdict)

    # Each process will get 'chunksize' nums and a queue to put his out
    # dict into
    out_q = Queue()
    chunksize = int(math.ceil(len(nums) / float(nprocs)))
    procs = []

    for i in range(nprocs):
        p = multiprocessing.Process(
                target=worker,
                args=(nums[chunksize * i:chunksize * (i + 1)],
                      out_q))
        procs.append(p)
        p.start()

    # Collect all results into a single result dict. We know how many dicts
    # with results to expect.
    resultdict = {}
    for i in range(nprocs):
        resultdict.update(out_q.get())

    # Wait for all worker processes to finish
    for p in procs:
        p.join()

    return resultdict

