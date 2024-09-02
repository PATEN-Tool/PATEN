import hashlib
import sys
from config import *
sys.path.append(UNDERSTAND_PYTHON_API_ROOT)
import linecache
import re

def replace_param(matched):
    res = re.sub('\w+', 'PARAM', matched.group(0))
    return res

def replace_var(matched):
    res = re.sub('\w+', 'VARIABLE', matched.group(0))
    return res

def abstr(variables, params, func):
    for param in params:
        pattern = re.compile(r'[^A-Za-z0-9_$]%s[^A-Za-z0-9_$]'%(param))
        func = re.sub(pattern, replace_param, func)
    #     func.replace(' %s '%(param), ' PARAM ')
    for var in variables:
        pattern = re.compile(r'[^A-Za-z0-9_$]%s[^A-Za-z0-9_$]' % (var))
        func = re.sub(pattern, replace_var, func)
    func = re.sub(r'".*"', "String", func) # replace String
    return func

def norm(line):
    line = re.sub(r'//.*', "", line)  # remove comments
    if line == '\n': # if this line is null, delete
        return ''
    pattern = re.compile(r'[\t{} ]')  # remove tab, {} and spaces
    line = re.sub(pattern, "", line)
    return line



def get_funcsig(startline, endline, project_dir, file):
    cur = startline
    func = ''
    linecache.checkcache(project_dir)
    while True:
        if cur == endline:
            func += linecache.getline(project_dir, cur)
            break
        func += linecache.getline(project_dir, cur)
        cur += 1
    lexems = file.lexer().lexemes(startline, endline)
    params = []
    variables = []
    for lex in lexems:
        if lex.ent() != None:
            if lex.ent().kindname() == 'Variable' and lex.ent().name() not in variables:
                variables.append(lex.ent().name())
            if lex.ent().kindname() == 'Parameter' and lex.ent().name() not in params:
                params.append(lex.ent().name())
    func = abstr(variables, params, func)
    lines_sig = {}
    cur = startline
    lines = func.split('\n')
    for line in lines:
        curr_line = norm(line)
        if curr_line != '':
            hs = hashlib.md5()
            hs.update(curr_line.encode())
            lines_sig[cur] = hs.hexdigest()
        cur += 1
    return lines_sig
