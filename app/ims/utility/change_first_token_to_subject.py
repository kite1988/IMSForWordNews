import os


answers_directory_path = '../resultDir/'
with open(answers_directory_path + 'all.combined.result', 'r') as result_file, open(answers_directory_path + 'all.combined.amended.result', 'w') as amended_file:
    for line in result_file:
        tokens = line.split(' ')

        first_token = tokens[0]
        second_token = tokens[1]
        subject = second_token.split('.')[0]


        amended_file.write(subject + ' ')
        for i in xrange(1, len(tokens)):
            amended_file.write(tokens[i].strip() + ' ')
        amended_file.write('\n')
