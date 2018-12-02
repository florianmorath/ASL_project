#{
    This NoQ represents the model of the two middleware system. 
    The analysis is done for write-only workload data. 
#}


# input data

N=48
Z=0

st_netthread=2.510889362214876e-05
st_worker=0.001663327647854486
delay_client=0
delay_network=0.0004347369299622783

m_netthread=1
m_worker=8

# the model
P = [ 0 0.5 0.5 0 0 0 0 0;
      0 0 0 1 0 0 0 0;
      0 0 0 0 1 0 0 0;
      0 0 0 0 0 1 0 0;
      0 0 0 0 0 0 1 0;
      0 0 0 0 0 0 0 1;
      0 0 0 0 0 0 0 1;
      1 0 0 0 0 0 0 0];
V = qncsvisits(P);

Q1 = qnmknode("-/g/inf", delay_client);
Q2 = qnmknode("-/g/inf", delay_network);
Q3 = qnmknode("-/g/inf", delay_network);
Q4 = qnmknode("m/m/m-fcfs", st_netthread, m_netthread);
Q5 = qnmknode("m/m/m-fcfs", st_netthread, m_netthread);
Q6 = qnmknode("m/m/m-fcfs", st_worker, m_worker);
Q7 = qnmknode("m/m/m-fcfs", st_worker, m_worker);
Q8 = qnmknode("-/g/inf", delay_network);
       

# solve 
[U R Q X] = qnsolve("closed", N, {Q1,Q2,Q3,Q4,Q5}, V, Z);

U
R
Q
X

#{ 
    output:

    U =

   0.00000   2.02315   2.02315   0.11685   0.11685   0.96759   0.96759   4.04631

    R =

    0.0000000   0.0004347   0.0004347   0.0000284   0.0000284   0.0042592   0.0042592   0.0004347

    Q =

        0.00000    2.02315    2.02315    0.13229    0.13229   19.82141   19.82141    4.04631

    X =

    9307.5   4653.7   4653.7   4653.7   4653.7   4653.7   4653.7   9307.5

#}