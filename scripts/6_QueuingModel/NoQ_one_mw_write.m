#{
    This NoQ represents the model of the one middleware system. 
    The analysis is done for write-only workload data. 
#}


# input data

N=48
Z=0

st_netthread=5.937021956549005e-05
st_worker=0.001180374794119698
delay_client=0
delay_network=0.0008008308099752614

m_netthread=1
m_worker=8

# the model
P = [ 0 1 0 0 0;
      0 0 1 0 0;
      0 0 0 1 0;
      0 0 0 0 1;
      1 0 0 0 0];
V = qncsvisits(P);

Q1 = qnmknode("-/g/inf", delay_client);
Q2 = qnmknode("-/g/inf", delay_network);
Q3 = qnmknode("m/m/m-fcfs", st_netthread, m_netthread);
Q4 = qnmknode("m/m/m-fcfs", st_worker, m_worker);
Q5 = qnmknode("-/g/inf", delay_network);
       

# solve 
[U R Q X] = qnsolve("closed", N, {Q1,Q2,Q3,Q4,Q5}, V, Z);

U
R
Q
X

#{ 
    output:

    U =

    0.00000   5.42764   0.40238   1.00000   5.42764

    R =

    0.0000000   0.0008008   0.0000993   0.0053812   0.0008008

    Q =

        0.00000    5.42764    0.67331   36.47141    5.42764

    X =

    6777.5   6777.5   6777.5   6777.5   6777.5
        

#}