#{
    This NoQ represents the model of the two middleware system. 
    The analysis is done for write-only workload data. 
#}


# input data

N=48
Z=0

st_netthread=3.0450680344297455e-05
st_worker=0.0016842866201057973
delay_client=0
delay_network=0.0004758621297610302

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
[U R Q X] = qnsolve("closed", N, {Q1,Q2,Q3,Q4,Q5,Q6,Q7,Q8}, V, Z);

U
R
Q
X


#{  experiment = [2018-12-07_09h02]

    U =

    0.00000   2.18518   2.18518   0.13983   0.13983   0.96679   0.96679   4.37036

    R =

    0.0000000   0.0004759   0.0004759   0.0000354   0.0000354   0.0042393   0.0042393   0.0004759

    Q =

    0.00000    2.18518    2.18518    0.16253    0.16253   19.46712   19.46712    4.37036

    X =

    9184.1   4592.0   4592.0   4592.0   4592.0   4592.0   4592.0   9184.1
#}