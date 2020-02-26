import java.util.*;
import java.util.stream.Collectors;

public abstract class EntityMapper {
    private final List<String> stopwords = Arrays.asList("of", "the", "a", "and", "by"); //todo extend stop words


    public HashMap<LeftEntity, List<RightEntity>> matches;

    public EntityMapper() {
        this.matches = new HashMap<>();
    }

    public abstract void createLinkedEntities();

    public List<RightEntity> disambiguateMatches(LeftEntity leftEntity, List<RightEntity> matchingCandidates) {
        return disambiguateMatches(leftEntity, matchingCandidates, false, false);
    }

    public List<RightEntity> disambiguateMatches(LeftEntity leftEntity, List<RightEntity> matchingCandidates, Boolean strictMode, Boolean veryLenientMode) {
        List<RightEntity> disambiguatedMatches = new ArrayList<>();
        for (RightEntity right : matchingCandidates) {
            //TODO what to do if context is empty?
            List<ContextElement> contextLvl1Matches = matchContexts(leftEntity.lvl1context, right.context);
            List<ContextElement> contextLvl2Matches = matchContexts(leftEntity.lvl2context, right.context);
            List<ContextElement> contextLvl3Matches = matchContexts(leftEntity.lvl3context, right.context);
            //if (leftEntity.labels.get(0).getPreprocessed_string().equals("bed")){
            System.out.println("candidate AAT: " + right.identifier);
            System.out.println("lvl1 context subject: " + leftEntity.lvl1context.stream().map(s -> s.contextLabels.get(0).getPreprocessed_string()).collect(Collectors.toList()));
            System.out.println("matches lvl1: " + contextLvl1Matches.stream().map(s -> s.contextLabels.get(0).getPreprocessed_string()).collect(Collectors.toList()));

            System.out.println("lvl2 context subject: " + leftEntity.lvl2context.stream().map(s -> s.contextLabels.get(0).getPreprocessed_string()).collect(Collectors.toList()));
            System.out.println("matches lvl2: " + contextLvl2Matches.stream().map(s -> s.contextLabels.get(0).getPreprocessed_string()).collect(Collectors.toList()));

            System.out.println("lvl3 context subject: " + leftEntity.lvl3context.stream().map(s -> s.contextLabels.get(0).getPreprocessed_string()).collect(Collectors.toList()));
            System.out.println("matches lvl3: " + contextLvl3Matches.stream().map(s -> s.contextLabels.get(0).getPreprocessed_string()).collect(Collectors.toList()));
            // }

            if (!strictMode && right.context.size() == 0) {
                contextLvl1Matches = new ArrayList<>(leftEntity.lvl1context.size());
                contextLvl2Matches = new ArrayList<>(leftEntity.lvl2context.size());
                contextLvl3Matches = new ArrayList<>(leftEntity.lvl3context.size());

            }

            if (entryOverDisambiguationThreshold(leftEntity, contextLvl1Matches.size(), contextLvl2Matches.size(), contextLvl3Matches.size(), strictMode, veryLenientMode)) {
                disambiguatedMatches.add(right);
            }
        }
        return disambiguatedMatches;
    }


    private boolean entryOverDisambiguationThreshold(LeftEntity leftEntity, int numLvl1Matches, int numLvl2Matches, int numLvl3Matches, Boolean strictMode, Boolean veryLenientMode) {
        Double disambiguationScore = calculateDisambiguationMetric(leftEntity, numLvl1Matches, numLvl2Matches, numLvl3Matches, strictMode, veryLenientMode);
        //info change here for threshold
        final Double threshold = 0.2;
        return disambiguationScore >= threshold;
    }


    private Double calculateDisambiguationMetric(LeftEntity leftEntity, int numLvl1Matches, int numLvl2Matches, int numLvl3Matches, Boolean strictMode, Boolean veryLenientMode) {
        int lvl1ContextExists = leftEntity.lvl1context.size() > 0 ? 1 : 0;
        int lvl2ContextExists = leftEntity.lvl2context.size() > 0 ? 1 : 0;
        int lvl3ContextExists = leftEntity.lvl3context.size() > 0 ? 1 : 0;
        //info change here for power
        Double scoreLvl1 = lvl1ContextExists == 1 ? Math.pow(((double) numLvl1Matches / leftEntity.lvl1context.size()), 1.0 / 5) : 0;
        Double scoreLvl2 = lvl2ContextExists == 1 ? Math.pow(((double) numLvl2Matches / leftEntity.lvl2context.size()), 1.0 / 5) : 0;
        Double scoreLvl3 = lvl3ContextExists == 1 ? Math.pow(((double) numLvl3Matches / leftEntity.lvl3context.size()), 1.0 / 5) : 0;

        Double overallScore = (0.9 * scoreLvl1 + 0.8 * scoreLvl2 + 0.7 * scoreLvl3) / (lvl1ContextExists * 0.9 + lvl2ContextExists * 0.8 + lvl3ContextExists * 0.7);
        if (!strictMode && lvl1ContextExists == 0 && lvl2ContextExists == 0 && lvl3ContextExists == 0) {
            overallScore = 1.0;
        }
        if (veryLenientMode && (numLvl1Matches + numLvl2Matches + numLvl3Matches) > 0) {
            overallScore = 1.0;
        }

        //System.out.println(overallScore);
        return overallScore;

    }

    private List<ContextElement> matchContexts(List<ContextElement> leftContext, List<ContextElement> rightContext) {
        return leftContext.stream()
                .filter(l -> {

                    List<String> leftLabels = l.contextLabels
                            .stream()
                            .map(label -> label.getPreprocessed_string())
                            .collect(Collectors.toList());
                    List<String> rightLabels = new ArrayList<>();
                    for (ContextElement rightContextEL : rightContext) {
                        rightLabels.addAll(rightContextEL.contextLabels
                                .stream()
                                .map(r -> r.getPreprocessed_string())
                                .collect(Collectors.toList()));
                    }
                    for (String leftLabel : leftLabels) {
                        // whole label is present
                        if (rightLabels.contains(leftLabel)) return true;
                        List<String> leftLabelWords = new ArrayList<>(Arrays.asList(leftLabel.split(" ")));
                        for (String s : new ArrayList<>(leftLabelWords)) {
                            if (s.contains("-")) {
                                leftLabelWords.addAll(Arrays.asList(s.split("-")));
                            }
                        }
                        for (String rightLabel : rightLabels) {
                            // substring is present -->no, this produces too much noise
                            //if (rightLabel.contains(leftLabel)) return true;
                            //if (leftLabel.contains(rightLabel)) return true;
                            List<String> rightLabelWords = new ArrayList<>(Arrays.asList(rightLabel.split(" ")));
                            // words overlap
                            rightLabelWords.retainAll(leftLabelWords);
                            //todo fuzzy matching?
                            if (rightLabelWords.size() > 0) {
                                for (String s : stopwords) {
                                    rightLabelWords.remove(s);
                                }
                                if (rightLabelWords.size() > 0) {
                                    return true;
                                }
                            }
                        }
                    }
                    return false;
                })
                .collect(Collectors.toList());

    }


    public String getMatchesString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<LeftEntity, List<RightEntity>> match : this.matches.entrySet()) {
            if (match.getValue().size() > 0) {
                stringBuilder.append(match.getKey().labels
                        .stream()
                        .filter(label -> label.language.equals("en"))
                        .map(label -> label.original_string)
                        .collect(Collectors.toList()));
                stringBuilder.append('\t');
                stringBuilder.append(match.getValue()
                        .stream()
                        .map(rightEntity -> new StringBuilder(rightEntity.label).append("(").append(rightEntity.identifier).append(")"))
                        .collect(Collectors.toList()).toString());
            }
            stringBuilder.append('\n');
        }
        return stringBuilder.toString();
    }


    public abstract class LeftEntity {
        String identifier;
        List<Label> labels;
        List<ContextElement> lvl1context;
        List<ContextElement> lvl2context;
        List<ContextElement> lvl3context;
        List<ContextElement> matchedRightEntries;

        public LeftEntity(String identifier) {
            this(identifier, new ArrayList<>());
        }

        public LeftEntity(String identifier, List<Label> labels) {
            this.identifier = identifier;
            this.labels = labels;
            this.lvl1context = new ArrayList<>();
            this.lvl2context = new ArrayList<>();
            this.lvl3context = new ArrayList<>();
            this.matchedRightEntries = new ArrayList<>();
        }


        void setLvl1context(List<Label> labels, String language) {

        }

        void setLvl1context(List<String> labels) {
            this.lvl1context = labels.stream()
                    .map(s -> new ContextElement(Arrays.asList(new Label(s))))
                    .collect(Collectors.toList());
        }

        void setLvl2context(List<String> labels) {
            this.lvl2context = labels.stream()
                    .map(s -> new ContextElement(Arrays.asList(new Label(s))))
                    .collect(Collectors.toList());
        }

        void setLvl3context(List<String> labels) {
            this.lvl3context = labels.stream()
                    .map(s -> new ContextElement(Arrays.asList(new Label(s))))
                    .collect(Collectors.toList());
        }

    }


    public abstract class RightEntity {
        String identifier;
        String label;
        List<ContextElement> context;

        public RightEntity(String identifier, String label) {
            this.identifier = identifier;
            this.label = label;
            this.context = new ArrayList<>();
        }

        public void setContext(List<ContextElement> context) {
            this.context = context;
        }
    }


    class Label {
        private String original_string;
        private String preprocessed_string;
        private String language;

        public Label(String label) {
            this(label, "None");
        }

        public Label(String label, String language) {
            this.original_string = label;
            this.preprocessed_string = label;
            this.language = language;
        }

        public String getOriginal_string() {
            return original_string;
        }

        public String getPreprocessed_string() {
            return preprocessed_string;
        }

        public void setPreprocessed_string(String preprocessed_string) {
            this.preprocessed_string = preprocessed_string;
        }

        public String getLanguage() {
            return language;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Label)) {
                return false;
            }
            Label other = (Label) obj;
            return (other.getOriginal_string().equals(this.original_string) && other.getLanguage().equals(this.language));
        }

        @Override
        public int hashCode() {
            return (this.getOriginal_string() + this.getLanguage()).hashCode();
        }
    }


    class ContextElement {
        List<Label> contextLabels;

        public ContextElement(List<Label> contextLabels) {
            this.contextLabels = contextLabels;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ContextElement)) {
                return false;
            }
            ContextElement other = (ContextElement) obj;
            for (Label contextLabel : this.contextLabels) {
                if (!(other.contextLabels.contains(contextLabel))) {
                    return false;
                }
            }
            return this.contextLabels.size() == other.contextLabels.size();
        }

        @Override
        public int hashCode() {
            return this.contextLabels.hashCode();
        }
    }

}
