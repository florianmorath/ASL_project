#{
    This NoQ represents the model of the one middleware system. 
    The analysis is done for read-only workload data. 
#}


# input data

N=48
Z=0

st_netthread=2.6320389569781902e-05
st_worker=0.0026662031004666058
delay_client=0
delay_network=0.0006332762832956317

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

    0.00000   1.90017   0.07898   1.00001   1.90017

    R =

    0.000000   0.000633   0.000029   0.014702   0.000633

    Q =

        0.00000    1.90017    0.08575   44.11391    1.90017

    X =

    3000.5   3000.5   3000.5   3000.5   3000.5  

#}