import matplotlib.pyplot as plt


class Evaluation:



    @staticmethod
    def write_eval_file(filename, data_dict):
        with open(filename, "w") as f:
            for k, v in data_dict.items():
                f.write(k + "\t[")
                for x in v[:-1]:
                    f.write(x + ", ")
                if len(v) > 0:
                    f.write(v[-1] + "]\n")
                else:
                    f.write("]\n")

    @staticmethod
    def fill_matches_dict_from_file(file_path):
        d = {}
        file = open(file_path).read()
        lines = file.split('\n')
        for l in lines:
            if l !='':
                l_list = l.split('\t')
                matches = [x.strip() for x in l_list[1].replace('[', '').replace(']', '').split(',') if x!='']
                d[l_list[0]] = matches
        return d


    @staticmethod
    def print_stats(a, p, r, f):
        print('accuracy %.2f, precision %.2f, recall %.2f, f1 %.2f' % (a, p, r, f))


    @staticmethod
    def get_eval_metrics(p_dict, tr_dict, sum_values, print_values=False):
        TP, FP, FN, TN = Evaluation.evaluate_predictions(p_dict, tr_dict, sum_values, print_values)
        if print_values:
            print("TP %d, FP %d, FN %d, TN %d" % (TP, FP, FN, TN))
        return Evaluation.calculate_accuracy(TP, FP, FN, TN), \
               Evaluation.calculate_precision(TP, FP), \
               Evaluation.calculate_recall(TP, FN), \
               Evaluation.calculate_f1(TP, FP, FN)


    @staticmethod
    def evaluate_predictions(p_dict, tr_dict, sum_values, print_values):
        TP, FP, FN = Evaluation.count_tp_fp_fn(p_dict, tr_dict, print_values)
        TN = sum_values - TP - FP - FN
        return TP, FP, FN, TN


    @staticmethod
    def calculate_precision(TP, FP):
        return TP / (TP + FP) if (TP + FP) > 0 else 0


    @staticmethod
    def calculate_recall(TP, FN):
        return TP / (TP + FN) if (TP + FN) > 0 else 0


    @staticmethod
    def calculate_accuracy(TP, FP, FN, TN):
        return TP / (TP + FP + FN + TN)


    @staticmethod
    def calculate_f1(TP, FP, FN):
        p = Evaluation.calculate_precision(TP, FP)
        r = Evaluation.calculate_recall(TP, FN)
        return 2 * (p * r) / (p + r) if (p + r) > 0 else 0


    @staticmethod
    def count_tp_fp_fn(p_dict, tr_dict, print_values=False):
        TP = 0
        FP = 0
        FN = 0

        for key, value in p_dict.items():
            for v in value:
                if v in tr_dict[key]:
                    TP += 1
                else:
                    FP += 1
                    if print_values:
                        print("FP(" + key + "): predicted: " + str(v) + ' should be: ' + str(tr_dict[key]))

        for key, value in tr_dict.items():
            if key not in p_dict.keys() and tr_dict[key] != []:
                FN += len([x for x in tr_dict[key] if x != ''])
                if print_values:
                    print("FN(" + key + "): predicted: NONE should be: " + str(tr_dict[key]))
            elif key in p_dict.keys():
                for v in value:
                    if v not in p_dict[key] and v != '':
                        FN += 1
                        if print_values:
                            print("FN(" + key + "): predicted: " + str(p_dict[key]) + ' should contain: ' + str(v))

        return TP, FP, FN




if __name__ == '__main__':

    ########## get baseline ##########
    candidate_path = 'candidates.txt'
    candidate_dict = Evaluation.fill_matches_dict_from_file(candidate_path)
    num_candidates = sum([len(v) for v in candidate_dict.values()])
    print("total number of candidates: %d" %num_candidates)


    ########### get evaluation sets ##########
    annotator1_path = 'eval_set_annotator_1.txt'
    annotator2_path = 'eval_set_annotator_2.txt'

    tr_a1_dict = Evaluation.fill_matches_dict_from_file(annotator1_path)
    tr_a2_dict = Evaluation.fill_matches_dict_from_file(annotator2_path)

    print('number of candidates marked as correct by annotator 1: ' + str(sum([len(v) for v in tr_a1_dict.values()])))
    print('number of candidates marked as correct by annotator 2: ' + str(sum([len(v) for v in tr_a2_dict.values()])))

    tr_union_dict = {}
    tr_intersect_dict = {}
    
    for key in candidate_dict.keys():
        tr_intersect_dict[key] = list(set(tr_a1_dict[key]).intersection(set(tr_a2_dict[key])))
        tr_union_dict[key] = list(set(tr_a1_dict[key]).union(set(tr_a2_dict[key])))
    Evaluation.write_eval_file("eval_set_union.txt", tr_union_dict)
    Evaluation.write_eval_file("eval_set_intersection.txt", tr_intersect_dict)


    ########## get aggregated evaluation set ##########
    union_path = "eval_set_union.txt"
    intersection_path = "eval_set_intersection.txt"
    tr_union_dict = Evaluation.fill_matches_dict_from_file(union_path)
    tr_intersect_dict = Evaluation.fill_matches_dict_from_file(intersection_path)

    print("number of links in intersection: %d" %sum([len(v) for v in tr_intersect_dict.values()]) )
    print("number of links in union: %d" %sum([len(v) for v in tr_union_dict.values()]) )


    ########## interrater agreement ##########
    interrateragreement, _, _ , _ = Evaluation.get_eval_metrics(tr_a1_dict, tr_a2_dict, num_candidates)
    print('Interrater-agreement:  %.2f' %interrateragreement)



    ########## evaluation of the influence of the threshold ##########
    union_precisions = []
    intersection_precisions = []
    union_recall = []
    intersection_recall = []
    thresholds = []

    for i in [0, 2, 5] + list(range(10,90,10)):
        print('\n')
        print("######################### THRESHOLD 0.%s  #########################" %str(i).zfill(2))
        if i==20:
            print_values = True
        else:
            print_values = False

        pred_path = 'matches_evaluation_th_%s.txt' %str(i).zfill(2)
        pred_dict = Evaluation.fill_matches_dict_from_file(pred_path)

        print("### predictions on union evaluation set: ")
        a,p,r,f = Evaluation.get_eval_metrics(pred_dict, tr_union_dict, num_candidates, print_values)
        Evaluation.print_stats(a,p,r,f)
        union_precisions.append(p)
        union_recall.append(r)

        print("### candidates on union evaluation set: ")
        a,p,r,f = Evaluation.get_eval_metrics(candidate_dict, tr_union_dict, num_candidates)
        Evaluation.print_stats(a,p,r,f)

        print("### predictions on intersection evaluation set: ")
        a,p,r,f = Evaluation.get_eval_metrics(pred_dict, tr_intersect_dict, num_candidates)
        Evaluation.print_stats(a,p,r,f)
        intersection_precisions.append(p)
        intersection_recall.append(r)

        print("### candidates on intersection evaluation set: ")
        a,p,r,f = Evaluation.get_eval_metrics(candidate_dict, tr_intersect_dict, num_candidates)
        Evaluation.print_stats(a,p,r,f)
        thresholds.append(i)


    ########## plotting influence of threshold ##########
    fig1 = plt.plot(intersection_recall, intersection_precisions, 'bo-')
    plt.xlim([0, 1])
    plt.ylim([0, 1])
    for t, x,y in zip(thresholds, intersection_recall, intersection_precisions,):
        label = t
        plt.annotate(label, (x,y), textcoords="offset points")
    plt.show()

    fig2 = plt.plot(thresholds, intersection_precisions, 'bo-')
    plt.xlim([0, 100])
    plt.ylim([0, 1])
    plt.show()

    fig3 = plt.plot(thresholds, intersection_recall, 'bo-')
    plt.xlim([0, 100])
    plt.ylim([0, 1])
    plt.show()

    ########## threshold is at least one common element ##########
    print("######################### THRESHOLD ONE MATCH  #########################")
    pred_path = 'matches_evaluation_one_match.txt'
    pred_dict = Evaluation.fill_matches_dict_from_file(pred_path)
    a, p, r, f = Evaluation.get_eval_metrics(pred_dict, tr_union_dict, num_candidates)
    Evaluation.print_stats(a, p, r, f)


