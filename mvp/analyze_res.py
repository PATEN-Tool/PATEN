def analyze_res():
    FN = 0
    FP = 0
    TN = 0
    TP = 0
    total_time = 0
    with open("/home/experiment/mvp/mvp_res_final",'r') as f:
        for i in f.readlines():
            total_time += float(i.split(":")[-2])
            if "uninfluenced_package" in i:
                if "False" in i:
                    TN += 1
                else:
                    FP += 1
            else:
                if "False" in i:
                    FN += 1
                else:
                    TP += 1
    print("TP: " + str(TP))
    print("FP: " + str(FP))
    print("TN: " + str(TN))
    print("FN: " + str(FN))
    Precision = TP/(TP+FP)
    Recall = TP / (TP + FN)
    print("Precision: "+str(TP/(TP+FP)))
    print("Recall: " + str(TP / (TP + FN)))
    print("Accuracy: " + str((TP+TN )/ (TP+FP+TN+FN)))
    print("F-measure: " + str(2*Precision*Recall/(Precision+Recall)))
    print("avg_time: "+ str(total_time/(TP+TN+FP+FN)))
    print("total: " + str(TN+TP+FN+FP))

analyze_res()