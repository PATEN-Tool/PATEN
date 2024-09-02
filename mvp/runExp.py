import os
import pymysql
# from config import *
# import MVP,func_extrac_sig,Target_test
import time
count_tfile = 0

experiment_set = set()
dict_res = {}

def get_vul_apis(snyk_id, commitid, filename, mysql):
    query = 'select distinct method_longname, params from %s where commitid="%s" and filename="%s" and snyk_id="%s"' % (Table, commitid, filename, snyk_id)
    cursor = mysql.cursor()
    cursor.execute(query)
    mysql.commit()
    res = cursor.fetchall()
    cursor.close()
    return res

def get_lines(snyk_id, commitid, filename, method_longname, params, mysql):
    query = 'select add_lines, del_lines from %s where commitid="%s" and filename="%s" and snyk_id="%s" and method_longname="%s" and params="%s"' % (
    Table, commitid, filename, snyk_id, method_longname, params)
    cursor = mysql.cursor()
    cursor.execute(query)
    mysql.commit()
    res = cursor.fetchall()
    cursor.close()
    addline = '0'
    delline = '0'
    for item in res:
        if item[0] != '0':
            addline = item[0]
        if item[1] != '0':
            delline = item[1]
    return addline, delline

def store_res(key,value,res):
    global dict_res
    if res == "not vul" or res == "skip":
        res = "False"
    else:
        res = "True"
    if key not in dict_res.keys():
        dict_res[key] = [value,res]
    else:
        if "uninfluenced_package" in key:
            if res == "False" and dict_res[key][1] == "True":
                dict_res[key][1] = res
        else:                                                   # is influenced_package
            if res == "True" and dict_res[key][1] == "False":
                dict_res[key][1] = res

def output_res():
    global dict_res
    with open("/home/experiment/mvp/mvp_res",'a') as f:
        for key, value in dict_res.items():
            f.write(key+":"+value[0]+":"+value[1]+"\n")

def analyze_res():
    FN = 0
    FP = 0
    TN = 0
    TP = 0
    total_time = 0
    with open("/home/experiment/mvp/mvp_res",'r') as f:
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

def runexp(dir):
    global count_tfile
    apis_info = None
    method = {}
    for root,dirs,files in os.walk(dir):
        for file in files:
            try:
                if "ana.udb" in file:
                    udbfile = root + '/ana.udb'
                    snyk_id = root.split("/")[4]
                    commitid = root.split("/")[6]
                    filename = root.split("/")[7]

                    apis_info = get_vul_apis(snyk_id, commitid, filename, mysql)
                    for api in apis_info:
                        addlines, dellines = get_lines(snyk_id, commitid, filename, api[0], api[1], mysql)
                        Vsyn, Vsem, Psyn, Psem, Sdel = MVP.get_feature(udbfile, api[0], api[1], root + "/vfile.java", root + "/pfile.java", addlines, dellines,root)
                        if Vsyn == None and Vsem == None and Psem == None and Psyn==None:
                            continue
                        method[api[0]+'/'+api[1]] = []
                        method[api[0] + '/' + api[1]].append(Vsyn)   
                        method[api[0] + '/' + api[1]].append(Vsem)
                        method[api[0] + '/' + api[1]].append(Psyn)
                        method[api[0] + '/' + api[1]].append(Psem)
                        method[api[0] + '/' + api[1]].append(Sdel)
                if "tfile.java" in file:
                    if apis_info == None or method == {}:
                        continue
                    print("Files being analyzed:"+root+"/tfile.java")
                    for api in apis_info:
                        target_api = "/".join(root.split("/")[4:])+"/"+api[0].split('.')[-1]
                        if target_api not in experiment_set:
                            continue
                        starttime = time.time()
                        target_flow, target_stm,count_tfile = func_extrac_sig.run_tgtfeature(root+"/tfile.java",api[0], api[1], udbfile,count_tfile)
                        if target_flow == None or target_stm == None:
                            continue
                        if (api[0] + '/' + api[1]) not in method.keys():
                            continue
                        features = method[api[0] + '/' + api[1]]
                        T1, T2, T3, T4, res = Target_test.prove_vul(target_stm, target_flow, features[0], features[1], features[2], features[3],features[4])
                        endtime = time.time()
                        key = root+":"+api[0]
                        value = "T1:"+str(T1)+":T2:"+str(T2)+":T3:"+str(T3)+":T4:"+str(T4)+":time:"+str((endtime-starttime)*1000)
                        store_res(key,value,res)                     
            except Exception as e:
                print(root)
                print(e)
                continue

if __name__ == '__main__':
    mysql = pymysql.connect(host=MYSQL_HOST, user=MYSQL_USER, password=MYSQL_PASS, database=MYSQL_DB,
                            port=MYSQL_PORT, charset='utf8mb4')

    f = open("/home/experiment/experiment_set.txt",'r')
    for i in f.readlines():
        experiment_set.add(i.strip())
    for root,dirs,files in os.walk("/home/experiment/experiment_dataset"):
        if "influenced_package" in root.split("/")[-3]:
            runexp(root)
    mysql.close()
    output_res()

    analyze_res()

