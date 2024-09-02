import sys
from config import *
import test2
sys.path.append(UNDERSTAND_PYTHON_API_ROOT)
import understand
import os

def relative_var(line, file):  ## transfer variable slicing
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

def forward_slicing(line, control_flow, references, forw_slicing_res, file):
    ref_p = 0
    for ref in references:
        if ref.line() == line and ref.kindname() == 'Set':
            ref_p += 1
            break
        ref_p += 1
    start_block = None
    for curs in range(1, len(control_flow)):  # get start block which contains target line
        startline = control_flow[curs].split(',')[1]
        endline = control_flow[curs].split(',')[3]
        if startline != '' and endline != '':
            if line >= int(startline) and line <= int(endline):
                start_block = curs
                break
    if start_block == None:
        return None
    block_path = []
    curr_block = start_block
    stack = []
    ref_p_point = []
    while True:
        block_path.append(curr_block) # set to visited
        startline = control_flow[curr_block].split(',')[1] #start and end line of this control block
        endline = control_flow[curr_block].split(',')[3]
        part_end = 0
        if startline != '' and endline != '':
            if len(references) == ref_p:
                return None
            while references[ref_p].line() < int(startline): # while ref_p points to statements in previous block
                ref_p += 1
                if ref_p == len(references):
                    break
            if ref_p != len(references):
                while references[ref_p].line() >= int(startline) and references[ref_p].line() <= int(endline): # block contains lines in ref
                    if references[ref_p].kindname() == 'Set': # reach Set line, partial end
                        ref_p += 1
                        part_end = 1
                        break
                    if references[ref_p].line() not in forw_slicing_res and references[ref_p].line() != line:
                        forw_slicing_res.append(references[ref_p].line())
                        new_line, new_refs = relative_var(references[ref_p].line(), file)
                        if new_line != 0:
                            forward_slicing(new_line, control_flow, new_refs, forw_slicing_res, file)
                    ref_p += 1
                    if ref_p == len(references):
                        break
            if part_end == 1 or ref_p == len(references): # if reach partial end
                if stack == []:
                    break
                else:
                    curr_block = stack.pop()
                    ref_p = ref_p_point.pop()
                    continue
        successors = control_flow[curr_block].split(',', 6)[6].split(',')
        if successors != ['']:
            for successor in successors:
                if int(successor) not in block_path:
                    ref_p_point.append(ref_p)
                    stack.append(int(successor))
        if stack == []:
            break
        else:
            curr_block = stack.pop()
            ref_p = ref_p_point.pop()
            continue

def data_flow_dep(func_linebegin, func_lineend, file, control_flow):
    lexs = file.lexer().lexemes(func_linebegin + 1, func_lineend)
    params = []
    vars = []
    sem_sig = []
    for lex in lexs:
        if lex.ent() != None:
            if lex.ent().kindname() == 'Parameter' and lex.ent().name() not in params:  # param data flow
                refs = lex.ent().refs()
                startpoint = None  # Set line
                for ref in refs:
                    if str(ref.kind()) == 'Set':
                        startpoint = ref.line()
                    if str(ref.kind()) == 'Use' and startpoint != None:
                        tmp = []
                        tmp.append(startpoint)
                        tmp.append(ref.line())
                        tmp.append('data')
                        sem_sig.append(tmp)
                params.append(lex.ent().name())
            if lex.ent().kindname() == 'Variable' and lex.ent().name() not in vars:
                refs = lex.ent().refs()
                for ref in refs:
                    if str(ref.kind()) == 'Set' and ref.line() >= func_linebegin and ref.line() <= func_lineend: ### revision for refrence out of method
                        forward_slicing_res = []
                        forward_slicing(ref.line(), control_flow, refs, forward_slicing_res, file)
                        if forward_slicing_res == None:
                            break
                        for res in forward_slicing_res:
                            tmp = []
                            tmp.append(ref.line())
                            tmp.append(res)
                            tmp.append('data')
                            sem_sig.append(tmp)
                vars.append(lex.ent().name())
    return sem_sig

def control_flow_dep(control_flow, sem_sig):
    cur_block = 1
    while True:
        if cur_block == len(control_flow) - 1:
            break
        data = control_flow[cur_block].split(',')
        if data[5] == '':
            cur_block += 1
            continue
        cur_block = node_pair(control_flow, cur_block, sem_sig)
    return sem_sig

def node_pair(control_flow, block, sem_sig):
    end_block = control_flow[block].split(',')[5]
    successor_stack = []
    cur_block = block
    visited_block = []
    successors = control_flow[cur_block].split(',', 6)[6].split(',')
    successors.append(str(end_block))
    end_block = int(max(successors))
    if control_flow[block].split(',')[3] != '':   ###
        control_line = control_flow[block].split(',')[3]
    else:
        control_line = control_flow[int(successors[0])].split(',')[3]  ###### temp dealing for try-catch mode
    while True:
        end_block_chg = 0
        visited_block.append(cur_block)
        successors = control_flow[cur_block].split(',', 6)[6].split(',')
        if successors != ['']:
            for successor in successors:
                if int(successor) < end_block and int(successor) > block:
                    successor_stack.append(successor)
                if int(successor) > end_block:
                    end_block = int(successor)
                    cur_block = block
                    end_block_chg = 1
        if end_block_chg == 1:
            continue
        if successor_stack != []:
            cur_block = int(successor_stack.pop())
            visited_block.append(cur_block)
            cur_block_startline = control_flow[cur_block].split(',')[1]
            cur_block_endline = control_flow[cur_block].split(',')[3]
            if cur_block_startline != '' and cur_block_endline != '':
                for line in range(int(cur_block_startline), int(cur_block_endline) + 1):
                    tmp = []
                    tmp.append(int(control_line))
                    tmp.append(line)
                    tmp.append('control')
                    if tmp not in sem_sig:
                        sem_sig.append(tmp)
            if control_flow[cur_block].split(',')[5] != '':
                cur_block = node_pair(control_flow, cur_block, sem_sig)
                if cur_block > int(end_block):
                    end_block = cur_block
                    cur_block = block
        else:
            break
    return int(end_block)

# def get_func_lineend(control_flow):
#     max_line = 0
#     for block in control_flow:
#         if block == '':
#             continue
#         if block.split(',')[3] == '':
#             continue
#         line = int(block.split(',')[3])
#         max_line = max(line, max_line)
#     return max_line

def und_analyzing(project_dir, method_name, params):
    udbfile = './tmp.udb'
    project_dir_replace_brackets = project_dir.replace(' ', '\ ').replace('(', '\(').replace(')', '\)')   ## replace brackets in filename
    os.system(UNDERSTAND_ROOT + '/und create -languages Java ' + udbfile)
    os.system(UNDERSTAND_ROOT + '/und add ' + project_dir_replace_brackets + ' ' + udbfile)
    os.system(UNDERSTAND_ROOT + '/und analyze ' + udbfile)
    sem_sig, line_sig = run_tgtfeature(project_dir, method_name, params, udbfile)
    os.remove('./tmp.udb')
    return sem_sig, line_sig


def run_tgtfeature(project_dir, method_name, params, udbfile,num):
    db = understand.open(udbfile)
    jarname = project_dir.split("/")[-2]
    tfile = project_dir.split("/")[-1]
    for item in db.lookup(tfile):
        if jarname in item.longname():
            file = item
    lexs = file.lexer().lexemes()
    control_flow = []
    control_flow.append('')
    func_linebegin = None
    func_lineend = None
    for lex in lexs:
        if lex.ent() != None:
            if ('Method' in lex.ent().kindname() or 'Constructor' in lex.ent().kindname()) and lex.ent().longname() == method_name and lex.ent().parameters() == params:
                freetext = lex.ent().freetext('CGraph').split(';')
                for block in freetext:
                    control_flow.append(block)
                func_linebegin = lex.ent().ref('Definein').line()
                # func_lineend = get_func_lineend(control_flow)
                func_lineend = lex.ent().ref('End').line()
                num += 1
                break
    if func_lineend == None:
        return None, None,num
    sem_sig = data_flow_dep(func_linebegin, func_lineend, file, control_flow)
    control_flow_dep(control_flow, sem_sig)
    line_sig = test2.get_funcsig(func_linebegin, func_lineend, project_dir, file)
    to_rm = []
    for tuple in range(len(sem_sig) - 1, -1, -1):
        if sem_sig[tuple][0] in line_sig and sem_sig[tuple][1] in line_sig:
            sem_sig[tuple][0] = line_sig[sem_sig[tuple][0]][0:6]
            sem_sig[tuple][1] = line_sig[sem_sig[tuple][1]][0:6]
        else:
            to_rm.append(tuple)
    for item in to_rm:
        sem_sig.pop(item)
    db.close()
    return sem_sig, line_sig,num
