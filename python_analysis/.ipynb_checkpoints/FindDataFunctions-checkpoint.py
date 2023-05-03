import numpy as np
import pandas as pd
import os
from HelperFunctions import *
from PtOccupancyFunctions import *

def getBimDirsVaryDCut(directory, ndrt):
    result = []
    if ndrt:
        ndrt = str(ndrt) + "drt"
        path = os.path.join(directory, ndrt)
    else:
        path = directory
    sdirs = [sdir.path for sdir in os.scandir(path) if sdir.is_dir() and "dcut" in sdir.name and "error" not in sdir.name]
    for sdir in sorted(sdirs, key=lambda x: float(x.split("/")[-1].replace("dcut",""))):
        result.append(getBimDirs(sdir, None))

    return result

def getDirs(directory, keys, constrVals):
    sims = []
    pattern = ".*/" + "/".join([("("+constrVals[i]+")" if constrVals[i] else"(\d*\.?\d*)")+keys[i] for i in range(len(keys))]) + "$"
    
    for root, subdirs, files in os.walk(directory):
        vals = re.search(pattern, root)
        if vals != None:
            subresult = {}
            subresult["root"] = root
            for root2, subdirs2, files2 in os.walk(root):
                for file2 in files2:
                    if file2=="0.trips.csv.gz":
                        subresult["trips"] = os.path.join(root2, file2)
                    if file2=="0.vehicleDistanceStats_drt.csv":
                        subresult["drt_dists"] = os.path.join(root2, file2)
                    if file2=="trip_success.csv.gz":
                        subresult["trip_success"] = os.path.join(root2, file2)
                    if file2=="0.CummulativePtDistance.txt":
                        subresult["pt_dist"] = os.path.join(root2, file2)
                    if file2=="0.drt_occupancy_time_profiles_drt.txt":
                        subresult["drt_occupancy"] = os.path.join(root2, file2)
                    if file2=="0.occupancyAnalysis.txt":
                        subresult["pt_occupancy"] = os.path.join(root2, file2)
                    if file2=="0.drt_trips_drt.csv":
                        subresult["drt_trips"] = os.path.join(root2, file2)
                    if file2=="0.drt_detours_drt.csv":
                        subresult["drt_detours"] = os.path.join(root2, file2)
                    if file2=="ph_modestats.txt":
                        subresult["ph_modestats"] = os.path.join(root2, file2)
                    if file2=="pkm_modestats.txt":
                        subresult["pkm_modestats"] = os.path.join(root2, file2)
                    if file2=="output_persons.csv.gz":
                        subresult["persons"] = os.path.join(root2, file2)
                    if file2=="modestats.txt":
                        subresult["modestats"] = os.path.join(root2, file2)
                
            sim = [float(val) for val in vals.groups() if val]
            sim.append(subresult)
            sims.append(sim)
            continue

    return sims

getDirsVectorized = np.vectorize(getDirs)

def getCarDir(directory):
    result = {}
    result["root"] = getDir(directory, "car")
    for root, subdirs, files in os.walk(result["root"]):
        for file in files:
            if file=="0.trips.csv.gz":
                result["trips"] = os.path.join(root, file)
            if file=="trip_success.csv.gz" and "unimodal" not in root:
                result["trip_success"] = os.path.join(root, file)
    
    return result

def getUniDirs(directory, ndrt):
    if ndrt:
        ndrt = str(ndrt) + "drt"
        unidir = os.path.join(directory, ndrt)
    else:
        unidir=directory
    result = {}
    result["root"] = getDir(unidir, "unimodal")
    for root, subdirs, files in os.walk(result["root"]):
        for file in files:
            if file=="0.trips.csv.gz":
                result["trips"] = os.path.join(root, file)
    
    return result

def getDir(path, directory):
    for root, subdirs, files in os.walk(path):
        for subdir in subdirs:
            if subdir == directory:
                return os.path.join(root, subdir)

#-------------------------------------------------------------#

def getTrips(paths, mode):
    if mode == "bimodal":
        columns=["person","trav_time","wait_time","traveled_distance","modes"]
    elif mode == "unimodal":
        columns=["person","trav_time","wait_time", "traveled_distance"]
    elif mode == "car":
        columns=["person", "traveled_distance", "trav_time"]
    path = paths["trips"]
    df = pd.read_csv(path, sep=";").loc[:,columns]
    df["trav_time"] = df["trav_time"].apply(timestmp2sec)
    df.set_index("person", inplace=True)
    if mode=="car":
        df = df.groupby("person").agg(
            {
                "trav_time": np.sum,
                "traveled_distance": np.sum
            }
        )
        return df
    df["wait_time"] = df["wait_time"].apply(timestmp2sec)
    if mode=="unimodal":
        df = df.groupby("person").agg(
            {
                "trav_time": np.sum,
                "wait_time": np.sum,
                "traveled_distance": np.sum,
            }
        )
        return df
    if mode == "bimodal":
        df = df.groupby("person").agg(
            {
                "trav_time": np.sum,
                "wait_time": np.sum,
                "traveled_distance": np.sum,
                "modes": combineModesSeriesStr,
            }
        )
        return df
    
# def getEll(path):
    # return path.split("/")[-1].split("_")[-1]

def getEll(path):
        return re.search(".*/(\d*\.?\d*)l", path)[1]

def getModeStats(paths, columns_ph, columns_pkm):
    path_ph = paths["ph_modestats"]
    path_pkm = paths["pkm_modestats"]
    df_ph = pd.read_csv(path_ph, sep='\t').loc[:,columns_ph]
    df_pkm = pd.read_csv(path_pkm, sep='\t').loc[:,columns_pkm]
    return df_ph.to_numpy()[0], df_pkm.to_numpy()[0]

def getBimodalFraction(paths,frac_columns):
    path_modes = paths["modestats"]
    df_modes = pd.read_csv(path_modes, sep='\t').loc[:,frac_columns]
    return df_modes.to_numpy()[0]

def getDrtVehicleDistances(paths):
    path = paths["drt_dists"]
    #change the argument to drivenDistance_m or occupiedDistance_m
    df = pd.read_csv(path, sep=";")["drivenDistance_m"]
    return df

def getTripSuccess(paths):
    path = paths["trip_success"]
    df = pd.read_csv(path, sep=";", index_col="personId")
    return df

def getCummulativePtDistance(paths):
    path = paths["pt_dist"]
    df = pd.read_csv(path).values[0, 0]
    return df

def getDrtOccupandyAndStandingFrac(paths, exclude_empty_vehicles, count_idle_vehicles=False, req_time_only=True):
    path = paths["drt_occupancy"]
    df = pd.read_csv(path, sep="\t")
    df["time"] = df["time"].apply(timestmphm2sec)
    if req_time_only:
        df = df[df["time"]<=4200]
    drt_occ, drt_deviation = getAverageOcc(
        df.drop(columns="time"), exclude_empty_vehicles=exclude_empty_vehicles, count_idle_vehicles=count_idle_vehicles
    )
    drt_standing_frac = getStandingFraction(df.drop(columns="time"))
    return drt_occ, drt_standing_frac

def getPtOccupancy(paths, pt_interval=600):
    path = paths["pt_occupancy"]
    #TODO generalize (no 900)
    av_pt_occ, av_pt_occ_sq, n_pt = getPtOccupancies(
        path, pt_interval
    )
    t_av_pt_occ_av = getAverageTimeSeries(av_pt_occ)
    t_av_n_pt = getAverageTimeSeries(n_pt)
    #sigma = np.sqrt(t_av_n_pt / (t_av_n_pt - 1)) * np.sqrt(
    #    av_pt_occ_sq - av_pt_occ ** 2
    #)
    #t_av_pt_occ_sigma = getAverageTimeSeries(sigma)
    return t_av_pt_occ_av#, t_av_pt_occ_sigma

def getDrtTrips(paths):
    path = paths["drt_trips"]
    df = pd.read_csv(path, sep=";").loc[:, ["personId", "waitTime", "travelTime", "travelDistance_m", "direcTravelDistance_m"]]
    df_perperson = df.set_index("personId")
    df_perperson = df.groupby("personId").agg(
        {
            "waitTime": np.sum,
            "travelTime": np.sum,
            "travelDistance_m": np.sum,
        }
    )
    return df, df_perperson

def getDrtDetours(paths):
    path = paths["drt_detours"]
    df = pd.read_csv(path, sep=";").loc[:, ["person", "distanceDetour", "unsharedTime"]]
    # df = df[df < 10]
    return df

def getPersons(paths,periodic=True):
    path = paths["persons"]
    system_size = 20000
    makePeriodic = lambda x: x if x < system_size/2 else system_size - x
    columns = ["first_act_x", "first_act_y", "last_act_x", "last_act_y"]
    df = pd.read_csv(path, sep=";").loc[:,columns]
    diffX = np.abs(df["last_act_x"] - df["first_act_x"])
    if (periodic):
        diffX = diffX.apply(makePeriodic)
    diffY = np.abs(df["last_act_y"] - df["first_act_y"])
    if (periodic):
        diffY = diffY.apply(makePeriodic)
    df["mean_dist"] = np.sqrt(diffX**2 + diffY**2)
    return df
