import numpy as np

def timestmp2sec(date):
    secs = [3600, 60, 1]
    return sum([secs[i] * float(el) for i, el in enumerate(date.split(":"))])

def timestmphm2sec(date):
    secs = [3600, 60]
    return sum([secs[i] * float(el) for i, el in enumerate(date.split(":"))])

def sec2timestmp(seconds):
    secs = [3600, 60, 1]
    hours = int(seconds // secs[0])
    seconds = seconds % secs[0]
    mins = int(seconds // secs[1])
    seconds = seconds % secs[1]
    return str(hours) + ":" + str(mins) + ":" + str(int(seconds))

def combineModesSeriesStr(modesSeries):
    raw = "-".join(modesSeries).split("-")
    last = ""
    result = []
    for m in raw:
        if m != last:
            result.append(m)
            last = m
    return "-".join(result)

def getAverageOcc(df, exclude_empty_vehicles=False):
    if exclude_empty_vehicles == True:
        df.drop(columns=["STAY", "0 pax"], inplace=True)
        weights = np.arange(1, len(df.columns) + 1)
    else:
        weights = np.zeros(len(df.columns))
        weights[1:] = np.arange(0, len(df.columns) - 1)

    pass_sum = df.sum(axis=1)
    mean = (df.dot(weights) / pass_sum).mean()
    s = (df.to_numpy() * (weights - mean) ** 2).sum(axis=1) / (pass_sum - 1)
    return mean, np.sqrt(s).mean()
