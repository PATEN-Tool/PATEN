import func_extrac_sig
import MVP
import os
from config import *
import pymysql

def prove_vul(target_stm, target_flow, Vsyn, Vsem, Psyn, Psem, Sdel):
    statements = [target_stm[index][0:6] for index in target_stm]
    t1 = 0.8
    t2 = 0.2
    t3 = 0.8
    t4 = 0.2
    if Vsyn != None:  ## If Vsyn is not null
        T1 = len(set(statements).intersection(Vsyn))/len(Vsyn)
        if Vsem != []:
            T3 = len([ele for ele in Vsem if ele in target_flow]) / len(Vsem)
        else:
            T3 = None
    else:
        T1 = None
        T3 = None
    if Psyn != None and len(Psyn) != 0:
        T2 = len(set(statements).intersection(Psyn))/len(Psyn)
        if Psem != []:
            T4 = len([ele for ele in Psem if ele in target_flow])/len(Psem)
        else:
            T4 = None
    else:
        T2 = None
        T4 = None
    if (T1 == None and T3 == None) or (T2 == None and T4 == None):
        return T1, T2, T3, T4, "skip"
    if Sdel <= set(statements):
        if (T1 != None and T1 > t1) or T1 == None:
                if (T2 != None and T2 <= t2) or T2 == None:
                    if (T3 != None and T3 > t3) or T3 == None:
                        if (T4 != None and T4 <= t4) or T4 == None:
                            return T1, T2, T3, T4, "vul"
    return T1, T2, T3, T4, "not vul"
