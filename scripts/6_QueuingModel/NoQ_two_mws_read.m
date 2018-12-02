#{
    This NoQ represents the model of the two middleware system. 
    The analysis is done for read-only workload data. 
#}


# input data

N=48
Z=0

st_netthread=1.2955283614406268e-05
st_worker=0.005397123125603651
delay_client=0
delay_network=0.00046672072307934
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

#{ 
    output:

    U =

   0.00000   0.67295   0.67295   0.01868   0.01868   0.97274   0.97274   1.34590

    R =

    0.000000   0.000467   0.000467   0.000013   0.000013   0.015698   0.015698   0.000467

    Q =

        0.00000    0.67295    0.67295    0.01904    0.01904   22.63507   22.63507    1.34590

    X =

    2883.7   1441.9   1441.9   1441.9   1441.9   1441.9   1441.9   2883.7

#}


