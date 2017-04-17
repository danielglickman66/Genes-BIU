from mlp import *
import numpy as np


def scalar_eq(s1,s2):
    res = np.abs(s1-s2) < 0.001
    print "expected:",s2
    print "got     :",s1
    if not res:
    	print "wrong!"
    else:
    	print "correct!"


def eq(a1,a2):
	print "expected:",a2
	print "got     :",a1
	if not(np.allclose(a1,a2)):
  		print "wrong!"
  	else:
  		print "correct!"


print "mlpfwd check:"
print "============="

weights1 = np.array([[1,2],[4,5]])
weights2 = np.array([[1],[4],[8]])

correct = np.array([0.99999774])
wo, ac = mlpfwd([1,4], weights1, weights2)

eq(ac[-1], correct)


weights1 = np.array([[1,2,3,1],[4,5,6,1],[7,8,9,1]])
weights2 = np.array([[1,1],[4,1],[8,1],[9,1],[2,1]])

correct = np.array([ 1.,0.99330704])
wo, ac = mlpfwd([1,4,6], weights1, weights2)

eq(ac[-1], correct)


print "\nloss and gradients check:"
print "========================="

weights1 = np.array([[1,2],[4,5]])
weights2 = np.array([[1],[4],[8]])

loss, w1g, w2g, _ = loss_and_gradients([1,2], [1], weights1, weights2)
correct_loss = 2.55528907123e-12
correct_w1g  = np.array([[-6.30538383e-16,-1.25600082e-16],[-1.26107677e-15,-2.51200164e-16]])
correct_w2g  = np.array([[-5.10993597e-12],[-5.11053519e-12],[-5.11056659e-12]])

print "checking loss:"
scalar_eq(loss, correct_loss)
print "\nchecking weight1 gradient"
eq(w1g, correct_w1g)
print "\nchecking weight2 gradient"
eq(w2g, correct_w2g)

