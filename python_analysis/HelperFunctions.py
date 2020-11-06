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
