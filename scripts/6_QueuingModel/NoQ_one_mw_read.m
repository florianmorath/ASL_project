#{
    This NoQ represents the model of the one middleware system. 
    The analysis is done for read-only workload data. 
#}


# input data

N=48
Z=0

st_netthread=2.1184925472643984e-05
st_worker=0.0026987094161291577
delay_client=0
delay_network=0.000467149454967088

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


#{ experiment = [2018-12-06_23h08]

    U =

    0.00000   1.38481   0.06280   1.00000   1.38481

    R =

    0.000000   0.000467   0.000023   0.015235   0.000467

    Q =

    0.00000    1.38481    0.06701   45.16337    1.38481

    X =

    2964.4   2964.4   2964.4   2964.4   2964.4
#}