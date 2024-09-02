import sys
sys.path.append("/home/experiment/")
from config import *
sys.path.append(UNDERSTAND_PYTHON_API_ROOT)
import understand
import pymysql
import os
# from git.repo import Repo
import test2

def relative_var(line, file):
    lexs = file.lexer().lexemes(line, line)
    for lex in lexs:
        if lex.ent() != None:
            if lex.ent().kindname() == "Variable" or lex.ent().kindname() == 'Parameter':
                set_refs = lex.ent().refs('Java Setby')
                if set_refs != []:
                    for ref in set_refs:
                        if ref.line() == line:
                            return line, lex.ent().refs()
    return 0, 0

def back_control_sta(line, control_flow):
    cur_block = 1
    while True:
        startline = control_flow[cur_block].split(',')[1]
        endline = control_flow[cur_block].split(',')[3]
        if startline != '' and endline != '':
            if line < int(startline):
                return None
            if line >= int(startline) and line <= int(endline):
                return None
        # visited[cur_block] = True
        if control_flow[cur_block].split(',')[5] != '':   ## if block has end node
            control_block, endblock = child_dealing(cur_block, line, control_flow)  # When reach first block with branches
            if control_block != 0:
                return control_block
            cur_block = endblock
        else:
            if control_flow[cur_block].split(',')[6] == '':
                return None
            cur_block = int(control_flow[cur_block].split(',')[6])

def child_dealing(cur_control_block, line, control_flow):  ## Dealing with conditional statements
    cur_block = cur_control_block  ## current analysis position
    successor_stack = []    ## In analysis, end loop when successor_stack is null
    end_block = int(control_flow[cur_control_block].split(',')[5])  # end block of these block controled by cur_control_block
    successors = control_flow[cur_block].split(',', 6)[6].split(',')
    successors.append(str(end_block))
    maxs = max(int(num) for num in successors)    ## max value indicate the returned end block number
    while True:
        end_block_chg = 0
        successors = control_flow[cur_block].split(',', 6)[6].split(',')
        for successor in successors:
            if successor == '':
                continue
            if int(successor) < end_block and int(successor) > cur_control_block and int(successor) not in successor_stack:  # if current block is not visited and successors smaller than endblock will be pushed into stack
                successor_stack.append(int(successor))
            if int(successor) > end_block:  ## successor bigger than endblock, change endblock
                end_block = int(successor)
                cur_block = cur_control_block
                maxs = max(maxs, end_block)
                end_block_chg = 1
        if end_block_chg == 1:
            continue
        if successor_stack != []:
            cur_block = successor_stack.pop()
            startline = control_flow[cur_block].split(',')[1]
            endline = control_flow[cur_block].split(',')[3]
            if startline != '' and endline != '':
                if line >= int(startline) and line <= int(endline):
                    return cur_control_block, 0
            if control_flow[cur_block].split(',')[5] != '':  ##if block has end node
                res, block = child_dealing(cur_block, line, control_flow)
                if res != 0:
                    return res, 0
                cur_block = block
        else:
            return 0, maxs

def backward_slicing(st_line, startblock, control_flow, set_lines):
    backward_slicing_res = []
    # param_def = False
    for line in range(int(control_flow[startblock].split(',')[1]), int(control_flow[startblock].split(',')[3]) + 1):
        if line <= st_line and line in set_lines:
            backward_slicing_res.append(line)
            return backward_slicing_res
    block_entry = []  # A path points to target line without Set statements
    block_entry.append(startblock)
    for ite in range(startblock-1, 0, -1):
        successors = control_flow[ite].split(',', 6)[6].split(',')  #get numbers after 6th, which represent successors
        for successor in successors:
            if int(successor) in block_entry: #if successors points to block entries
                no_set_line = 1
                startline = control_flow[ite].split(',')[1]
                endline = control_flow[ite].split(',')[3]
                if startline != '' and endline != '':
                    for line in set_lines:
                        if line >= int(startline) and line <= int(endline): # if block contains set line
                            tmp = line
                            no_set_line = 0
                    if no_set_line == 0:
                        backward_slicing_res.append(tmp)
                if no_set_line == 1:
                    block_entry.append(ite)
    return backward_slicing_res

def forward_slicing(line, control_flow, references, forw_slicing_res, file):
    for curs in range(1, len(control_flow)):  # get start block which contains target line
        startline = control_flow[curs].split(',')[1]
        endline = control_flow[curs].split(',')[3]
        if startline != '' and endline != '':
            if line < int(startline):
                start_block = 1
                break
            if line >= int(startline) and line <= int(endline):
                start_block = curs
                break
    ref_p = 0
    if start_block != 1:
        for ref in references:
            if ref.line() == line and ref.kindname() == 'Set':
                ref_p += 1
                break
            ref_p += 1
    block_path = []
    curr_block = start_block
    stack = []
    ref_p_point = []
    while True:
        block_path.append(curr_block)  # set to visited
        startline = control_flow[curr_block].split(',')[1]  # start and end line of this control block
        endline = control_flow[curr_block].split(',')[3]
        part_end = 0
        if startline != '' and endline != '':
            while references[ref_p].line() < int(startline):  # while ref_p points to statements in previous block
                ref_p += 1
                if ref_p == len(references):
                    break
            if ref_p != len(references):
                while references[ref_p].line() >= int(startline) and references[ref_p].line() <= int(endline):  # block contains lines in ref
                    if references[ref_p].kindname() == 'Set' or references[ref_p].kindname() == 'Define':  # reach Set line, partial end
                        ref_p += 1
                        part_end = 1
                        break
                    if references[ref_p].line() not in forw_slicing_res:
                        forw_slicing_res.append(references[ref_p].line())
                        new_line, new_refs = relative_var(references[ref_p].line(), file)
                        if new_line != 0:
                            forward_slicing(new_line, control_flow, new_refs, forw_slicing_res, file)
                    ref_p += 1
                    if ref_p == len(references):
                        break
            if part_end == 1 or ref_p == len(references):  # if reach partial end
                if stack == []:
                    break
                else:
                    curr_block = stack.pop()
                    ref_p = ref_p_point.pop()
                    continue
        successors = control_flow[curr_block].split(',', 6)[6].split(',')
        if successors != ['']:
            for successor in successors:
                if ":" in successor:
                    continue
                if int(successor) not in block_path:
                    ref_p_point.append(ref_p)
                    stack.append(int(successor))
        if stack == []:
            break
        else:
            curr_block = stack.pop()
            ref_p = ref_p_point.pop()
            continue
    # print('forward slicing result:%s'%(forw_slicing_res))

def new_slicing(file, line, slicing_result,root,api):
    lexs = file.lexer().lexemes(line, line)
    var_list = []
    relate_stat = []
    for lex in lexs:
        if lex.ent() != None:
            if lex.ent().kindname() == 'Variable' or lex.ent().kindname() == 'Parameter':
                var_list.append(lex.ent())
    if var_list != []:
        # print(var_list[0].parent().freetext('CGraph'))
        for var in var_list:
            control_flow = []
            start_block = None
            control_flow.append('')
            for ref in var.refs():   ## find out which method this line belongs to
                if ref.line() == line:
                    break
            freetext = ref.ent().freetext('CGraph').split(';')
            # freetext = var.parent().freetext('CGraph').split(';')
            for block in freetext:
                control_flow.append(block)
            # print('function control flow: %s' % (control_flow))
            if control_flow[2].split(',')[0] == '36':  ## if method only has start and end block
                return
            # if line < int(control_flow[2].split(',')[1]):  ## Some special condition(e.g. lamda method)
            #     return
            for curs in range(1, len(control_flow)):  # get start block which contains target line
                # print(control_flow[curs])
                startline = control_flow[curs].split(',')[1]
                endline = control_flow[curs].split(',')[3]
                if startline != '' and endline != '':
                    if line >= int(startline) and line <= int(endline):
                        start_block = curs
            block = back_control_sta(line, control_flow)  ## control dependecy
            # print('control dependency is:%s' % (block))
            if block != None:
                control_startline = control_flow[block].split(',')[1]
                control_endline = control_flow[block].split(',')[3]
                if control_startline != '' and control_endline != '':
                    for related_line in range(int(control_startline), int(control_endline) + 1):
                        tmp = []
                        if line < related_line:
                            tmp.append(line)
                            tmp.append(related_line)
                        else:
                            tmp.append(related_line)
                            tmp.append(line)
                        tmp.append('control')
                        if tmp not in slicing_result and line != related_line:
                            slicing_result.append(tmp)
                        if related_line not in relate_stat:
                            relate_stat.append(related_line)
            # print('analyse object is:%s'%(var))
            backward_slicing_res = []
            references = var.refs()
            Parameter_defineline = None
            if var.kindname() == 'Parameter':   ## If parameter
                Parameter_defineline = var.ref('Definein').line()
                backward_slicing_res.append(var.ref('Definein').line())
            else:   ## if variable
                set_lines = []
                for ref in references:
                    if ref.kindname() == 'Set':
                        set_lines.append(ref.line())
                if start_block is None:
                    return None
                backward_slicing_res = backward_slicing(line, start_block, control_flow, set_lines)
            #find start blocks for forwarding slicing according to backward slicing result

            for back_line in backward_slicing_res:
                tmp = []
                if line < back_line:
                    tmp.append(line)
                    tmp.append(back_line)
                else:
                    tmp.append(back_line)
                    tmp.append(line)
                tmp.append('data')
                if tmp not in slicing_result and line != back_line:  ## backward slicing ignore
                    relate_stat.append(back_line)
                    if Parameter_defineline == None:
                        slicing_result.append(tmp)
                forw_slicing_res = []
                forward_slicing(back_line, control_flow, references, forw_slicing_res, file)
                # print()

                for related_line in forw_slicing_res:
                    tmp = []
                    if back_line < related_line:
                        tmp.append(back_line)
                        tmp.append(related_line)
                    else:
                        tmp.append(related_line)
                        tmp.append(back_line)
                    tmp.append('data')
                    if tmp not in slicing_result and back_line != related_line:
                        relate_stat.append(related_line)
                        if Parameter_defineline == None:
                            slicing_result.append(tmp)
        return relate_stat
    return None


def target_position(method, params):
    print("Method: %s"%method)
    # file_longname, method_longname, params,
    query_add = 'select add_lines from %s where method_longname="%s" and params="%s" and add_lines!="0";'%(Table, method, params)
    query_del = 'select del_lines from %s where method_longname="%s" and params="%s" and del_lines!="0";'%(Table, method, params)
    query_filecommit = 'select distinct file_longname,commitid from %s where method_longname="%s" and params="%s"'%(Table, method, params)
    mysql = pymysql.connect(host=MYSQL_HOST, user=MYSQL_USER, password=MYSQL_PASS, database=MYSQL_DB,
                            port=MYSQL_PORT, charset='utf8mb4')
    cursor = mysql.cursor()
    cursor.execute(query_add)
    mysql.commit()
    res_add = cursor.fetchall()
    cursor.execute(query_del)
    mysql.commit()
    res_del = cursor.fetchall()
    cursor.execute(query_filecommit)
    mysql.commit()
    res_file = cursor.fetchall()
    cursor.close()
    mysql.close()
    return res_add, res_del, res_file

def get_filenames():
    query = 'select file_longname ,commitid from api_test group by commitid, file_longname'
    mysql = pymysql.connect(host=MYSQL_HOST, user=MYSQL_USER, password=MYSQL_PASS, database=MYSQL_DB,
                            port=MYSQL_PORT, charset='utf8mb4')
    cursor = mysql.cursor()
    cursor.execute(query)
    mysql.commit()
    res = cursor.fetchall()
    cursor.close()
    mysql.close()
    return res

def method_list(item):
    query = 'select distinct method_longname, params from api_test where commitid="%s" and file_longname="%s"'%(item[1], item[0])
    mysql = pymysql.connect(host=MYSQL_HOST, user=MYSQL_USER, password=MYSQL_PASS, database=MYSQL_DB,
                            port=MYSQL_PORT, charset='utf8mb4')
    cursor = mysql.cursor()
    cursor.execute(query)
    mysql.commit()
    res = cursor.fetchall()
    cursor.close()
    mysql.close()
    return res

def get_function_lines(file, method_name, params):
    lexems = file.lexer().lexemes()
    for lex in lexems:
        if lex.ent() != None:
            if ('Method' in lex.ent().kindname() or 'Constructor' in lex.ent().kindname()) and lex.ent().longname() == method_name and lex.ent().parameters() == params:
                define_line = lex.ent().ref('Definein').line()
                # end_line = define_line + lex.ent().metric(['CountLine'])['CountLine'] - 1
                end_line = lex.ent().ref('End').line()
                return define_line, end_line
    return 0, 0

def get_Vsyn(relate_stat_vul, relate_stat_pat, Svul):
    Vsyn = (set(Svul).intersection(set(relate_stat_pat))).union(relate_stat_vul)
    if len(Vsyn) == 0:  ### If set Vsyn is empty
        return None
    return Vsyn

def get_Vsem(Vsyn, slicing_res_vul, slicing_res_pat):
    Vsem = []
    for vul_flow in slicing_res_vul:
        if vul_flow[0] in Vsyn and vul_flow[1] in Vsyn:
            Vsem.append(vul_flow)
    for pat_flow in slicing_res_pat:
        if pat_flow[0] in Vsyn and pat_flow[1] in Vsyn and pat_flow not in Vsem:
            Vsem.append(pat_flow)
    return Vsem

def get_Psyn(relate_stat_pat, Svul):
    Psyn = set(relate_stat_pat).difference(set(Svul))
    if len(Psyn) == 0:  ### If set Psyn is empty
        return None
    return Psyn

def get_Psem(slicing_res_pat, slicing_res_vul, Svul, relate_stat_pat):
    Psem = []
    Tsem = []
    Fsem = []
    for vul_flow in slicing_res_vul:
        if vul_flow[0] in Svul and vul_flow[1] in Svul:
            Fsem.append(vul_flow)
        if vul_flow[0] in relate_stat_pat and vul_flow[1] in relate_stat_pat:
            Tsem.append(vul_flow)
    for pat_flow in slicing_res_pat:
        if pat_flow[0] in Svul and pat_flow[1] in Svul and pat_flow not in Fsem:
            Fsem.append(pat_flow)
        if pat_flow[0] in relate_stat_pat and pat_flow[1] in relate_stat_pat and pat_flow not in Tsem:
            Tsem.append(pat_flow)
    for flow in Tsem:
        if flow not in Fsem:
            Psem.append(flow)
    return Psem


def get_signature_with_known_info(vfile,pfile,method, params, add_line, del_line, udbfile_vul, udbfile_pat):
    relate_stat_vul = []
    relate_stat_pat = []  ## keep statements
    slicing_res_vul = []
    slicing_res_pat = []  ## keep data flow or control flow
    line_sig_vul = {}
    db_vul = understand.open(udbfile_vul)
    file_vul = db_vul.lookup(vfile.split('/')[-1])[0]
    func_startline, func_endline = get_function_lines(file_vul, method, params)  ## tag
    if func_startline != 0 and func_endline != 0:
        line_sig_vul = test2.get_funcsig(func_startline, func_endline, vfile, file_vul)
    if del_line != ():
        for line in del_line[0].split(','):
            if line != '':
                states = new_slicing(file_vul, int(line), slicing_res_vul)
                if states != None:
                    for state in states:
                        if state not in relate_stat_vul:
                            relate_stat_vul.append(state)
        for line in del_line[0].split(','):
            if line != '' and int(line) not in relate_stat_vul:
                relate_stat_vul.append(int(line))
        for index, elem in enumerate(relate_stat_vul):
            if relate_stat_vul[index] in line_sig_vul:
                relate_stat_vul[index] = line_sig_vul[int(relate_stat_vul[index])][0:6]
        i = 0
        while True:
            if type(relate_stat_vul[i]) == int:
                relate_stat_vul.remove(relate_stat_vul[i])
            else:
                i += 1
            if i == len(relate_stat_vul):
                break
        for result in slicing_res_vul:
            result[0] = line_sig_vul[result[0]][0:6]
            result[1] = line_sig_vul[result[1]][0:6]
    if add_line != ():
        db_pat = understand.open(udbfile_pat)
        file_add = db_pat.lookup(pfile.split('/')[-1])[0]
        func_startline, func_endline = get_function_lines(file_add, method, params)
        line_sig_pat = test2.get_funcsig(func_startline, func_endline, pfile, file_add)
        for line in add_line[0].split(','):
            if line != '':
                states = new_slicing(file_add, int(line), slicing_res_pat)
                if states != None:
                    for state in states:
                        if state not in relate_stat_pat:
                            relate_stat_pat.append(state)
        for line in add_line[0].split(','):  ## original lines
            if line != '':
                if int(line) not in relate_stat_pat:
                    relate_stat_pat.append(int(line))
        elems_to_del = []
        for index, elem in enumerate(relate_stat_pat):  # replace number to signature
            if relate_stat_pat[index] in line_sig_pat:
                relate_stat_pat[index] = line_sig_pat[relate_stat_pat[index]][0:6]
            else:
                elems_to_del.append(elem)
        for elem in elems_to_del:
            relate_stat_pat.remove(elem)
        for result in slicing_res_pat:
            result[0] = line_sig_pat[result[0]][0:6]
            result[1] = line_sig_pat[result[1]][0:6]
    Svul = []
    for item in line_sig_vul:
        Svul.append(line_sig_vul[item][0:6])
    if line_sig_vul == {}:
        Vsyn = 0
    else:
        Vsyn = get_Vsyn(relate_stat_vul, relate_stat_pat, Svul)
    Vsem = []
    if Vsyn != None:
        Vsem = get_Vsem(Vsyn, slicing_res_vul, slicing_res_pat)
    if line_sig_vul == {}:
        Psyn = set(relate_stat_pat)
    else:
        Psyn = get_Psyn(relate_stat_pat, Svul)
    Psem = []
    if Psyn != None:
        Psem = get_Psem(slicing_res_pat, slicing_res_vul, Svul, relate_stat_pat)
    return Vsyn, Vsem, Psyn, Psem

def get_feature(udbfile, method, params, vfile, pfile, add_line, del_line,root):
    relate_stat_vul = []
    relate_stat_pat = []  ## keep statements
    slicing_res_vul = []
    slicing_res_pat = []
    ## keep data flow or control flow
    Sdel = set()
    line_sig_vul = {}
    db = understand.open(udbfile)
    if db.lookup(vfile.split('/')[-1]) == [] or db.lookup(pfile.split('/')[-1])== []:
        return None, None, None, None
    file_vul = db.lookup(vfile.split('/')[-1])[0]
    file_add = db.lookup(pfile.split('/')[-1])[0]
    func_startline, func_endline = get_function_lines(file_vul, method, params)  ## tag
    if func_startline != 0 and func_endline != 0:
        line_sig_vul = test2.get_funcsig(func_startline, func_endline, vfile, file_vul)
    if del_line != '0':
        for line in del_line.split(','):
            if line != '':
                states = new_slicing(file_vul, int(line), slicing_res_vul,root,method)
                if states != None:
                    for state in states:
                        if state not in relate_stat_vul:
                            relate_stat_vul.append(state)
        for line in del_line.split(','):
            if line != '':
                if int(line) in line_sig_vul.keys():
                    Sdel.add(line_sig_vul[int(line)][0:6])
            if line != '' and int(line) not in relate_stat_vul:
                relate_stat_vul.append(int(line))
        for index, elem in enumerate(relate_stat_vul):
            if relate_stat_vul[index] in line_sig_vul:
                relate_stat_vul[index] = line_sig_vul[int(relate_stat_vul[index])][0:6]
        i = 0
        while True:
            if type(relate_stat_vul[i]) == int:
                relate_stat_vul.remove(relate_stat_vul[i])
            else:
                i += 1
            if i == len(relate_stat_vul):
                break
        for result in slicing_res_vul:
            result[0] = line_sig_vul[result[0]][0:6]
            result[1] = line_sig_vul[result[1]][0:6]
    if add_line != '0':
        func_startline, func_endline = get_function_lines(file_add, method, params)
        line_sig_pat = test2.get_funcsig(func_startline, func_endline, pfile, file_add)
        for line in add_line.split(','):
            if line != '':
                states = new_slicing(file_add, int(line), slicing_res_pat,root,method)
                if states != None:
                    for state in states:
                        if state not in relate_stat_pat:
                            relate_stat_pat.append(state)
        for line in add_line.split(','):  ## original lines
            if line != '':
                if int(line) not in relate_stat_pat:
                    relate_stat_pat.append(int(line))
        elems_to_del = []
        for index, elem in enumerate(relate_stat_pat):  # replace number with signature
            if relate_stat_pat[index] in line_sig_pat:
                relate_stat_pat[index] = line_sig_pat[relate_stat_pat[index]][0:6]
            else:
                elems_to_del.append(elem)
        for elem in elems_to_del:
            relate_stat_pat.remove(elem)
        for result in slicing_res_pat:
            result[0] = line_sig_pat[result[0]][0:6]
            result[1] = line_sig_pat[result[1]][0:6]
    Svul = []
    for item in line_sig_vul:
        Svul.append(line_sig_vul[item][0:6])
    if line_sig_vul == {}:
        Vsyn = None
    else:
        Vsyn = get_Vsyn(relate_stat_vul, relate_stat_pat, Svul)
    Vsem = []
    if Vsyn != None:
        Vsem = get_Vsem(Vsyn, slicing_res_vul, slicing_res_pat)
    if line_sig_vul == {}:
        Psyn = set(relate_stat_pat)
    else:
        Psyn = get_Psyn(relate_stat_pat, Svul)
    Psem = []
    if Psyn != None:
        Psem = get_Psem(slicing_res_pat, slicing_res_vul, Svul, relate_stat_pat)
    return Vsyn, Vsem, Psyn, Psem, Sdel
