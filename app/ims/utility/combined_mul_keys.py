import os


answers_directory_path = '../resultDir/'

is_only_modifying_grouped_files = False

filter_phrase = 'amended.result' if is_only_modifying_grouped_files else '.result'

all_files = os.listdir(answers_directory_path)
all_files = sorted(all_files)
amended_files = filter(lambda x: x.endswith(filter_phrase), all_files)

with open(answers_directory_path + 'all.combined.result', 'w') as result_file:
    for filename in amended_files:
        with open(answers_directory_path + filename, 'r') as single_word_result:
            result_file.write(single_word_result.read())
