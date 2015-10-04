import os


answers_directory_path = '../EnglishLS.test/'
final_answers = []
with open(answers_directory_path + 'EnglishLS.test.key', 'r') as result_file:
    for line in result_file:
        tokens = line.split(' ')

        first_token = tokens[0]

        subject = first_token.split('.')[0]
        final_answers.append(subject + ' ' + ' '.join(tokens[1:]))


with open(answers_directory_path + 'EnglishLS.test.amended.key', 'w+') as amended_result_file:
    for line in final_answers:


        print line
        amended_result_file.write(line)
    print answers_directory_path + 'EnglishLS.test.amended.key'
print amended_result_file


