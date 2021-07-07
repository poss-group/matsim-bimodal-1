import numpy as np
import pandas as pd
import os
from HelperFunctions import *
from PtOccupancyFunctions import *

def getBimDirs(directory, ndrt):
    result = []
    if ndrt:
        ndrt = str(ndrt) + "drt"
        bimdir = os.path.join(directory, ndrt)
    else:
        bimdir = directory
    sub_dirs = []
    sdirs = [
        sdir.path
        for sdir in
            os.scandir(bimdir)
        if sdir.is_dir() and "l_" in sdir.name and "error" not in sdir.name
    ]

    for sdir in sorted(sdirs, key=lambda x: int(x.split("/")[-1].split("_")[-1])):
        subresult = {}
        subresult["root"] = sdir
        for root, subdirs, files in os.walk(sdir):
            for file in files:
                if file=="0.trips.csv.gz" and "unimodal" not in root:
                    subresult["trips"] = os.path.join(root, file)
                if file=="0.vehicleDistanceStats_drt.csv" and "unimodal" not in root:
                    subresult["drt_dists"] = os.path.join(root, file)
                if file=="trip_success.csv.gz" and "unimodal" not in root:
                    subresult["trip_success"] = os.path.join(root, file)
                if file=="0.CummulativePtDistance.txt" and "unimodal" not in root:
                    subresult["pt_dist"] = os.path.join(root, file)
                if file=="0.drt_occupancy_time_profiles_drt.txt" and "unimodal" not in root:
                    subresult["drt_occupancy"] = os.path.join(root, file)
                if file=="0.occupancyAnalysis.txt" and "unimodal" not in root:
                    subresult["pt_occupancy"] = os.path.join(root, file)
                if file=="0.drt_trips_drt.csv" and "unimodal" not in root:
                    subresult["drt_trips"] = os.path.join(root, file)
                if file=="0.drt_detours_drt.csv" and "unimodal" not in root:
                    subresult["drt_detours"] = os.path.join(root, file)
                if file=="ph_modestats.txt" and "unimodal" not in root:
                    subresult["ph_modestats"] = os.path.join(root, file)
                if file=="pkm_modestats.txt" and "unimodal" not in root:
                    subresult["pkm_modestats"] = os.path.join(root, file)
                    
        result.append(subresult)
    
    return result

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
        columns=["person","trav_time","wait_time"]
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
    
def getEll(path):
    return path.split("/")[-1].split("_")[-1]

def getModeStats(paths, columns_ph, columns_pkm):
    path_ph = paths["ph_modestats"]
    path_pkm = paths["pkm_modestats"]
    df_ph = pd.read_csv(path_ph, sep='\t').loc[:,columns_ph]
    df_pkm = pd.read_csv(path_pkm, sep='\t').loc[:,columns_pkm]
    return df_ph.to_numpy()[0], df_pkm.to_numpy()[0]

def getDrtVehicleDistances(paths):
    path = paths["drt_dists"]
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

def getDrtOccupandyAndStandingFrac(paths, exclude_empty_vehicles, count_idle_vehicles=False):
    path = paths["drt_occupancy"]
    df = pd.read_csv(path, sep="\t")
    df["time"] = df["time"].apply(timestmphm2sec)
    drt_occ, drt_deviation = getAverageOcc(
        df.drop(columns="time"), exclude_empty_vehicles=exclude_empty_vehicles, count_idle_vehicles=count_idle_vehicles
    )
    drt_standing_frac = getStandingFraction(df.drop(columns="time"))
    return drt_occ, drt_standing_frac

def getPtOccupancy(paths):
    path = paths["pt_occupancy"]
    av_pt_occ, av_pt_occ_sq, n_pt = getPtOccupancies(
        path, 600
    )
    t_av_pt_occ_av = getAverageTimeSeries(av_pt_occ)
    sigma = np.sqrt(n_pt / (n_pt - 1)) * np.sqrt(
        av_pt_occ_sq - av_pt_occ ** 2
    )
    t_av_pt_occ_sigma = getAverageTimeSeries(sigma)
    return t_av_pt_occ_av, t_av_pt_occ_sigma

def getDrtTrips(paths):
    path = paths["drt_trips"]
    df = pd.read_csv(path, sep=";").loc[:, ["personId", "travelDistance_m", "waitTime", "travelTime"]]
    df.set_index("personId", inplace=True)
    df = df.groupby("personId").agg(
        {
            "waitTime": np.sum,
            "travelTime": np.sum,
            "travelDistance_m": np.sum,
        }
    )
    return df

def getDrtDetours(paths):
    path = paths["drt_detours"]
    df = pd.read_csv(path, sep=";").loc[
        :, "distanceDetour"
    ]
    df = df[df < 10]
    return df
