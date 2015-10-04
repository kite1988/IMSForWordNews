import os
from IPython import embed

sense_to_group_path = '../answers+misc/tasks/english-group-lex-sample/sense2group'
answer_file_path = '../resultDir/'

if __name__ == "__main__":
    mapping = {}
    with open(sense_to_group_path, 'r') as mapping_file:
        for pair in mapping_file:
            key = pair.split()[0]
            value = pair.split()[1]
            mapping[key] = value

    print mapping

    result_files = os.listdir(answer_file_path)
    for result_file in result_files:
        if result_file.endswith('.amended.result'):
            # no need to amend already amended files
            continue
        amended_file = result_file.replace('.result', '.amended.result')

        with open( answer_file_path + amended_file, 'w') as opened_amended_file, open(answer_file_path + result_file,'r') as opened_result_file:
            for line in opened_result_file:
                for token in line.split(' '):
                    token = token.strip()
                    print '...' + token + '...'
                    if token in mapping:
                        print 'MAPPPP'
                        opened_amended_file.write(mapping[token])
                    else:
                        opened_amended_file.write(token)
                    opened_amended_file.write(' ')
                opened_amended_file.write('\n')








