import numpy as np
import math

def init_mlp(inputs, targets, nhidden):
    """ Initialize network """

    # Set up network size
    nin = np.shape(inputs)[1]
    nout = np.shape(targets)[1]
    ndata = np.shape(inputs)[0]
    nhidden = nhidden

    #Initialize network
    weights1 = (np.random.rand(nin+1, nhidden)-0.5)*2/np.sqrt(nin)
    weights2 = (np.random.rand(nhidden+1, nout)-0.5)*2/np.sqrt(nhidden)

    return weights1, weights2


def loss_and_gradients(input_x, expected_output_y, weights1, weights2):
    """compute loss and gradients for a given x,y
    
    this function gets an (x,y) pair as input along with the weights of the mlp,
    computes the loss on the given (x,y), computes the gradients for each weights layer,
    and returns a tuple of loss, weights 1 gradient, weights 2 gradient.
    The loss should be calculated according to the loss function presented in the assignment
    
    Arguments:
        input_x {numpy 1d array} -- an instance from the dataset
        expected_output_y {scalar} -- the ground truth
        weights1 {numpy 2d array} -- weights between input layer and hidden layer
        weights2 {numpy 2d array} -- weights between hidden layer and output layer
    
    Returns:
        tuple -- loss, weights 1 gradient, weights 2 gradient, and activations[-1] which is y_hat
    """
    # Initialize gradients
    weights1_gradient, weights2_gradient = np.zeros(weights1.shape), np.zeros(weights2.shape)
    # Initialize loss
    loss = 0
    weighted_outputs, activations = mlpfwd(input_x, weights1, weights2)
    y_hat = activations[-1]
    t3 = activations[0] #dh_output/dw vectors of last layer input errors
    t3 = np.append(t3,1)
    delta_o = (y_hat - expected_output_y)*y_hat*(1-y_hat)
    bla = t3*delta_o
    weights2_gradient[0] = [bla[0]];weights2_gradient[1] = [bla[1]];weights2_gradient[2] = [bla[2]]

    right = (delta_o *weights2)
    right = right.flatten()
    left = (t3*(1-t3))
    delta_h = right  * left
    delta_h = delta_h[0:2]
    weights1_gradient = np.array([ delta_h * input_x[i] for i in range(len(input_x))])

    loss = 0.5*((y_hat-expected_output_y)**2)
    return loss, weights1_gradient, weights2_gradient, activations[-1]





def sigmoid(x):
  return 1 / (1 + math.e**(-x))

def mlpfwd(input_x, weights1, weights2):
    """feed forward
    
    this function gets an input x and feeds it through the mlp.
    
    Arguments:
        input_x {numpy 1d array} -- an instance from the dataset
        weights1 {numpy 2d array} -- weights between input layer and hidden layer
        weights2 {numpy 2d array} -- weights between hidden layer and output layer
    
    Returns:
        tuple -- list of weighted outputs along the way, list of activations along the way:
        
        1) The first part of the tuple consists of a list, where every item in the list
        holds the values of a layer in the network, before the activation function has been applied
        on it. The value of a layer in the network is the weighted sum of the layer before it.
        
        2) The second part of the tuple consists of a list, where every item in the list holds
        the values of a layer in the network, after the activation function has been applied on it.
        Don't forget to add the bias to a layer, when required.
    """
    input_x = np.array(input_x)
    weighted_outputs, activations = [], []

    h1 = input_x.dot(weights1)
    o1 = sigmoid(h1)

    h2 = np.append(o1,1).dot(weights2)
    o2 = sigmoid(h2)


    #???
    weighted_outputs.append(h1)
    weighted_outputs.append(h2)

    activations.append(o1)
    activations.append(o2)

    return weighted_outputs, activations


def accuracy_on_dataset(inputs, targets, weights1, weights2):
    """compute accuracy
    
    this function gets a dataset and returns model's accuracy on the dataset.
    The accuracy is calculated using a threshold of 0.5:
    if the prediction is >= 0.5 => y_hat = 1
    if the prediction is < 0.5 => y_hat = 0
    
    Arguments:
        inputs {numpy 2d array} -- instances
        targets {numpy 2d array} -- ground truths
        weights1 {numpy 2d array} -- weights between input layer and hidden layer
        weights2 {numpy 2d array} -- weights between hidden layer and output layer

    Returns:
        scalar -- accuracy on the given dataset
    """
    correct = 0
    for i in range(len(inputs)):
        _,activations = mlpfwd(inputs[i],weights1,weights2)
        if activations[-1] >=0.5 and  targets[i] == 1: correct +=1
        elif targets[i] == 0: correct += 1

    return float(correct)/len(inputs)


def mlptrain(inputs, targets, eta, nepochs, weights1, weights2):
    """train the model
    
    Arguments:
        inputs {numpy 2d array} -- instances
        targets {numpy 2d array} -- ground truths
        eta {scalar} -- learning rate
        nepochs {scalar} -- number of epochs
        weights1 {numpy 2d array} -- weights between input layer and hidden layer
        weights2 {numpy 2d array} -- weights between hidden layer and output layer
    """
    ndata = np.shape(inputs)[0]
    # Add the inputs that match the bias node
    inputs = np.concatenate((inputs,np.ones((ndata,1))),axis=1)

    for n in range(nepochs):
        epoch_loss = 0
        predictions = []
        for ex_idx in range(len(inputs)):
            x = inputs[ex_idx]
            y = targets[ex_idx]
            
            # compute gradients and update the mlp
            loss, weights1_gradient, weights2_gradient, y_hat= loss_and_gradients(x, y, weights1, weights2)
            weights1 -= eta * weights1_gradient
            weights2 -= eta * weights2_gradient
            epoch_loss += loss
            predictions.append(y_hat)

        if (np.mod(n,100)==0):
            print n, epoch_loss, accuracy_on_dataset(inputs, targets, weights1, weights2)

    return weights1, weights2
        
        



