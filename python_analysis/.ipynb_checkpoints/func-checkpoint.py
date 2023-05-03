import numpy as np
import scipy.integrate
import scipy.optimize
import pandas as pd
from scipy.stats import invgamma
from scipy.interpolate import interp1d
# Parameters-------------------------------------------------------------------------
beta = 1/6*(np.sqrt(2)+np.log(1+np.sqrt(2)))
delta = 1.5
b = 1.5
kexp = 3.1

#Power law prefactor a and exponent c:
a = 1.0
c = 0.12

mean_distance = 1

#spacial resolution
M = 100

#For Non Linear spacing:
l_list = np.logspace(-2,3,M)


#Fix V_drt and vary V_tram in reality only the ratio matters:
V_drt = 50

#For no supply demand we vary frequency from SD frequency to 10x
alpha_list = np.linspace(0,1,100)

#For fixed capacity:
train_capacity = 100

e_train = 9.47
e_car = 2.47
e_drt = 3.28
#---------------x---------------------x-------------------x------------------------x----------------

#Carbon Index------------x--------------------------x----------------------x-----------------------x--

def getCarbonIndex(delta,b,l,dc,alpha,t_w,nu,E,vel_rat,power_law,capacity,dldcFuni,dgdcFbi,Funi,Fbi,ta,ts,mu_data,system_size,reqs,mean_req_dist,chi,freespeedcar):
    if power_law:
        eta = getPowerLaw_etaVectorized(l,dc,nu,E,Funi,Fbi,dldcFuni)
    else:
        eta = b/delta
    CI_drt_uni = (1/eta)*(e_drt/e_car)*dldcFuni
    CI_drt_bi = (1/eta)*(e_drt/e_car)*2*beta*l*Fbi
    CI_pt = 4*mu_data*e_train/(E*l*e_car)
    #CI_pt = 4*mu_data*(float(system_size)/float(l) + 1)*float(system_size)*e_train/(int(reqs)*e_train)
    CI_total = CI_drt_uni + CI_drt_bi + CI_pt
    if power_law:
        return eta,CI_total
    else:
        return CI_total

getCarbonIndexVectorized = np.vectorize(getCarbonIndex)

#------------------x-------------------------------x-----------------------------x---------------------x
#eta as a function of demand (power law)

def getPowerLaw_eta(l,dc,nu,E,Funi,Fbi,dldcFuni):
    lambd = getBimodal_lambdaVectorized(l,dc,nu,E,Funi,Fbi,dldcFuni)
    return 1.0*(a*((lambd)**c))#*get_h_appr(Fbi)
	
def getBimodal_lambda(l,dc,nu,E,Funi,Fbi,dldcFuni):
    rt = (nu*E*(dldcFuni+2*beta*l*Fbi)**3)/((1+Fbi)**2)
    return rt

def getBimodal_DrtOccupancy(delta,b,l,dc,alpha,t_w,nu,E,vel_rat,power_law,capacity,dldcFuni,dgdcFbi,Funi,Fbi,ta,ts,mu_data,system_size,reqs,mean_req_dist,chi,freespeedcar):
    rt = ((dldcFuni+2*beta*l*Fbi)**3)*mean_req_dist/(((1+Fbi)**2)*freespeedcar*chi*3600)
    return rt

getPowerLaw_etaVectorized = np.vectorize(getPowerLaw_eta)
getBimodal_lambdaVectorized = np.vectorize(getBimodal_lambda)
getBimodal_DrtOccupancyVectorized = np.vectorize(getBimodal_DrtOccupancy)

#-------------x-----------------------x-------------------------x------------------------------x
def getmu(l,dc,nu,E,alpha,capacity,dgdcFbi):
    rt = nu*E*dgdcFbi*l/(alpha*np.pi*capacity)
    return rt

def getConvenience(delta,b,l,dc,alpha,t_w,nu,E,vel_rat,power_law,capacity,dldcFuni,dgdcFbi,Funi,Fbi,ta,ts,mu_data,system_size,reqs,mean_req_dist,chi,freespeedcar):
    V_tram = vel_rat
    Vm = V_tram
    mu = mu_data
    rt1 = delta*dldcFuni + t_w*Funi
    if (l>=Vm*ta):
        V_tm = l/(l/Vm + ta + ts)
    else:
        V_tm = l/(2*np.sqrt(l*ta/Vm) + ts)
    #V_tm = 15.625/8.33
    rt2 = (1/mu + 2*beta*l*delta + 2*t_w)*Fbi
    rt3 = 4*dgdcFbi/(np.pi*V_tm)
    return rt1 + rt2 + rt3


getmuVectorized = np.vectorize(getmu)
getConvenienceVectorized = np.vectorize(getConvenience)

#----------------------x-----------------------x--------------------------x-------------------------
#Pareto--------x------------x----------------x----------------x--------------x----------------x-------


def getParetoFront(pareto_convenience,pareto_CI):
    size = pareto_CI.shape[0]
    data_ids = np.arange(size)
    pareto_front = np.ones(size,dtype=bool)
    
    for i in range(size):
        for j in range(size):
            if ((pareto_convenience[j]>pareto_convenience[i]) and (pareto_CI[j]<pareto_CI[i])):
                pareto_front[i] = 0
                break
            
    return data_ids[pareto_front]

getParetoFrontVectorized = np.vectorize(getParetoFront)
#-------------------------------------------------------------------------------------------------------
#Tram Speed-----------------------------x----------------x------------

def getTramSpeed(l,ta,ts,vel_rat):
    V_tram = vel_rat
    Vm = V_tram
    if (l>=Vm*ta):
        V_tm = l/(l/Vm + ta + ts)
    else:
        V_tm = l/(2*np.sqrt(l*ta/Vm) + ts)
    return V_tm

getTramSpeedVectorized = np.vectorize(getTramSpeed)


#identity---------------------------------------------------------------------------
def identity(x):
    return x


#h function--------x----------------x----------------------x-----------------------------
def get_h_appr(Fbi):
    Fbi_vals = np.array([0.009893343149249056,0.02083329418764246,0.03296779677263839,0.04647082578566297,0.0615477082238598,0.0784417338520248,0.09744215408448897,0.1188939106049447,0.14320941843677237,0.17088233900336447,0.20250337525362738,0.23877673557399326,0.28053512088512533,0.32874535974289976,0.38449446268214804,0.4489208658164727,0.5230363257532109,0.6073139011922267,0.7008160007395302,0.7995038302951234,0.8935069888540305,0.9650533357221032,0.9967085574867303,0.9999982664353511])
    h_vals = np.array([1.0056625641591364,1.006699019485842,1.0090559013857245,1.0113063405011375,1.0145741331232674,1.018750490209771,1.0244413735100006,1.0314302756370417,1.0394092673541047,1.0492129089289994,1.0603401693202699,1.0737486741922024,1.089669580452785,1.1095293628707878,1.1362814066147982,1.1720085925983867,1.2080200001373902,1.245794864109046,1.2811682473356023,1.3080890257225464,1.3315628257079979,1.347512486914894,1.353216463852699,1.3535211130162959])
    h_interp = interp1d(Fbi_vals, h_vals, kind='cubic')

    def h_appr(Fbi):
        try:
            return h_interp(Fbi)
        except ValueError:
            return 1

    return h_appr(Fbi)

#-----------x-----------x------------x----------------------------------
#Integral functions
def get_dldcFuni(x):
    return invgamma.expect(identity, args=(kexp-1,), scale=kexp-2, lb=0, ub=x)

def get_dgdcFbi(x):
    return invgamma.expect(identity, args=(kexp-1,), scale=kexp-2, lb=x, ub=None)

def get_Funi(x):
    return invgamma.cdf(x, kexp-1, scale = kexp-2)

get_dldcFuniVectorized = np.vectorize(get_dldcFuni)
get_dgdcFbiVectorized = np.vectorize(get_dgdcFbi)
get_FuniVectorized = np.vectorize(get_Funi)

#---------------x-------------x-----------------x-------------x-----------
#Traffic wrt drt
def get_traffic(l,dc):
	Funi = get_Funi(dc)
	Fbi = 1 - Funi
	dldcFuni = get_dldcFuni(dc)
	lmbd = getBimodal_lambda(l,dc,1,1,Funi,Fbi,dldcFuni)
	return lmbd**(1-c)/get_h_appr(Fbi)
	
get_trafficVectorized = np.vectorize(get_traffic)