#{
    This NoQ represents the model of the one middleware system. 
    The analysis is done for write-only workload data. 
#}


# input data

N=48
Z=0

st_netthread=4.13748506034247e-05
st_worker=0.0011025992820804533
delay_client=0
delay_network=0.0005103574480891613

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


#{  experiment = [2018-12-06_23h08]

    U =

    0.00000   3.70294   0.30020   1.00000   3.70294

    R =

    0.0000000   0.0005104   0.0000591   0.0055358   0.0005104

    Q =

    0.00000    3.70294    0.42898   40.16514    3.70294

    X =

    7255.6   7255.6   7255.6   7255.6   7255.6

#}