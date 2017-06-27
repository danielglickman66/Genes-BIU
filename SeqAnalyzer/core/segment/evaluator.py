





def score(segmented_sentence , gold_sentence):
    predicted_segments = get_segment_indexes(segmented_sentence)
    correct_segments = get_segment_indexes(gold_sentence)
    free = 1

    n_correct = sum([1 for index in predicted_segments if index in correct_segments]) + free

    precision = float(n_correct) / (len(predicted_segments)  + free)
    recall = float(n_correct) / (len(correct_segments) +  free)
    f = 2 * (precision * recall) / (precision + recall)


    return (precision , recall , f)


def get_segment_indexes(sentence):
    indexes = []

    for i in range(len(sentence)):
        if sentence[i] == ' ':
            aligned_segment_index = i - len(indexes)
            indexes.append(aligned_segment_index)

    return set(indexes)