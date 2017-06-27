import random
from deap import creator, base, tools, algorithms



def evalwithfiles(individual):
    lines1 = get_random_line('a.txt')
    lines2 = get_random_line('noisy_a.txt')
    return  (eval_reg(individual,lines2) - eval_reg(individual,lines1) ,)

int2char = ['A','B','C','D']
def eval_single(individual, subseq):
    s = 0
    for i in range(len(individual)):
        if int2char[individual[i]] == subseq[i]:
            s +=1
        
    #return (s*s/len(individual))/(len(individual) + s/len(individual))
    return float(2*s - len(individual)) - (len(individual) + 1)


def eval_reg(individual,lines,fitness=eval_single):
    score = 0
    for line in lines:
        indexs = [random.randint(0,len(line) - len(individual)) for i in range(7)]
        for index in indexs:
            score += fitness(individual,line[index:index+len(individual)+1])
            """ score ~= to count in Z score.. we can compute here the mean and stdev by saving all fitness
            result and approximate the Z score """
    return score



import os, random 
def get_random_line(file_name, n_lines = 50):
    total_bytes = os.stat(file_name).st_size 
    random_point = random.randint(0, total_bytes)
    file = open(file_name)
    file.seek(random_point)
    file.readline() # skip this line to clear the partial line
    lines = []
    for i in range(n_lines):
        l = file.readline()
        if not l:
            break
        lines.append(l)
    return lines

def generate_individual(min_length=2,max_length=8):
    length = random.randint(min_length , max_length)
    return toolbox.individual(n = length)


creator.create("FitnessMax", base.Fitness, weights=(1.0,))
creator.create("Individual", list, fitness=creator.FitnessMax)

toolbox = base.Toolbox()

toolbox.register("attr_bool", random.randint, 0, 3)
toolbox.register("individual", tools.initRepeat, creator.Individual, toolbox.attr_bool, n=3)
toolbox.register("population", tools.initRepeat, list, generate_individual)
#toolbox.register("population", tools.initRepeat, list, toolbox.individual)
toolbox.register("evaluate", evalwithfiles)
toolbox.register("mate", tools.cxTwoPoint)
toolbox.register("mutate", tools.mutFlipBit, indpb=0.05)
toolbox.register("select", tools.selTournament, tournsize=3)

population = toolbox.population(n=300)

NGEN=40
for gen in range(NGEN):
    offspring = algorithms.varAnd(population, toolbox, cxpb=0.5, mutpb=0.1)
    fits = toolbox.map(toolbox.evaluate, offspring)
    for fit, ind in zip(fits, offspring):
        ind.fitness.values = fit
    population = toolbox.select(offspring, k=len(population))
top10 = tools.selBest(population, k=10)
print top10