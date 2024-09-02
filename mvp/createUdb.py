# from config import *
import json
import os

UNDERSTAND_ROOT = '/home/experiment/scitools/bin/linux64'
UNDERSTAND_PYTHON_API_ROOT = '/home/experiment/scitools/bin/linux64/Python'
import sys
sys.path.append(UNDERSTAND_PYTHON_API_ROOT)
import understand

def createudb():
    for root, dirs, files in os.walk("/home/experiment/experiment_dataset/SNYK-JAVA-COMFASTERXMLJACKSONCORE-32111/uninfluenced_package/6799f8/SubTypeValidator.java"):
        for file in files:
            if "pfile.java" in file:
                udbfile = root + '/ana.udb'
                if  not os.path.exists(udbfile):
                    os.system(UNDERSTAND_ROOT + '/und create -languages Java ' + udbfile)
                    os.system(UNDERSTAND_ROOT + '/und add ' + root + ' ' + udbfile)
                    os.system(UNDERSTAND_ROOT + '/und analyze ' + udbfile)


def removeudb():
    # for root, dirs, files in os.walk("/home/experiment/PATEN_datasetexperiment_dataset/"):
    for root, dirs, files in os.walk("/home/experiment/experiment_dataset/SNYK-JAVA-COMFASTERXMLJACKSONCORE-32111/uninfluenced_package/6799f8/SubTypeValidator.java"):

        for file in files:
            if "pfile.java" in file:
                udbfile = root + '/ana.udb'
                if  os.path.exists(udbfile):
                    os.remove(udbfile)

if __name__ == '__main__':
    # removeudb()
    createudb()
