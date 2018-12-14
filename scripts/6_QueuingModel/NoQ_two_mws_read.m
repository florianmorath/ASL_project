#{
    This NoQ represents the model of the two middleware system. 
    The analysis is done for read-only workload data. 
#}


# input data

N=48
Z=0

st_netthread=1.4379151963363624e-05
st_worker=0.005422811454882355
delay_client=0
delay_network=0.0005376733152884033
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

    0.00000   0.77134   0.77134   0.02063   0.02063   0.97243   0.97243   1.54267

    R =

    0.000000   0.000538   0.000538   0.000015   0.000015   0.015640   0.015640   0.000538

    Q =

    0.00000    0.77134    0.77134    0.02106    0.02106   22.43627   22.43627    1.54267

    X =

    2869.2   1434.6   1434.6   1434.6   1434.6   1434.6   1434.6   2869.2


#}


