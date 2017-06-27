from selector_factory import register

eps = 0.1
smooth = 0.2

#the difference
@register
def difference( a =0. , b = 0.):
    return a -b

@register
def chi2(a=0. , b=0. , directed =   True ):
    if a <= 0 and b <= 0: return 0.
    a = max(a,0)
    b = max(b,0)
    dom = a + b  + eps

    g =  (a-b)**2 / dom
    if directed and b > a:
        return -g
    return g

#directed returns -g . needed to make this selection functions compitable with other function types
@register
def gini(a=0. , b= 0. , directed = True):
    if a <= 0 and b <= 0: return 0.
    a = max(a,0)
    b = max(b,0)
    dom = a + b  + eps

    p = a / dom
    g =  p * (1 - p) #real gini impurity
    g = max(a/dom , b/dom)
    if directed and b > a:
        return -g
    return g

@register
def combined(a=0. , b=0. , directed = True):
    return gini(a,b,False)*difference(a,b)
